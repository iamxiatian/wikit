package ruc.irm.wikit.espm;

import com.mysql.jdbc.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.exception.WikitException;
import ruc.irm.wikit.espm.graph.CategoryTreeGraph;
import ruc.irm.wikit.espm.graph.CategoryTreeGraphRedisImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

/**
 * Calculate the random walk probability between two ESPM paths
 * User: xiatian
 * Date: 10/7/14
 * Time: 5:23 PM
 */
public class EspmRandomWalk {
    private double alpha = 0.6;
    private Conf conf = null;
    private CategoryTreeGraph categoryTreeCache = null;


    public EspmRandomWalk(Conf conf) throws WikitException {
        this.conf = conf;
        this.categoryTreeCache = new CategoryTreeGraphRedisImpl(conf);
    }

    public double calculateRandomWalkScore(List<Pair<String,Double>> pathListOne, List<Pair<String,Double>> pathListTwo) {
        double sum = 0.0;
        for (int i = 0; i < pathListOne.size(); i++) {
            String path1 = pathListOne.get(i).getLeft();
            double weight1 = pathListOne.get(i).getRight();

            double maxScore = 0;
            double maxWeight2 = 0;
            for (int j = 0; j < pathListTwo.size(); j++) {
                String path2 = pathListTwo.get(j).getLeft();
                double score2 = pathListTwo.get(j).getRight();

                double value = calculateRandomWalkScore(StringUtils.split(path1, "/", true), 0, StringUtils.split(path2, "/", true), 0);
                if (maxScore < value) {
                    maxScore = value;
                    maxWeight2 = score2;
                }
            }

            sum += maxScore * weight1 * maxWeight2;
        }


        return sum/pathListOne.size();
    }

    /*
    public double calculateRandomWalkScore(List<String> pathListOne, List<Double> scoreListOne, List<String> pathListTwo, List<Double> scoreListTwo) {
        double sum = 0.0;

        for (int i = 0; i < pathListOne.size(); i++) {
            String path1 = pathListOne.get(i);
            double weight1 = scoreListOne.get(i);

            double maxScore = 0;
            double maxWeight2 = 0;
            for (int j = 0; j < pathListTwo.size(); j++) {
                String path2 = pathListTwo.get(j);
                double score2 = scoreListTwo.get(j);

                double value = calculateRandomWalkScore(StringUtils.split(path1, "/", true), 0, StringUtils.split(path2, "/", true), 0);
                if (maxScore < value) {
                    maxScore = value;
                    maxWeight2 = score2;
                }
            }

            sum += maxScore * weight1 * maxWeight2;
        }


        return sum/pathListOne.size();
    }
     */

    private double calculateRandomWalkScore(List<String> path1, int position1, List<String> path2, int position2) {
        double result = 0.0;

        if (position1 >= path1.size() || position2 >= path2.size()) {
            return 0;
        }

        String c1 = path1.get(position1);
        String c2 = path2.get(position2);

        double part1 = getTransferProbability(c1, path2, position2);
        double part2 = getTransferProbability(c1, c2) * calculateRandomWalkScore(path1, position1 + 1, path2, position2 + 1);

        return alpha*part1 + (1-alpha)*part2;
    }

    public double getTransferProbability(String fromCategory, List<String> toPath, int toPathStartPosition) {
        double result = 0.0;

        long fromNodeChildCount = categoryTreeCache.getChildCount(fromCategory);

        for (int i = toPathStartPosition; i < toPath.size(); i++) {
            String toCategory = toPath.get(i);
            if (fromCategory.equalsIgnoreCase(toCategory) || categoryTreeCache.isChild(fromCategory, toCategory)) {
                result += 1.0 / (fromNodeChildCount + 1);
            }
        }

        return result;
    }

    /**
     * Get the probability from <code>fromCategory</code> to <code>toCategory</code>
     * @param fromCategory
     * @param toCategory
     * @return
     */
    public double getTransferProbability(String fromCategory, String toCategory) {
        long childCount = categoryTreeCache.getChildCount(fromCategory);
        if (fromCategory.equalsIgnoreCase(toCategory) || categoryTreeCache.isChild(fromCategory, toCategory)) {
            return 1.0 / (childCount + 1);
        } else {
            return 0.0;
        }
    }

}
