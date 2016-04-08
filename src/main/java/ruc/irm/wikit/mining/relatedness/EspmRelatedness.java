package ruc.irm.wikit.mining.relatedness;

import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.exception.WikitException;
import ruc.irm.wikit.espm.SemanticPath;
import ruc.irm.wikit.espm.SemanticPathMining;
import ruc.irm.wikit.espm.impl.SemanticPathMiningWikiImpl;

import java.util.List;

/**
 * @author Tian Xia
 * @date Mar 11, 2016 10:56 PM
 */
public class EspmRelatedness {

    private Conf conf = null;
    private SemanticPathMining espm = null;

    public EspmRelatedness(Conf conf) throws WikitException {
        this.conf = conf;
        this.espm = new SemanticPathMiningWikiImpl(conf);
    }

    public double calculate(String word1, String word2) {
        try {
            List<SemanticPath> path1 = espm.getSemanticPaths(word1, 50, 5);
            List<SemanticPath> path2 = espm.getSemanticPaths(word2, 50, 5);
            System.out.println(path1);
            System.out.println();
            System.out.println(path2);
            return getDependentPossibility(path1.get(0), path2.get(0));
        } catch (WikitException e) {
            e.printStackTrace();
            return 0;
        }
    }



    private double getDependentPossibility(SemanticPath path1, SemanticPath path2) {
        System.out.println("path1:" + path1.toString());
        System.out.println("path2:" + path2.toString());

        int minDepth = path1.length();
        int maxDepth = path2.length();
        if(minDepth>maxDepth) {
            minDepth = maxDepth;
            maxDepth = path1.length();
        }

        if(path1.getLastNode(0)==path2.getLastNode(0)) {
            //@IMPORTANT 到达叶子节点的路径，只保留一条
            return 1.0;
        }
        // sum = (first＋last)×count÷2
        //int totalWeight = (1+minDepth)*minDepth/2;
        double totalWeight = 0;

        double currentWeight = 0;
        for (int i = 1; i < minDepth; i++) {
            double weight =(minDepth-i);
            //double weight = Math.log(minDepth-i + 1)/Math.log(2);
            totalWeight += weight;
            int node1 = path1.id(i);
            int node2 = path2.id(i);

            currentWeight += (minDepth-i)* getSimilarity(node1, node2, i);
            //currentWeight += weight * getSimilarity(node1, node2, i);
        }

        //return 1.0;
        totalWeight += (maxDepth - minDepth);
        //totalWeight += Math.log(maxDepth - minDepth+1)/Math.log(2);

        return currentWeight / totalWeight;
    }


    private double getSimilarity(int c1, int c2, int depth) {
        if (c1 == c2) {
            return 1.0;
        } else {
            return 0;
//            Set<Integer> p1 = tree.getParentIds(c1);
//            Set<Integer> p2 = tree.getParentIds(c2);
//            int differentItems = p1.size() + p2.size();
//            int both = 0;
//            for(int id: p1) {
//                if(p2.contains(p1)) {
//                    both++;
//                    differentItems--;
//                }
//            }
//
//            return (1 - 1.0 / (depth+1)) * both/differentItems;
        }
    }
}
