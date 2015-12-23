package ruc.irm.wikit.espm.impl;

import java.util.HashSet;
import java.util.Set;

/**
 * User: xiatian
 * Date: 4/24/14
 * Time: 2:46 AM
 */
public class DynamicTreeNode {
    public int id;
    public double weight;

    /** because a child may belong to many parents,
     * so, only the proportion count is calculated.
     */
    public double childCount = 0.0;

    /**
     * 该类别所拥有的概念数量，包括所有子类别的概念数量
     */
    public int recursiveCptCount = 0;

    public Set<DynamicTreeNode> parents = new HashSet<>();

    public Set<DynamicTreeNode> children = new HashSet<>();

    public DynamicTreeNode(int id, double weight, int rc) {
        this.id = id;
        this.weight = weight;
        this.recursiveCptCount = rc;
        this.childCount = 0.0;
    }

    public void addParent(DynamicTreeNode p) {
        this.parents.add(p);
    }

    public void addChild(DynamicTreeNode c) {
        this.children.add(c);
    }

    private double getNormalizedWeight() {
        if (childCount == 0) {
            return weight;
        } else {
            return  weight/childCount;
        }
    }

    @Override
    public String toString() {
        return "CategoryNode{" +
                "id='" + id + '\'' +
                ", weight=" + weight +
                ", childCount=" + childCount +
                '}';
    }
}
