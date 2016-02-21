package ruc.irm.wikit.esa;

import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.exception.WikitException;
import ruc.irm.wikit.esa.concept.ConceptCache;
import ruc.irm.wikit.esa.concept.ConceptCacheRedisImpl;
import ruc.irm.wikit.esa.concept.ConceptVectorSimilarity;
import ruc.irm.wikit.esa.concept.vector.ConceptIterator;
import ruc.irm.wikit.esa.concept.vector.ConceptVector;
import ruc.irm.wikit.esa.concept.vector.TroveConceptVector;
import ruc.irm.wikit.util.HeapSort;
import ruc.irm.wikit.util.text.analysis.ESAAnalyzer;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * ESA Model, Performs search on the index located in database.
 *
 * @author Cagatay Calli <ccalli@gmail.com>
 */
public class ESAModelImpl implements ESAModel, Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(ESAModelImpl.class);

    private ConceptCacheRedisImpl conceptCache = null;
    Connection connection;

    PreparedStatement pstmtQuery;
    PreparedStatement pstmtLinks;
    //Statement stmtInlink;

    ESAAnalyzer analyzer;

    int maxConceptId;

    int[] ids;
    double[] values;

    HashMap<String, Integer> freqMap = new HashMap<String, Integer>(30);
    HashMap<String, Double> tfidfMap = new HashMap<String, Double>(30);
    HashMap<String, Float> idfMap = new HashMap<String, Float>(30);

    ArrayList<String> termList = new ArrayList<String>(30);

    TIntIntHashMap inlinkMap;

    static float LINK_ALPHA = 0.5f;

    ConceptVectorSimilarity sim = new ConceptVectorSimilarity();
    private Conf conf = null;

    public void clean() {
        freqMap.clear();
        tfidfMap.clear();
        idfMap.clear();
        termList.clear();
        inlinkMap.clear();

        Arrays.fill(ids, 0);
        Arrays.fill(values, 0);
    }

    public ESAModelImpl(Conf conf) {
        this.conf = conf;

        this.conceptCache = new ConceptCacheRedisImpl(conf);

//        initDB();
        analyzer = new ESAAnalyzer(conf);

        this.maxConceptId = conceptCache.getMaxConceptId() + 1;
        ids = new int[maxConceptId];
        values = new double[maxConceptId];

        inlinkMap = new TIntIntHashMap(300);
    }

    @Override
    protected void finalize() throws Throwable {
        if (connection != null) {
            connection.close();
        }
        super.finalize();
    }

    public ConceptVector getConceptVector(String text) throws WikitException {
        int numTerms = 0;
        ResultSet rs;
        int doc;

        this.clean();

        for (int i = 0; i < ids.length; i++) {
            ids[i] = i;
        }

        try {
            //Get valid terms by Lucene analyzer, and record itd tf, idf
            TokenStream tokenStream = analyzer.tokenStream("contents", new StringReader(text));
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                CharTermAttribute t = tokenStream.getAttribute(CharTermAttribute.class);
                String term = t.toString();
                LOG.debug("extract term:" + term);

                // global IDF of term
                if (!idfMap.containsKey(term)) {
                    float idf = conceptCache.getIdf(term,0);
                    if (idf > 0) {
                        idfMap.put(term, idf);
                    }
                }

                // term counts for TF
                if (freqMap.containsKey(term)) {
                    freqMap.put(term, freqMap.get(term) + 1);
                } else {
                    freqMap.put(term, 1);
                }
                termList.add(term);
                numTerms++;
            }

            tokenStream.end();
            tokenStream.close();

            if (numTerms == 0) {
                return null;
            }

            //calculate TF-IDF vector (normalized)
            double vsum = 0;
            for (String tk : idfMap.keySet()) {
                double tf = 1.0 + Math.log(freqMap.get(tk));
                double v = idfMap.get(tk) * tf;
                tfidfMap.put(tk, v);
                vsum += v * v;
            }
            vsum = Math.sqrt(vsum);

            // comment this out for canceling query normalization
            for (String tk : idfMap.keySet()) {
                double v = tfidfMap.get(tk);
                tfidfMap.put(tk, v / vsum);
            }


            boolean findOne = false;
            for (String tk : termList) {
                ConceptCache.DocScore[] docScores = conceptCache.getTfIdf(tk);
                if (docScores != null) {
                    findOne = true;
                    for (ConceptCache.DocScore docScore : docScores) {
                        Double v = tfidfMap.get(tk);
                        values[docScore.docId] += docScore.score * tfidfMap.get(tk);
                    }
                }
            }

            // no result
            if (!findOne) {
                return null;
            }

            HeapSort.heapSort(values, ids);

            ConceptVector newCV = new TroveConceptVector(ids.length);
            for (int i = ids.length - 1; i >= 0 && values[i] > 0; i--) {
                newCV.set(ids[i], values[i] / numTerms);
            }

            return newCV;
        } catch (IOException e) {
            throw new WikitException(e);
        }
    }


    /**
     * Returns trimmed form of concept vector
     *
     * @param cv
     * @return
     */
    public ConceptVector trimVector(ConceptVector cv, int LIMIT) {
        ConceptVector cv_normal = new TroveConceptVector(LIMIT);
        ConceptIterator it;

        if (cv == null)
            return null;

        it = cv.orderedIterator();

        int count = 0;
        while (it.next()) {
            if (count >= LIMIT) break;
            cv_normal.set(it.getId(), it.getValue());
            count++;
        }

        return cv_normal;
    }

    private TIntIntHashMap setInlinkCounts(Collection<Integer> ids) {
        inlinkMap.clear();

        for (int id : ids) {
            inlinkMap.put(id, conceptCache.getInlinkCount(id));
        }

        return inlinkMap;
    }


    public ConceptVector getLinkVector(ConceptVector cv, int limit)  {
        if (cv == null)
            return null;
        return getLinkVector(cv, LINK_ALPHA, limit);
    }
