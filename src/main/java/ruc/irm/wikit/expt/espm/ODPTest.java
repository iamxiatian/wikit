package ruc.irm.wikit.expt.espm;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.common.exception.WikitException;
import ruc.irm.wikit.db.MongoClient;
import ruc.irm.wikit.esa.ESAModel;
import ruc.irm.wikit.esa.ESAModelImpl;
import ruc.irm.wikit.esa.concept.vector.ConceptIterator;
import ruc.irm.wikit.esa.concept.vector.ConceptVector;
import ruc.irm.wikit.esa.concept.vector.TroveConceptVector;
import ruc.irm.wikit.espm.SemanticPath;
import ruc.irm.wikit.espm.graph.CategoryTreeGraph;
import ruc.irm.wikit.espm.graph.CategoryTreeGraphRedisImpl;
import ruc.irm.wikit.espm.impl.SemanticPathMiningWikiImpl;
import ruc.irm.wikit.util.PorterStemmer;
import ruc.irm.wikit.util.ProgressCounter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * var map = function() { emit("path", this.maxScore); }
 * var red = function(k, v) {
 * var i, sum = 0;
 * for (i in v) {
 * sum += v[i];
 * }
 * return sum;
 * }
 * <p>
 * db.expt.result.odp.mapReduce(map, red, {out : {inline: 1}});
 * db.expt.result.odp.mapReduce(map, red, {out : {inline: 1}})["results"][0]["value"];
 * <p>
 *
 * @author Tian Xia <a href="mailto:xiat@ruc.edu.cn">xiat@ruc.edu.cn</a>
 * @date June 9, 2014 11:00 PM
 */
public class ODPTest {
    private static final Logger LOG = LoggerFactory.getLogger(ODPTest.class);

    private CategoryTreeGraph treeCache = null;
    private SemanticPathMiningWikiImpl categoryAnalysis = null;
    private MongoCollection<Document> exptColl = null;
    private MongoCollection<Document> resultColl = null;

    private PorterStemmer stemmer = null;

    private int esaCptLimit = 100;

    private Conf conf;

    public ODPTest(Conf conf) throws WikitException {
        this.conf = conf;
        this.categoryAnalysis = new SemanticPathMiningWikiImpl(conf);
        this.exptColl = MongoClient.getOriginCollection(conf, OdpExptData.COLL_NAME_EXPT);
        this.exptColl.createIndex(new Document("matchedPath", 1));

        this.esaCptLimit = conf.getInt("expt.esa.concept.limit", 100);
        this.resultColl = MongoClient.getOriginCollection(conf, "expt.result.odp.esa" + esaCptLimit);

        this.treeCache = new CategoryTreeGraphRedisImpl(conf);
        this.stemmer = new PorterStemmer();
    }

    private Set<String> getLevelOneWikiCategories() {
        return treeCache.getLevelOneCategoryNames()
                .stream()
                .map(String::toLowerCase)
                .map(stemmer::stem)
                .collect(Collectors.toSet());
    }

    private void cleanOldResults() {
        for (int i = 5; i <= 200; i += 5) {
            MongoCollection<Document> coll = MongoClient.getOriginCollection(conf,
                    "expt.result.odp.esa" + i);
            coll.drop();
            coll = MongoClient.getOriginCollection(conf,
                    "expt.result.odp.esa" + i + ".more");
            coll.drop();
        }
    }

    /**
     * Export odp dataset for final experiment
     */
    private void exportExptOdpData(File f) throws IOException {
        Set<String> levelOneCategories = getLevelOneWikiCategories();

        PrintWriter writer = new PrintWriter(Files.newWriter(f, Charsets.UTF_8));
        MongoCursor<Document> cursor = this.exptColl.find().iterator();
        int skipped = 0;
        while (cursor.hasNext()) {
            Document item = cursor.next();
            String matchedPath = (String) item.get("matchedPath");

            String c1 = StringUtils.split(matchedPath, "/")[0];
            if (levelOneCategories.contains(stemmer.stem(c1.toLowerCase()))) {
                String desc = (String) item.get("desc");
                writer.print("matched:");
                writer.println(matchedPath);
                writer.print("desc:");
                writer.println(desc);
                writer.println();
            } else {
                LOG.debug("skip " + matchedPath);
                skipped++;
            }
        }
        LOG.info("Export experiment data to {}", f.getAbsolutePath());
    }

