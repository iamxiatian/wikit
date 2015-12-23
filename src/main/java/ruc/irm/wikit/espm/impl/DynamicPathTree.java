package ruc.irm.wikit.espm.impl;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.exception.WikitException;
import ruc.irm.wikit.espm.SemanticPath;
import ruc.irm.wikit.espm.graph.CategoryTreeGraph;
import ruc.irm.wikit.espm.graph.CategoryTreeGraphRedisImpl;

import java.util.*;

/**
 * A temporary Category Tree to maintain related categories, only appeared
 * categories are kept in memory. Usually, we use <code>addLeafNode
 * (categoryId, weight)</code> to construct this tree.
 *
 * @see{SemanticPathMining#constructCategoryTree}
 *
 * User: xiatian
 * Date: 4/24/14
 * Time: 2:46 AM
 */
public class DynamicPathTree {
    private static final Logger LOG = LoggerFactory.getLogger(DynamicPathTree.class);

    /** maintain the mapping from category id to dynamic node */
    private Map<Integer, DynamicTreeNode> id2NodeIndex = new HashMap<>();

    private Set<Integer> leaveIds = new HashSet<>();

    private CategoryTreeGraph catTreeGraph = null;

    private int rootId = -1;
    private Conf conf = null;


    public DynamicPathTree(Conf conf) throws WikitException {
        this.conf = conf;
        this.catTreeGraph = new CategoryTreeGraphRedisImpl(conf);

        //get root id
        this.rootId = catTreeGraph.getIdByName(conf.getWikiRootCategoryName());
    }

    /**
     * The score that will transit to parents
     */
    private static final class TransitScore {
        int catId;
        double score;

        TransitScore(int catId, double score) {
            this.catId = catId;
            this.score = score;
        }
    }

    public void addLeafNode(int catId, double weight) {
        if (leaveIds.contains(catId)) {
            LOG.error("ERROR: " + catId + " has already exists!");
            return;
        }

        leaveIds.add(catId);

        if (!id2NodeIndex.containsKey(catId)) {
            id2NodeIndex.put(catId, new DynamicTreeNode(catId, 0,
                    catTreeGraph.getRecursiveConceptCount(catId)));
        }

        Queue<TransitScore> queue = new LinkedList<>();
        queue.add(new TransitScore(catId, weight));

        while (!queue.isEmpty()) {
            TransitScore currentTransitScore = queue.poll();

            //update current node's weight
            DynamicTreeNode currentNode = id2NodeIndex.get(currentTransitScore.catId);
            currentNode.weight += currentTransitScore.score;

            Collection<Integer> parents = catTreeGraph.getParentIds(currentTransitScore.catId);
            double parentTransitScoreValue = 0;
            if(parents.size()>0) {
                parentTransitScoreValue = currentTransitScore.score/parents.size();
            }

            for (int pid : parents) {
                if (id2NodeIndex.containsKey(pid)) {
                    DynamicTreeNode parentNode = id2NodeIndex.get(pid);
                    currentNode.addParent(parentNode);
                    parentNode.addChild(currentNode);
                } else {
                    DynamicTreeNode parentNode = new DynamicTreeNode(pid, 0.0, catTreeGraph.getRecursiveConceptCount(pid));
                    id2NodeIndex.put(pid, parentNode);
                    currentNode.addParent(parentNode);
                    parentNode.addChild(currentNode);
                }
                queue.add(new TransitScore(pid, parentTransitScoreValue));
            }
        }
    }

    public void adjustOld(DynamicTreeNode leafNode) {
        Queue<DynamicTreeNode> queue = new LinkedList<>();
        queue.add(leafNode);
        while (!queue.isEmpty()) {
            DynamicTreeNode c = queue.poll();
            for (DynamicTreeNode p : c.parents) {
                p.childCount += 1.0 / c.parents.size();
                p.weight += c.weight / c.parents.size();
                queue.add(p);
            }
        }
    }

