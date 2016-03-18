package ruc.irm.wikit.espm;

import ruc.irm.wikit.espm.graph.CategoryTreeGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A Semantic Path was composed by a sequence of nodes (which was denoted by
 * category id) and their weights.
 */
public class SemanticPath {
    private List<Integer> nodeIds = new ArrayList<>();
    private List<Double> weights = new ArrayList<>();
    private double avgWeight = -1;
    private boolean showDetailOnPath = false;

    private CategoryTreeGraph treeGraph = null;

    public SemanticPath(CategoryTreeGraph treeGraph) {
        this.treeGraph = treeGraph;
    }

    public int length() {
        return nodeIds.size();
    }

    public int id(int i) {
        return nodeIds.get(i);
    }

    public String name(int i) {
        return treeGraph.getNameById(id(i), "Null");
    }

    public SemanticPath(CategoryTreeGraph treeGraph, int categoryId, double weight) {
        this.treeGraph = treeGraph;
        this.nodeIds.add(categoryId);
        this.weights.add(weight);
    }

    public String getPathString() {
        return getPathString('/');
    }

    public String getPathString(char seprator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < nodeIds.size(); i++) {
            if (i > 1) {
                sb.append(seprator);
            }
            int id = nodeIds.get(i);
            String name = treeGraph.getNameById(id, "Null");
            sb.append(name);

            if(showDetailOnPath) {
                int childCount = treeGraph.getChildIds(id).size();
                int conceptCount = treeGraph.getConceptCount(id);
                sb.append("(ch:").append(childCount)
                        .append(", cp:").append(conceptCount)
                        .append(", ").append(String.format("%.3f", weights.get(i)))
                        .append(")");
            }
        }
        return sb.toString();
    }

    public List<Integer> getNodeIds() {
        return this.nodeIds;
    }

    public List<String> nameList() {
        return getNodeIds()
                .stream()
                .map(id -> treeGraph.getNameById(id, "Null"))
                .collect(Collectors.toList());
    }

    public void addNode(int categoryId, double weight) {
        this.nodeIds.add(categoryId);
        this.weights.add(weight);
    }

    public int getLastNode(int notExistedValue) {
        return nodeIds.size() > 0 ? nodeIds.get(nodeIds.size() - 1) : notExistedValue;
    }

    public double getAvgWeight() {
        if (avgWeight == -1) {
            double total = 0;
            for (double w : weights) {
                total += w;
            }
            avgWeight = total / weights.size();
        }

        //均值倾向于short path，而总和倾向于long path，如何均衡？
        return avgWeight;// * Math.log(weights.size())/Math.log(2);
    }

    public SemanticPath clone() {
        SemanticPath path = new SemanticPath(treeGraph);
        path.weights.addAll(this.weights);
        path.nodeIds.addAll(this.nodeIds);
        return path;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(getAvgWeight()).append("\t");
        for (int i = 0; i < nodeIds.size(); i++) {
            if (i > 0) {
                sb.append(" -> ");
            }
            sb.append(treeGraph.getNameById(nodeIds.get(i), "Null"))
                    .append("/")
                    .append(weights.get(i));
        }
        return sb.toString();
    }
}