    /**
     * Save path description's ESA results to MongoDB, therefore, we do not
     * need to re-run ESA algorithm to get description's ESA concepts later.
     * Currently, we preserve 500 concepts to database.
     */
    private void generateESAResults() throws WikitException {
        Set<String> levelOneCategories = getLevelOneWikiCategories();
        ESAModel esaModel = new ESAModelImpl(conf);
        MongoCollection<Document> esaColl = MongoClient.getOriginCollection(conf,"expt.odp.esa");
        esaColl.drop();

        MongoCursor<Document> cursor = this.exptColl.find().iterator();
        int skipped = 0;
        while (cursor.hasNext()) {
            Document item = cursor.next();
            String matchedPath = (String) item.get("matchedPath");

            String c1 = StringUtils.split(matchedPath, "/")[0];
            if (levelOneCategories.contains(stemmer.stem(c1.toLowerCase()))) {
                String desc = (String) item.get("desc");
                ConceptVector cv = esaModel.getCombinedVector(desc, 500);
                Document esaItem = new Document();
                esaItem.append("path", matchedPath);
                List<Integer> ids = new ArrayList<>();
                List<Double> scores = new ArrayList<>();

                if(cv==null || cv.count()==0) {
                    LOG.error("concept vector is null for matchedPath {}",matchedPath);
                    continue;
                }
                ConceptIterator conceptIterator = cv.orderedIterator();
                while (conceptIterator.next()) {
                    ids.add(conceptIterator.getId());
                    scores.add(conceptIterator.getValue());
                }
                esaItem.append("ids", ids).append("scores", scores);

                esaColl.insertOne(esaItem);
            } else {
                LOG.debug("skip " + matchedPath);
                skipped++;
            }
        }
        LOG.info("{} paths are skipped.", skipped);
    }

    private void generateESPMResult() throws WikitException {
        this.resultColl.drop();

        MongoCollection<Document> esaColl = MongoClient.getOriginCollection(conf,
                "expt.odp.esa");
        MongoCursor<Document> esaCursor = esaColl.find().iterator();
        ProgressCounter progress = new ProgressCounter(esaColl.count());

        while (esaCursor.hasNext()) {
            Document item = esaCursor.next();
            String path = (String)item.get("path");
            List<Integer> ids = (List<Integer>)item.get("ids");
            List<Double> esaScores = (List<Double>)item.get("scores");
            ConceptVector cv = new TroveConceptVector(ids.size());
            for (int i = 0; i < ids.size(); i++) {
                cv.add(ids.get(i), esaScores.get(i));
            }

            matchComapre(this.resultColl, path, cv, this.esaCptLimit);

            progress.increment();
        }
        progress.done();
    }


    @Deprecated
    public void runFirstStep() throws WikitException {
        this.resultColl.drop();
        Set<String> levelOneCategories = getLevelOneWikiCategories();

        List<String> paths = new LinkedList<>();
        MongoCursor<Document> cursor = this.exptColl.find().iterator();
        while (cursor.hasNext()) {
            Document item = cursor.next();
            String matchedPath = (String) item.get("matchedPath");

            String c1 = StringUtils.split(matchedPath, "/")[0];
            if (levelOneCategories.contains(stemmer.stem(c1.toLowerCase()))) {
                paths.add(matchedPath);
            }

        }

        int position = 0;
        for (String matchedPath : paths) {
            Document item = this.exptColl.find(new Document("matchedPath",
                    matchedPath)).first();

            String desc = (String) item.get("desc");
            double score = matchComapre(matchedPath, desc);

            System.out.println(position + "\t" + matchedPath + "\t" + score);

            position++;
        }
    }