    /**
     * 根据父节点的文章数量决定所传递的比率(adjustWithParentDistribution)
     * @param leafNode
     */
    public void adjust(DynamicTreeNode leafNode) {
        Queue<DynamicTreeNode> queue = new LinkedList<>();
        queue.add(leafNode);
        while (!queue.isEmpty()) {
            DynamicTreeNode c = queue.poll();
            int total = getTotalConcepts(c.parents);

            for (DynamicTreeNode p : c.parents) {
                p.childCount += 1.0 / c.parents.size();
                p.weight += c.weight * p.recursiveCptCount / total;
                queue.add(p);
            }
        }
    }

    private int getTotalConcepts(Set<DynamicTreeNode> parents){
        int total = 0;
        for (DynamicTreeNode node : parents) {
            total += node.recursiveCptCount;
        }
        return total;
    }

    public void adjust() {
        System.out.println("adjust...");
        for (int leaf : leaveIds) {
            adjust(id2NodeIndex.get(leaf));
        }
    }



    public  List<SemanticPath> getSemanticPaths() {
        List<SemanticPath> list = new ArrayList<>();
        list.add(new SemanticPath(catTreeGraph, rootId, 1.0));
        List<SemanticPath> paths = drillDownPaths(list);
        Collections.sort(paths, new Comparator<SemanticPath>() {
            @Override
            public int compare(SemanticPath o1, SemanticPath o2) {
                if (o1.getAvgWeight() == o2.getAvgWeight()) {
                    return 0;
                } else if (o1.getAvgWeight() > o2.getAvgWeight()) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });

        return paths;
    }

    /**
     * Get semantic paths, all the paths which are sub-path of other path are
     * removed. For example, if there is a path A-B-C-D, path A-B-C will be
     * removed, because longer path contains more information.
     *
     * @return
     */
    public  List<SemanticPath> getFilteredSemanticPaths() {
        Set<Integer> leafs = Sets.newHashSet(leaveIds);
        for (int id : leafs) {
            if(id2NodeIndex.get(id).childCount>0) {
                System.out.println("remove seed category id: " + id);
                leaveIds.remove(id);
            }
        }

        return getSemanticPaths();
    }

    /**
     * Find the lower paths from given paths
     * @param upperPaths
     * @return
     */
    private List<SemanticPath> drillDownPaths(List<SemanticPath> upperPaths) {
        List<SemanticPath> lowerPaths = new ArrayList<>();
        boolean hasChild = false;
        for (SemanticPath path : upperPaths) {
            int lastCatId = path.getLastNode(-1);
            if (lastCatId < 0) continue;

            DynamicTreeNode node = id2NodeIndex.get(lastCatId);

            if (node.children.size() > 0) {
                for (DynamicTreeNode child : node.children) {
                    SemanticPath p = path.clone();
                    p.addNode(child.id, child.weight);
                    lowerPaths.add(p);
                }
                hasChild = true;
            } else {
                lowerPaths.add(path);
            }
        }

        if (hasChild) {
            return drillDownPaths(lowerPaths);
        } else {
            return lowerPaths;
        }
    }

    public CategoryDistribution getLevelDistribution() {
        CategoryDistribution distribution = new CategoryDistribution();

        double normalizedTotal = 0.0;
        Collection<Integer> levelOneIds = catTreeGraph.getLevelOneCategoryIds();
        for (int id: levelOneIds) {
            DynamicTreeNode node = id2NodeIndex.get(id);

            if (node != null) {
                normalizedTotal += (node.weight / node.children.size());
            }
        }

        for (int id: levelOneIds) {
            DynamicTreeNode node = id2NodeIndex.get(id);
            distribution.names.add(catTreeGraph.getNameById(id, String.valueOf(id)));

            if (node != null) {
                distribution.scores.add(node.weight);
                distribution.nscores.add(node.weight / node.children.size() / normalizedTotal);
            } else {
                distribution.scores.add(0.0);
                distribution.nscores.add(0.0);
            }
        }

        return distribution;
    }

    public static void main(String[] args) {

    }


}