//
//
//    private Collection<Integer> getInlinks(int id) {
//        return conceptCache.getInlinkIds(id);
//    }
//
//    private int getInlinkCount(int wikiId) {
//        return conceptCache.getConceptInlinkCount(wikiId);
//    }
//
//    private int getInlinkCount(String wikiId) {
//        return conceptCache.getConceptInlinkCount(Integer.parseInt(wikiId));
//    }


    /**
     * Computes secondary interpretation vector of regular features
     *
     * @param cv
     * @param ALPHA
     * @param limit
     * @return
     * @throws SQLException
     */
    public ConceptVector getLinkVector(ConceptVector cv, double ALPHA, int limit) {
        if (cv == null)
            return null;

        ArrayList<Integer> pages = new ArrayList<Integer>();

        //store origin concept id-value map
        TIntFloatHashMap originConceptIdValues = new TIntFloatHashMap(1000);

        //store weighted concept id-value map by inlinks
        TIntFloatHashMap inlinkConceptIdValues = new TIntFloatHashMap();

        //Final normalized inlink weights
        final HashMap<Integer, Float> normalizedInlinkIdValues = new HashMap<Integer, Float>(1000);

        this.clean();

        // collect article objects
        ConceptIterator it = cv.orderedIterator();
        int count = 0;
        while (it.next() && (count++)<limit) {
            pages.add(it.getId());
            originConceptIdValues.put(it.getId(), (float) it.getValue());
        }

        // open inlink counts, assign values for inlinkMap
        setInlinkCounts(pages);


        Collection<Integer> allInlinkPages = new HashSet<>();

        for (int pageId : pages) {
            Collection<Integer> inLinkIds = conceptCache.getInlinkIds(pageId);
            if (inLinkIds.isEmpty()) {
                //System.out.println("no inlinks for " + pid + "\t" + redisData.getConceptTitleById(pid));
                continue;
            }
            ArrayList<Integer> links = new ArrayList<Integer>(inLinkIds.size());

            final double inlink_factor_p = Math.log(conceptCache.getInlinkCount(pageId));

            float originValue = originConceptIdValues.get(pageId);

            //Step 1: get valid inlinks which is more general than pid, store to links
            for (int id : inLinkIds) {
                final double inlink_factor_link = Math.log(conceptCache.getInlinkCount((id)));

                // check concept generality..
                if (inlink_factor_link - inlink_factor_p > 1) {
                    links.add(id);
                    allInlinkPages.add(id);
                }
            }

            //Step 2: update valid inlink's weight
            for (int linkPageId : links) {
//                if (inlinkConceptIdValues.containsKey(linkPageId)) {
//                    inlinkConceptIdValues.put(linkPageId, inlinkConceptIdValues.get(linkPageId) + originValue);
//                } else {
//                    inlinkConceptIdValues.put(linkPageId, originValue);
//                }
                float linkPageValue = 0.0f;
                if (originConceptIdValues.containsKey(linkPageId)) {
                    linkPageValue = originConceptIdValues.get(linkPageId);
                }

                if (inlinkConceptIdValues.containsKey(linkPageId)) {
                    inlinkConceptIdValues.put(pageId, inlinkConceptIdValues.get(linkPageId) + linkPageValue);
                } else {
                    inlinkConceptIdValues.put(pageId, linkPageValue);
                }
            }

        }

        ConceptVector linkConceptVector = new TroveConceptVector(maxConceptId);
        for (int pid : inlinkConceptIdValues.keys()) {
            linkConceptVector.set(pid, inlinkConceptIdValues.get(pid));
        }

        //
//        for (int pid : allInlinkPages) {
//            normalizedInlinkIdValues.put(pid, (float) (ALPHA * inlinkConceptIdValues.get(pid)));
//        }
//
//
//        //System.out.println("read links..");
//
//
//        ArrayList<Integer> keys = new ArrayList(normalizedInlinkIdValues.keySet());
//        Collections.sort(keys, new Comparator<Integer>() {
//            @Override
//            public int compare(Integer left, Integer right) {
//                return normalizedInlinkIdValues.get(right).compareTo(normalizedInlinkIdValues.get(left));
//            }
//        });
//
//        ConceptVector linkConceptVector = new TroveConceptVector(maxConceptId);
//
//        for (int c = 0; c < keys.size() && c < LIMIT; c++) {
//            int p = keys.get(c);
//            linkConceptVector.set(p, normalizedInlinkIdValues.get(p));
//        }

        return linkConceptVector;
    }

    /**
     * get combined vector and limit 50 top concepts.
     * adjust vector by inlinks.
     */
    public ConceptVector getCombinedVector(String query) throws WikitException {
//        ConceptVector cvBase = getConceptVector(query);
//        int limit = 50;
//        //process top limit inlinks
//        if (cvBase == null) {
//            LOG.warn("concept vector is null for query {}", query);
//            return null;
//        }
//
////        ConceptVector cvNormal = trimVector(cvBase, 10);
////        ConceptVector cvLink = getLinkVector(cvNormal, 10);
//
//        ConceptVector cvLink = getLinkVector(cvBase, limit);
//        cvBase.add(cvLink);
//        return cvBase;
        return getCombinedVector(query, true, false, 50);
    }

    @Override
    public ConceptVector getCombinedVector(String text, int limit) throws WikitException {
        //return trimVector(getCombinedVector(text), limit);
        return getCombinedVector(text, true, false, limit);
    }

    /**
     * Calculate semantic relatedness between documents
     *
     * @param doc1
     * @param doc2
     * @return returns relatedness if successful, -1 otherwise
     */
    @Override
    public double getRelatedness(String doc1, String doc2) {
        try {
            ConceptVector c1 = getCombinedVector(doc1);
            ConceptVector c2 = getCombinedVector(doc2);
            // IConceptVector c1 = trimVector(getConceptVector(doc1),10);
            // IConceptVector c2 = trimVector(getConceptVector(doc2),10);

//            ConceptVector c1 = getConceptVector(doc1);
//            ConceptVector c2 = getConceptVector(doc2);

            if (c1 == null || c2 == null) {
                // return 0;
                return -1;    // undefined
            }

            final double rel = sim.calcSimilarity(c1, c2);

            // mark for dealloc
            c1 = null;
            c2 = null;

            return rel;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }

    }

    public ConceptVector getPageViewVector(final ConceptVector cv, int limit) {
        if (cv == null || limit<=0)
            return null;


        final ConceptVector pageViewVector = new TroveConceptVector(limit);
        int count = 0;
        double pvSum = 0;
        ConceptIterator it = cv.orderedIterator();
        while (it.next() && (count++) < limit) {
            int id = it.getId();
            int pv = 1 + conceptCache.getPageViewById(id);
            double value = Math.log(pv)/Math.log(2); //pv;//Math.log(pv);
            pvSum += value;
            pageViewVector.add(id, value);
        }

        //normalize
        it = pageViewVector.iterator();
        while (it.next()) {
            double pv = it.getValue();
            pageViewVector.set(it.getId(), pv / pvSum);
        }

        return pageViewVector;
    }

    public ConceptVector getCombinedVector(String query, boolean considerLinks, boolean considerPageViews, int limit) throws WikitException {
        ConceptVector cvBase = getConceptVector(query);

        LOG.debug("process top " + limit + " inlinks...");
        if (cvBase == null) {
            return null;
        }

        ConceptVector cvNormal = trimVector(cvBase, limit * 2);
        if(considerLinks) {
            ConceptVector cvLink = getLinkVector(cvNormal, limit*2);
            cvNormal.add(cvLink);
        }

        if (considerPageViews) {
            ConceptVector cvPageView = getPageViewVector(cvNormal, limit * 2);

            ConceptIterator it = cvNormal.orderedIterator();
            double total = 0.0;
            while (it.next()) {
                total += it.getValue();
            }

            it = cvNormal.orderedIterator();
            while (it.next()) {
                double value = it.getValue() + cvPageView.get(it.getId()) * total;
                cvNormal.set(it.getId(), value / 2);
            }
//            cvNormal.merge(cvPageView, conf.getDouble("esa.popular.lambda", 0.5));
        }

        return cvNormal;
    }



    public void close() {
        conceptCache.close();
    }

}