    private String mergeString(List<String> list, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < list.size(); i++) {
            if (sb.length() > 0) {
                sb.append("/");
            }
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    /**
     * Calculate the semantic path for given Concept Vector, and compared
     * between this calculated paths and provided givenPath
     *
     * @throws WikitException
     */
    private double matchComapre(MongoCollection<Document> storeColl,
                                String givenPath,
                                ConceptVector cv,
                                int conceptLimit)
            throws WikitException {
        List<SemanticPath> wikiPaths = categoryAnalysis.getSemanticPaths(cv, conceptLimit, 20);
        String[] givenPathDetails = StringUtils.split(givenPath, "/");

        double maxMatchedScore = -1;
        SemanticPath maxMatchedPath = null;
        PorterStemmer stemmer = new PorterStemmer();
        int maxMatchedPosition = 0;
        List<String> esaPaths = new ArrayList<>();
        for (int p = 0; p < wikiPaths.size(); p++) {
            SemanticPath wikiPath = wikiPaths.get(p);
            esaPaths.add(mergeString(wikiPath.nameList(), 1));

            Set<String> wikiCategoryBag = new HashSet<>();
            for (int i = 1; i < wikiPath.length(); i++) {
                wikiCategoryBag.add(stemmer.stem(wikiPath.name(i).toLowerCase()));
            }

            Set<String> odpCategoryBag = new HashSet<>();
            for (String category : givenPathDetails) {
                odpCategoryBag.add(stemmer.stem(category.toLowerCase()));
            }

            //check how many odp categories appeared in automatic generated semantic paths
            int count = 0;
            for (String oc : odpCategoryBag) {
                //check whether odp category appeared in wiki categories or not
                for (String wc : wikiCategoryBag) {
                    if (partialMatch(oc, wc)>=0.5) {
                        count++;
                        break;
                    }
                }
            }

            double score = count * 1.0 / givenPathDetails.length;
            if (maxMatchedScore < score) {
                maxMatchedScore = score;
                maxMatchedPath = wikiPath;
                maxMatchedPosition = p;
            }
        }
        Document resultItem = new Document("path", givenPath)
                .append("depth", givenPathDetails.length)
                .append("esaPaths", esaPaths)
                .append("esaPathCount", wikiPaths.size())
                .append("maxPath", mergeString(maxMatchedPath.nameList(), 1))
                .append("maxScore", maxMatchedScore)
                .append("maxRank", maxMatchedPosition);

        storeColl.insertOne(resultItem);
        return maxMatchedScore;
    }

    /**
     * Calculate the semantic path for given odp description, and compared
     * between this calculated path and provided givenPath
     *
     * @param givenPath given path that represented the text
     * @param text
     * @throws IOException
     */
    @Deprecated
    private double matchComapre(String givenPath, String text)
            throws WikitException {
        List<SemanticPath> wikiPaths = categoryAnalysis.getSemanticPaths(text, esaCptLimit, 20);
        String[] givenPathDetails = StringUtils.split(givenPath, "/");

        double maxMatchedScore = -1;
        SemanticPath maxMatchedPath = null;
        PorterStemmer stemmer = new PorterStemmer();
        int maxMatchedPosition = 0;
        List<String> esaPaths = new ArrayList<>();
        for (int p = 0; p < wikiPaths.size(); p++) {
            SemanticPath wikiPath = wikiPaths.get(p);
            esaPaths.add(mergeString(wikiPath.nameList(), 0));

            Set<String> wikiCategoryBag = new HashSet<>();
            for (int i = 1; i < wikiPath.length(); i++) {
                wikiCategoryBag.add(stemmer.stem(wikiPath.name(i).toLowerCase()));
            }

            Set<String> odpCategoryBag = new HashSet<>();
            for (String category : givenPathDetails) {
                odpCategoryBag.add(stemmer.stem(category.toLowerCase()));
            }

            //check how many odp categories appeared in automatic generated semantic paths
            int count = 0;
            for (String oc : odpCategoryBag) {
                //check whether odp category appeared in wiki categories or not
                for (String wc : wikiCategoryBag) {
                    if (partialMatch(oc, wc)>=0.5) {
                        count++;
                        break;
                    }
                }
            }

            double score = count * 1.0 / givenPathDetails.length;
            if (maxMatchedScore < score) {
                maxMatchedScore = score;
                maxMatchedPath = wikiPath;
                maxMatchedPosition = p;
            }
        }
        Document resultItem = new Document("path", givenPath)
                .append("depth", givenPathDetails.length)
                .append("text", text)
                .append("esaPaths", esaPaths)
                .append("esaPathCount", wikiPaths.size())
                .append("maxScore", maxMatchedScore)
                .append("maxRank", maxMatchedPosition);
        this.resultColl.insertOne(resultItem);
        return maxMatchedScore;
    }

    private double partialMatch(String s1, String s2) {
        if (s1.equals(s2)) {
            return 1;
        }

        String[] parts1 = StringUtils.split(s1, " ");
        String[] parts2 = StringUtils.split(s2, " ");
        for (String part1 : parts1) {
            for (String part2 : parts2) {
                if (part1.equals(part2)) {
                    return 0.5;
                }
            }
        }
        return 0;
    }

    private void fix() {
        MongoCursor<Document> cursor = resultColl.find().iterator();
        List<ObjectId> ids = new ArrayList<>();
        while (cursor.hasNext()) {
            Document item = cursor.next();
            ids.add((ObjectId) item.get("_id"));
        }

        int count = 0;
        for (ObjectId id : ids) {
            Document item = resultColl.find(new Document("_id", id)).first();
            List<String> esaPaths = (List<String>)item.get("esaPaths");
            for (int i = 0; i < esaPaths.size(); i++) {
                String path = esaPaths.get(i);
                if(!path.startsWith("[Main")) {
                    System.out.println(id.toString());
                    return;
                }
                path = path.substring(29, path.length() - 3).replaceAll(", ", "/");
                esaPaths.set(i, path);
            }
            resultColl.updateOne(new Document("_id", item.get("_id")), item);
            System.out.print(".");
        }
        System.out.println();
    }

//    private double similarityNDCG(String path, List<String> esaPaths) {
//        String[] comparedCategories = StringUtils.split(path, "/");
//        double maxScore = 0;
//        for (int i = 0; i < esaPaths.size(); i++) {
//            String esaPath = esaPaths.get(i);
//            double score = similarityNDCG(comparedCategories, StringUtils.split(esaPath, "/"));
//            score = score / (i + 1);
//            if (maxScore < score) {
//                maxScore = score;
//            }
//        }
//
//        return maxScore;
//    }

    private double similarityBag(String[] idealPath, String[] esaPath) {
        Set<String> esaBag = new HashSet<>();
        for (int i = 0; i < esaPath.length; i++) {
            esaBag.add(stemmer.stem(esaPath[i].toLowerCase()));
        }

        Set<String> odpBag = new HashSet<>();
        for (String category : idealPath) {
            odpBag.add(stemmer.stem(category.toLowerCase()));
        }

        //check how many odp categories appeared in automatic generated semantic paths
        double count = 0;
        for (String oc : odpBag) {
            //check whether odp category appeared in wiki categories or not
//            if(esaBag.contains(oc)) count++;

            double max = 0;
            for (String wc : esaBag) {
                double v = partialMatch(oc, wc);
                if (v>max){
                    max = v;
                }
            }
            count += max;
        }
        double score = count * 1.0 / odpBag.size();

        return score;
    }

    /**
     * Get nDCG similarity
     * @param idealPath
     * @param esaPath
     * @return
     */
    private double similarityNDCG(String[] idealPath, String[] esaPath) {
        double idealDCG = 0;

        Set<String> idealBag = new HashSet<>();
        int idealPosition = 1;
        for (String c : idealPath) {
            idealBag.add(stemmer.stem(c.toLowerCase()));
            double idealWeight = Math.log(2) / Math.log(idealPosition + 1);
            idealPosition++;
            idealDCG += idealWeight;
        }

        double dcg = 0;
        int dcgPosition = 1;
        for (String c : esaPath) {
            //since the first position is 0, so we change log2(1+k) to log2(2+k)
            //1/(Math.log(2 + k) / Math.log(2)); = Math.log(2)/Math.log(2+k)
            double dcgWeight = Math.log(2) / Math.log(dcgPosition + 1);
            dcgPosition++;

            c = stemmer.stem(c.toLowerCase());
            if (idealBag.contains(c)) {
                dcg += dcgWeight;
            }
        }

        return dcg / idealDCG;
    }

    @Deprecated
    private double similarityOld(String[] comparedPath, String[] esaPath) {
        Set<String> esaBag = new HashSet<>();
        for (String c : esaPath) {
            esaBag.add(stemmer.stem(c.toLowerCase()));
        }

        double idealDCG = 0;
        double dcg = 0;
        for (int k = 0; k < comparedPath.length; k++) {
            //since the first position is 0, so we change log2(1+k) to log2(2+k)
            //1/(Math.log(2 + k) / Math.log(2)); = Math.log(2)/Math.log(2+k)
            double weight = Math.log(2) / Math.log(2 + k);
            idealDCG += weight;
            String c = stemmer.stem(comparedPath[k].toLowerCase());
            if (esaBag.contains(c)) {
                dcg += weight;
            }
        }

        return dcg / idealDCG;
    }

    /**
     * Temp code, just for test
     */
    private void tempTest() {
        String path = "Health/Testing/Genetic";
        String[] categories = StringUtils.split(path, "/");
        String esaPath = "Main topic classifications/Health/Diseases and disorders/Syndromes";
        double score = similarityNDCG(categories, StringUtils.split(esaPath, "/"));
        System.out.println(path);
        System.out.println(esaPath);
        System.out.println("score:" + score);
        System.out.println();

        esaPath = "Health/Diseases and disorders/Syndromes";
        score = similarityNDCG(categories, StringUtils.split(esaPath, "/"));
        System.out.println(path);
        System.out.println(esaPath);
        System.out.println("score2:" + score);
    }

    /**
     * Drill more info on result collection and save to Collection "expt
     * .result.odp.more", so that we can easily analysis
     */
    private void drillMoreInfoOnResult() {
        MongoCollection<Document> moreOdpResultCollection = MongoClient
                .getOriginCollection(conf,
                "expt.result.odp.esa" + esaCptLimit + ".more");
        moreOdpResultCollection.drop();

        //cache all the path into paths
        List<String> paths = new LinkedList<>();
        MongoCursor<Document> cursor = this.resultColl.find().iterator();
        while (cursor.hasNext()) {
            Document item = cursor.next();
            String path = (String) item.get("path");
            paths.add(path);
        }

        Set<String> levelOneCategories = getLevelOneWikiCategories();
        //process each path and append the info
        int count = 0;
        for (String path : paths) {
            Document resultItem = this.resultColl.find(new Document("path",
                    path)).first();
            Document moreItem = new Document();
            moreItem.put("_id", resultItem.get("_id"));
            moreItem.put("path", path);

            String[] categories = StringUtils.split(path, "/");

            //first category
            String c1 = categories[0];

            //does first category exist not
            boolean existedC1 = levelOneCategories.contains(stemmer.stem(c1.toLowerCase()));
            moreItem.put("c1", c1);

            if (!existedC1) continue;

            List<String> fullEsaPaths = (List<String>)resultItem.get("esaPaths");

            int maxNdcgRank = 0;
            double maxNdcgScore = 0;
            List<Double> ndcgScores = new ArrayList<>();

            int maxBagRank = 0;
            double maxBagScore = 0;
            List<Double> bagScores = new ArrayList<>();

            List<String> easPaths = new ArrayList<>();
            String root = conf.getWikiRootCategoryName() + "/";
            int position = 0;
            for (String esaPath : fullEsaPaths) {
                if (esaPath.startsWith(root)) {
                    esaPath = esaPath.substring(root.length());
                }
                easPaths.add(esaPath);

                String[] arrayEsaPath = StringUtils.split(esaPath, "/");
                double score = similarityNDCG(categories, arrayEsaPath);
                ndcgScores.add(score);
                if (maxNdcgScore < score) {
                    maxNdcgScore = score;
                    maxNdcgRank = position;
                }

                score = similarityBag(categories, arrayEsaPath);
                bagScores.add(score);
                if (maxBagScore < score) {
                    maxBagScore = score;
                    maxBagRank = position;
                }

                position++;
            }
            moreItem.put("maxNdcgRank", maxNdcgRank);
            moreItem.put("maxNdcgScore", maxNdcgScore);
            moreItem.put("ndcgScores", ndcgScores);


            moreItem.put("maxBagRank", maxBagRank);
            moreItem.put("maxBagScore", maxBagScore);
            moreItem.put("bagScores", bagScores);

            moreItem.put("esaPaths", easPaths);

            Document exptItem = this.exptColl.find(new Document
                    ("matchedPath", path)).first();
            moreItem.put("odpPaths", exptItem.get("odpPaths"));

            moreOdpResultCollection.insertOne(moreItem);
        }
    }

    private static class BestMatch {
        List<Double> bestScores = new ArrayList<>();

        public void addBestScore(int rank, double score) {
            for (int i = bestScores.size() - 1; i < rank; i++) {
                bestScores.add(0.0);
            }
            bestScores.set(rank, score + bestScores.get(rank));
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Double d : bestScores) {
                sb.append(d).append("\t");
            }
            return sb.toString().trim();
        }
    }

