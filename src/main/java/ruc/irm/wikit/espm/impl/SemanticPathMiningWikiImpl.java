package ruc.irm.wikit.espm.impl;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.exception.WikitException;
import ruc.irm.wikit.esa.ESAModelImpl;
import ruc.irm.wikit.esa.concept.ConceptCache;
import ruc.irm.wikit.esa.concept.ConceptCacheRedisImpl;
import ruc.irm.wikit.esa.concept.vector.ConceptIterator;
import ruc.irm.wikit.esa.concept.vector.ConceptVector;
import ruc.irm.wikit.espm.SemanticPath;
import ruc.irm.wikit.espm.SemanticPathMining;
import ruc.irm.wikit.espm.graph.CategoryTreeGraph;
import ruc.irm.wikit.espm.graph.CategoryTreeGraphRedisImpl;
import ruc.irm.wikit.util.ValueComparator;

import java.io.*;
import java.util.*;

/**
 * User: xiatian
 * Date: 4/21/14
 * Time: 2:16 PM
 */
public class SemanticPathMiningWikiImpl implements SemanticPathMining {
    private static Logger LOG = LoggerFactory.getLogger(SemanticPathMiningWikiImpl.class);

    /**
     * An ConceptItem represents a concept that contains concept id, the ESA value(score),
     * and the categories that the concept belongs to.
     * <p>
     * Notes: one concept may have several categories
     */
    private static final class ConceptItem {
        public int id;
        public double value;
        public Set<Integer> catIds;

        public ConceptItem() {

        }

        public ConceptItem(int id, double value) {
            this.id = id;
            this.value = value;
        }
    }

    private static final class Category {
        int id;
        /**
         * ConceptId --> ConceptItem
         */
        Map<Integer, ConceptItem> items = new HashMap<>();

        public Category(int id) {
            this.id = id;
        }

        void addItem(ConceptItem conceptItem) {
            items.put(conceptItem.id, conceptItem);
        }

        Collection<ConceptItem> concepts() {
            return items.values();
        }

        int size() {
            return items.size();
        }

        boolean hasConcept(int conceptId) {
            return items.containsKey(conceptId);
        }

        ConceptItem getConcept(int conceptId) {
            return items.get(conceptId);
        }

    }

    private ConceptCache conceptCache = null;
    private CategoryTreeGraph treeCache = null;
    private Conf conf = null;

    private ESAModelImpl esaModel = null;

    public SemanticPathMiningWikiImpl(Conf conf) throws WikitException {
        this.conf = conf;
        this.conceptCache = new ConceptCacheRedisImpl(conf);
        this.treeCache = new CategoryTreeGraphRedisImpl(conf);

        this.esaModel = new ESAModelImpl(conf);
    }

    /**
     * Given concept vector which represents a text, calculate the category probabilities.
     *
     * @param cv
     * @param conceptLimit only conceptLimit concepts are used to calculate
     *                     the category path distribution
     * @return category id -> scores
     */
    public SortedMap<Integer, Double> getCategoryDistribution(ConceptVector cv,
                                                              int conceptLimit) throws WikitException {
        ConceptIterator conceptIterator = cv.orderedIterator();

        //Category ID --> Category
        Map<Integer, Category> bags = new HashMap<>();
        int count = 0;
        while (conceptIterator.next() && count++ < conceptLimit) {
            int conceptId = conceptIterator.getId();
            ConceptItem conceptItem = new ConceptItem(conceptId, conceptIterator.getValue());
            Set<Integer> catIds = treeCache.getCategoryIdsByConceptId(conceptId);
            conceptItem.catIds = catIds;

            for (int catId : catIds) {
                if (bags.containsKey(catId)) {
                    bags.get(catId).addItem(conceptItem);
                } else {
                    Category category = new Category(catId);
                    category.addItem(conceptItem);
                    bags.put(catId, category);
                }
            }
        }
        double totalScore = 0;
        SortedMap<Integer, Double> sortedScores = null;
        //category id -> score
        Map<Integer, Double> scores = new HashMap<>();

//        LOG.info("Method 1:");
//        for (Map.Entry<Integer, Category> entry : bags.entrySet()) {
//            double categoryScore = 0;
//            Category category = entry.getValue();
//            for (ConceptItem item : category.concepts()) {
//                categoryScore += item.value;
//            }
//            double normalizedScore = categoryScore/entry.getValue().size();
//            scores.put(entry.getKey(), normalizedScore);
//            totalScore += normalizedScore;
//        }
//
//        sortedScores = new TreeMap<Integer,Double>(new ValueComparator(scores));
//        sortedScores.putAll(scores);


        //Method 2, take link into account"
        double LAMBDA = 0.6;
        totalScore = 0;
        scores = new HashMap<>();
        for (Map.Entry<Integer, Category> entry : bags.entrySet()) {
            double categoryScore = 0;
            Category category = entry.getValue();
            for (ConceptItem conceptItem : category.concepts()) {
                double v1 = conceptItem.value;
                double v2 = 0.0;
                Set<Integer> inlinkIds = conceptCache.getInlinkIds(conceptItem.id);
                for (int inlinkId : inlinkIds) {
                    if (category.hasConcept(inlinkId)) {
                        v2 += category.getConcept(inlinkId).value;
                        //System.out.println(inlink + "==>" + item.id + "\t" + item.title);
                    }
                }
                if (inlinkIds.size() > 0) {
                    v2 = v2 / inlinkIds.size(); //normalize
                }

                //if item connected with
                double v = LAMBDA * v1 + (1 - LAMBDA) * v2;
                categoryScore += v;
            }

            double normalizedScore = categoryScore / category.size();
            scores.put(category.id, normalizedScore);
            totalScore += normalizedScore;
        }

        sortedScores = new TreeMap<Integer, Double>(new ValueComparator<>(scores));

        boolean normalize = true;
        if (normalize) {
            //normalized the value
            for (Map.Entry<Integer, Double> entry : scores.entrySet()) {
                sortedScores.put(entry.getKey(), entry.getValue() / totalScore);
            }
        } else {
            sortedScores.putAll(scores);
        }
        return sortedScores;
    }