    /**
     * output the statistics NDCG information grouped by main categories. for
     * each category, we calculate its average best match scores from top 1
     * to top N (N is determined by the size of list scores which are stored in
     * MongoDB, collection name is "expt.result.odp.esaXX.more")
     *
     * @param columns: the Latex table columns, i.e., BM@1, BM@2, ...,
     *               BM@columns
     */
    private void ndcgInfo(int columns) {
        MongoCollection<Document> moreOdpResultCollection = MongoClient
                .getOriginCollection(conf,
                "expt.result.odp.esa" + esaCptLimit + ".more");
        //Level 1 category ==> total scores
        Map<String, Double> scoreDistribution = new HashMap<>();

        //Level 1 category ==> item count
        Map<String, Integer> categoryDistribution = new HashMap<>();
        Map<String, BestMatch> bestMatches = new HashMap<>();

        String TOTAL_LABEL = "Avg";
        categoryDistribution.put(TOTAL_LABEL, 0);
        bestMatches.put(TOTAL_LABEL, new BestMatch());

        MongoCursor<Document> cursor = moreOdpResultCollection.find().iterator();
        int totalRecord = 0;
        while (cursor.hasNext()) {
            totalRecord++;
            Document item = cursor.next();
            String path = (String) item.get("path");
            String[] categories = StringUtils.split(path, "/");
            String mainCategory = categories[0];

            double maxScore = (Double) item.get("maxNdcgScore");
            if (categoryDistribution.containsKey(mainCategory)) {
                categoryDistribution.put(mainCategory, categoryDistribution.get(mainCategory) + 1);
            } else {
                categoryDistribution.put(mainCategory, 1);
            }

            if (scoreDistribution.containsKey(mainCategory)) {
                scoreDistribution.put(mainCategory, scoreDistribution.get(mainCategory) + maxScore);
            } else {
                scoreDistribution.put(mainCategory, maxScore);
            }

            categoryDistribution.put(TOTAL_LABEL, categoryDistribution.get(TOTAL_LABEL) + 1);
            BestMatch totalBestMatch = bestMatches.get(TOTAL_LABEL);

            List<Double> scores = (List<Double>) item.get("ndcgScores");
            BestMatch bestMatch = bestMatches.get(mainCategory);
            if (bestMatch == null) {
                bestMatch = new BestMatch();
                bestMatches.put(mainCategory, bestMatch);
            }

            double max = 0;
            for (int i = 0; i < scores.size(); i++) {
                double score = scores.get(i);
                if (score > max) {
                    max = score;
                }
                bestMatch.addBestScore(i, max);
                totalBestMatch.addBestScore(i, max);
            }
        }
//        //print info for R analysis
//        System.out.println("Category distribution");
//        StringBuilder categoriesBuilder = new StringBuilder();
//        categoriesBuilder.append("categories<-c(");
//        StringBuilder labelBuilder = new StringBuilder();
//        labelBuilder.append("lbls<-c(");
//
//
//        for (Map.Entry<String, Integer> entry : categoryDistribution.entrySet()) {
//            if (categoriesBuilder.length() > 14) {
//                categoriesBuilder.append(", ");
//                labelBuilder.append(", ");
//            }
//            categoriesBuilder.append(entry.getValue());
//            labelBuilder.append(entry.getKey());
//            //System.out.println(entry.getKey() + "\t" + entry.getValue());
//        }
//        categoriesBuilder.append(")");
//        labelBuilder.append(")");
//
//        System.out.println(categoriesBuilder.toString());
//        System.out.println(labelBuilder.toString());
//        System.out.println("pie(slices, labels = lbls, main=\"Category Distribution\")");


        //print latex table header
        String[] labels = new String[]{"Arts", "Business",
                            "Health", "Science", "Society", "Sports", "Avg"};
        System.out.printf("Category/#");
        for (int i = 1;
             i <= bestMatches.get("Arts").bestScores.size() && i<= columns;
             i++) {
            System.out.print("\t& BM@" + i);
        }
        System.out.println("\\\\\n\t\\Xhline{0.6pt}");

        for (String label : labels) {
            BestMatch bestMatch = bestMatches.get(label);
            int count = categoryDistribution.get(label);

            System.out.print(label);
            System.out.printf("/%d \t",count);

            for (int j=0; j<columns && j<bestMatch.bestScores.size(); j++) {
                double d = bestMatch.bestScores.get(j);
                System.out.printf(" & %.2f\\%%", d / count * 100);
            }

            System.out.println(" \\\\");
        }
        System.out.println("\\Xhline{0.8pt}");

        //print CSV for plot
        for (String label : labels) {
            BestMatch bestMatch = bestMatches.get(label);
            System.out.print(label);

            for (double d : bestMatch.bestScores) {
                System.out.printf("\t%.2f", d / categoryDistribution.get(label) * 100);
            }
            System.out.println();
        }
    }

    /**
     * output the statistics information
     */
    private Map<String, Double> calculateMRR(double simThreshold) {
        Map<String, Double> result = new HashMap<>();

        MongoCollection<Document> moreOdpResultCollection = MongoClient
                .getOriginCollection(conf,
                "expt.result.odp.esa" + esaCptLimit + ".more");
        //cache all the path into paths
        Map<String, Double> mrrRankDistribution = new HashMap<>();      //Level 1 category ==> total scores

        //maintain category name to appeared count relations
        Map<String, Integer> categoryDistribution = new HashMap<>(); //Level 1 category ==> count

        String TOTAL_LABEL = "Avg";
        categoryDistribution.put(TOTAL_LABEL, 0);
        mrrRankDistribution.put(TOTAL_LABEL, 0.0);

        MongoCursor<Document> cursor = moreOdpResultCollection.find().iterator();
        int totalRecord = 0;
        while (cursor.hasNext()) {
            totalRecord++;
            Document item = cursor.next();
            String path = (String) item.get("path");
            String[] categories = StringUtils.split(path, "/");
            String firstCategory = categories[0];
            List<Double> scores = (List<Double>) item.get("bagScores");

            categoryDistribution.put(TOTAL_LABEL, categoryDistribution.get(TOTAL_LABEL) + 1);
            if (categoryDistribution.containsKey(firstCategory)) {
                categoryDistribution.put(firstCategory, categoryDistribution.get(firstCategory) + 1);
            } else {
                categoryDistribution.put(firstCategory, 1);
            }

            for (int i = 0; i < scores.size(); i++) {
                if (scores.get(i) >= simThreshold) {
                    double value = 1 / (i + 1);
                    if (mrrRankDistribution.containsKey(firstCategory)) {
                        mrrRankDistribution.put(firstCategory, mrrRankDistribution.get(firstCategory) + value);
                    } else {
                        mrrRankDistribution.put(firstCategory, value);
                    }
                    mrrRankDistribution.put(TOTAL_LABEL, mrrRankDistribution.get(TOTAL_LABEL) + value);
                    break;
                }
            }
        }

        for (Map.Entry<String, Double> entry : mrrRankDistribution.entrySet()) {
            result.put(entry.getKey(), entry.getValue() / categoryDistribution.get(entry.getKey()) * 100);
        }

        return result;
    }