    public String printCategoryDistribution(SortedMap<Integer, Double> categoryDistribution) {
        StringBuilder scoreValues = new StringBuilder();
        LOG.debug("output category distribution:");
        scoreValues.append("scores<-c(");
        for (Map.Entry<Integer, Double> score : categoryDistribution.entrySet()) {
            String name = treeCache.getNameById(score.getKey(),
                    String.valueOf(score.getKey()));
            LOG.debug(name + "\t\t" + score.getValue() + "\t" + treeCache.getDepth(score.getKey()));
            if (scoreValues.length() > 10) {
                scoreValues.append(", ");
            }
            scoreValues.append(score.getValue());
        }
        scoreValues.append(")");
        return scoreValues.toString();
    }

    public List<SemanticPath> getSemanticPaths(String text,
                                               int conceptLimit,
                                               int topPaths)
            throws WikitException {
        try {
            ConceptVector cv = esaModel.getCombinedVector(text, conceptLimit);

            return getSemanticPaths(cv, conceptLimit, topPaths);
        } catch (NullPointerException e) {
            throw new NullPointerException("null exception, text:" + text);
        }
    }

    public List<SemanticPath> getSemanticPaths(ConceptVector cv,
                                               int conceptLimit,
                                               int topPaths)
            throws WikitException {
        if (cv == null || cv.size()==0) {
            return new ArrayList<>();
        }
        SortedMap<Integer, Double> categoryDistribution = getCategoryDistribution(cv, conceptLimit);
        return getSemanticPaths(constructCategoryTree(categoryDistribution), topPaths);
    }

    public CategoryDistribution getLevelOneDistribution(DynamicPathTree tree) throws WikitException {
        return tree.getLevelDistribution();
    }

    public DynamicPathTree constructCategoryTree(SortedMap<Integer, Double> categoryDistribution)
            throws WikitException {
        DynamicPathTree tree = new DynamicPathTree(conf);
        for (Map.Entry<Integer, Double> score : categoryDistribution.entrySet()) {
            tree.addLeafNode(score.getKey(), score.getValue());
        }

        return tree;
    }

    /**
     * GML (Graph Modeling Language) is a text file format supporting network
     * data with a very easy syntax. It is used by Graphlet, Pajek, yEd,
     * LEDA and NetworkX.
     */
    public void exportAsGML(File gmlFile, List<SemanticPath> paths) {
        gmlFile.getParentFile().mkdirs();

        try (PrintWriter writer =
                     new PrintWriter(Files.newWriter(gmlFile, Charsets.UTF_8));
        ) {

            writer.println("graph\n[");
            //write vertex
            for (SemanticPath path : paths) {
                for (int i = 0; i < path.length(); i++) {
                    writer.println("\tnode\n\t[");
                    writer.println("\t\tid " + path.id(i));
                    writer.println("\t\tlabel \"" + path.name(i) + "\"");
                    writer.println("\t]");
                }
            }

            //write edge
            for (SemanticPath path : paths) {
                for (int i = 0; i < path.length() - 1; i++) {
                    writer.println("\tedge\n\t[");
                    writer.println("\t\tsource " + path.id(i + 1));
                    writer.println("\t\ttarget " + path.id(i));
                    writer.println("\t]");
                }
            }
            writer.println(']');
            writer.flush();
        } catch (IOException e) {
            LOG.error("Export to GML file error!", e);
        }
    }


    public List<SemanticPath> getSemanticPaths(DynamicPathTree categoryTree, int topN)
            throws WikitException {
        List<SemanticPath> paths = null;
//        paths = categoryTree.getSemanticPaths();
        paths = categoryTree.getFilteredSemanticPaths();
        List<SemanticPath> results = new ArrayList<>();

//        int count = 0;
//        for (int i = paths.size() - 1; i >= 0; i--) {
//            results.add(paths.get(i));
//            if (++count >= 200) break;
//        }

        GreedyMWIS greedyMWIS = new GreedyMWIS(treeCache);
        greedyMWIS.buildMWISGraph(paths);
        for (int i = 0; i < topN; i++) {
            SemanticPath topPath = greedyMWIS.findTopNode();
            if (topPath != null) {
                results.add(topPath);
            } else {
                break;
            }
        }
        return results;
    }

}