    private void testMRR(boolean outputHeader) {
        String[] labels = new String[]{"Arts", "Business", "Health", "Science", "Society", "Sports", "Avg"};
        //labels = new String[]{"Avg"};
        Double[] values = new Double[]{0.2, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5,
                0.55, 0.6, 0.65, 0.7, 0.75, 0.8, 0.85, 0.9, 0.95, 1.0};

        List<Map<String, Double>> allResults = new ArrayList<>();
        for (double d : values) {
            allResults.add(calculateMRR(d));
        }

        if(outputHeader) {
            System.out.print("Concepts");
            for (double d : values) {
                System.out.printf("\t%.2f", d);
            }
            System.out.println();
        }

        System.out.print(esaCptLimit);
        int i = 0;
        for (double d : values) {
            Double mrr = allResults.get(i++).get("Avg");
            if (mrr == null) mrr = 0d;
            System.out.printf("\t %.2f", mrr);
        }
        System.out.println();
    }

    private void testMRRForLatex() {
        String[] labels = new String[]{"Arts", "Business", "Health", "Science", "Society", "Sports", "Avg"};
        //labels = new String[]{"Avg"};
        Double[] values = new Double[]{0.2, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5};

        List<Map<String, Double>> allResults = new ArrayList<>();
        for (double d : values) {
            allResults.add(calculateMRR(d));
        }

        System.out.print("Name");
        for (double d : values) {
            System.out.printf("\t & \t%.2f", d);
        }
        System.out.println("\\\\ \\hline \\hline");

        for (String label : labels) {
            System.out.printf(label + "\t");
            int i = 0;
            for (double d : values) {
                Double mrr = allResults.get(i++).get(label);
                if (mrr == null) mrr = 0d;
                if(mrr<10) {
                    System.out.printf(" &  %.2f\\%%", mrr);
                } else {
                    System.out.printf(" & %.2f\\%%", mrr);
                }
            }
            System.out.println(" \\\\");
        }

    }

    private void testMRR2() {
        String[] labels = new String[]{"Arts", "Business", "Health", "Science", "Society", "Sports", "Avg"};
        List<Double> values = new ArrayList<>();

        List<Map<String, Double>> allResults = new ArrayList<>();
        for (double d = 0.2; d <= 1.0; d = d + 0.05) {
            allResults.add(calculateMRR(d));
            values.add(d);
        }

        System.out.print("Name");
        for (double d : values) {
            System.out.printf("\t%.2f", d);
        }
        System.out.println("");

        for (String label : labels) {
            System.out.printf(label);
            int i = 0;
            for (double d : values) {
                Double mrr = allResults.get(i++).get(label);
                if (mrr == null) mrr = 0d;
                System.out.printf("\t%.2f", mrr);
            }
            System.out.println("");
        }

    }

    private void testSimilarity() {
        double v = similarityNDCG(StringUtils.split("Arts/Acting/Education/Europe", "/"), StringUtils.split("Arts/Aesthetics/Art criticism", "/"));
        System.out.println(v);
    }

    public static void main(String[] args) throws WikitException, ParseException, IOException {
        String helpMsg = "usage: ODPTest -c config.xml";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("c", true, "config file"));
        options.addOption(new Option("prepare", false, "Prepare to generate ESA concepts"));
        options.addOption(new Option("build", false, "build test result"));
        options.addOption(new Option("drill", false, "drill more result(generate .more collection)"));

        options.addOption(new Option("ext", false, "do extended work for " +
                "drill or mrr task"));

        options.addOption(new Option("ndcg", false, "view ndcg info"));
        options.addOption(new Option("mrr", false, "view MRR"));
        options.addOption(new Option("mrr2", false, "detail MRR for specified limit parameter"));

        options.addOption(new Option("from", true, "start concept limit " +
                "for build ESPM(It should be multiple of integer 5) "));
        options.addOption(new Option("limit", true, "top concept limit number"));
        options.addOption(new Option("inc", true, "increment for build," +
                "drill and mrr task"));
        options.addOption(new Option("o", true, "export expt data to file"));

        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("c")) {
            helpFormatter.printHelp(helpMsg, options);
            return;
        }

        Conf conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);
        int limit = NumberUtils.toInt(commandLine.getOptionValue("limit"), 20);
        conf.setInt("expt.esa.concept.limit", limit);
        ODPTest odpTest = new ODPTest(conf);

        System.out.println("Make sure to generateESAResult first.");

        if (commandLine.hasOption("prepare")) {
            odpTest.cleanOldResults();
            odpTest.generateESAResults();
        } else if (commandLine.hasOption("build")) {
            System.out.println("Are you sure to generate ESPM? Y/N");
            Scanner scanner = new Scanner(System.in);
            String line = scanner.nextLine();
            if (line.equals("Y")) {
                //for (; c <= limit; c += 5) {
                Integer[] array = new Integer[]{6,7,8,9,11,12,13,14,16,17,18,
                        19,21,22,23,24,26,27,28,29};
                for(int c: array){
                    conf.setInt("expt.esa.concept.limit", c);
                    ODPTest test = new ODPTest(conf);
                    System.out.println("Generate ESPM with concept limit " + c);
                    test.generateESPMResult();
                    test.drillMoreInfoOnResult();
                }
            }
        } else if (commandLine.hasOption("drill")) {
            int c = Integer.parseInt(commandLine.getOptionValue("from", "5"));
            int inc = Integer.parseInt(commandLine.getOptionValue("inc", "5"));
            System.out.println("Are you sure to DRILL ESPM from " + c +
                    " to " + limit + " with increment " + inc + "? Y/N");
            Scanner scanner = new Scanner(System.in);
            String line = scanner.nextLine();
            if (line.equals("Y")) {
                for (; c <= limit; c += inc) {
                    conf.setInt("expt.esa.concept.limit", c);
                    ODPTest test = new ODPTest(conf);
                    System.out.println("Drill more results with concept limit " + c);
                    test.drillMoreInfoOnResult();
                }
            }
        } else if (commandLine.hasOption("ndcg")) {
            odpTest.ndcgInfo(10);
        } else if (commandLine.hasOption("mrr")) {
            int c = Integer.parseInt(commandLine.getOptionValue("from", "5"));
            int inc = Integer.parseInt(commandLine.getOptionValue("inc", "5"));
            System.out.println("Are you sure to generate MRR from " + c +
                    " to " + limit + " with increment " + inc + "? Y/N");

            Scanner scanner = new Scanner(System.in);
            String line = scanner.nextLine();
            if (line.equals("Y")) {
                for (; c <= limit; c += inc) {
                    conf.setInt("expt.esa.concept.limit", c);
                    ODPTest test = new ODPTest(conf);
                    test.testMRR(c == 2);
                }
            }
        } else if (commandLine.hasOption("mrr2")) {
            ODPTest test = new ODPTest(conf);
            test.testMRRForLatex();
            System.out.println("\n-------------------");
            test.testMRR2();
        } else if (commandLine.hasOption("o")) {
            String outFile = commandLine.getOptionValue("o");
            odpTest.exportExptOdpData(new File(outFile));
        }
        System.out.println("I'm DONE!");
    }
}
