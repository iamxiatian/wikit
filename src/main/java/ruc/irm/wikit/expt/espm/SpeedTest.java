package ruc.irm.wikit.expt.espm;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.common.exception.WikitException;
import ruc.irm.wikit.esa.ESAModel;
import ruc.irm.wikit.esa.ESAModelImpl;
import ruc.irm.wikit.esa.concept.ConceptCache;
import ruc.irm.wikit.esa.concept.ConceptCacheRedisImpl;
import ruc.irm.wikit.esa.concept.vector.ConceptVector;
import ruc.irm.wikit.espm.SemanticPath;
import ruc.irm.wikit.espm.SemanticPathMining;
import ruc.irm.wikit.espm.graph.CategoryTreeGraph;
import ruc.irm.wikit.espm.graph.CategoryTreeGraphRedisImpl;
import ruc.irm.wikit.espm.impl.DynamicPathTree;
import ruc.irm.wikit.espm.impl.GreedyMWIS;
import ruc.irm.wikit.espm.impl.SemanticPathMiningWikiImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

/**
 * @author Tian Xia
 * @date Aug 05, 2016 10:52
 */
public class SpeedTest {
    private ESAModel esaModel = null;
    private SemanticPathMining espmModel = null;
    private ConceptCache conceptCache = null;
    private CategoryTreeGraph treeCache = null;

    private int count = 0;
    private long esaNanos = 0;
    private long pathMiningNanos = 0;
    private long pathPickingNanos = 0;

    private int conceptLimit = 100;
    private int topPaths = 20;

    public SpeedTest(Conf conf) throws WikitException {
        this.esaModel = new ESAModelImpl(conf);
        this.espmModel = new SemanticPathMiningWikiImpl(conf);
        this.treeCache = new CategoryTreeGraphRedisImpl(conf);
        this.conceptCache = new ConceptCacheRedisImpl(conf);
    }


    private void run(String text) throws WikitException {
        count++;

        long time = System.nanoTime();
        ConceptVector cv = esaModel.getCombinedVector(text, conceptLimit);
        esaNanos += (System.nanoTime() - time);

        time = System.nanoTime();
        SortedMap<Integer, Double> categoryDistribution = espmModel.getCategoryDistribution(cv, conceptLimit);
        DynamicPathTree tree = espmModel.constructCategoryTree(categoryDistribution);
        List<SemanticPath> paths = tree.getFilteredSemanticPaths();
        pathMiningNanos += (System.nanoTime() - time);

        time = System.nanoTime();
        List<SemanticPath> results = new ArrayList<>();
        GreedyMWIS greedyMWIS = new GreedyMWIS(treeCache);
        greedyMWIS.buildMWISGraph(paths);
        for (int i = 0; i < topPaths; i++) {
            SemanticPath topPath = greedyMWIS.findTopNode();
            if (topPath != null) {
                results.add(topPath);
            } else {
                break;
            }
        }
        pathPickingNanos += (System.nanoTime()-time);
    }

    public void test() throws WikitException {
        String[] words = new String[]{"Apple", "Microsoft", "Indiana", "Obama", "China", "Computer",
                "Math",
                "Java",
                "Chicago",
                "Beijing"
        };

        //test sentence
        words = new String[]{"Yellow River yields clues to Chinese legend of ancient 'Great Flood'",
        "Turkey issues warrant for preacher Gulen over coup",
        "Clinton's 'exceptional' lead over Trump: One for the history books?",
        "Video shows cabin chaos after Emirates plane crash",
        "China’s crazy elevated bus is already being tested",
        "Israel accuses World Vision's Gaza representative of funding Hamas",
        "Nasdaq vs. NYSE: Why Companies Choose One Over the Other",
        "South Sudan army slams UN report alleging killings, rape",
        "IOC approves entry of 271 Russian athletes for Rio Games",
        "Japan's Emperor Akihito to address nation on Monday following abdication report"};
        for(String w: words) {
            for(int i=0; i<10; i++)
                run(w);
        }

        int wordCount = 0;
        for (String w : words) {
            wordCount += StringUtils.split(w).length;
        }
        System.out.println("total word:" + wordCount + ", " + wordCount/10.0);

        print();
    }

    public void print() {
        System.out.println("Total:" + count);
        System.out.println("Nano Time for esa: " + esaNanos + "\t mining: " + pathMiningNanos + "\t picking: " + pathPickingNanos);

        System.out.println("Second Time for esa: " + esaNanos/1000000000.0/count
                + "\t mining: " + pathMiningNanos/1000000000.0/count
                + "\t picking: " + pathPickingNanos/1000000000.0/count);
        double countPerSecond = count/((esaNanos + pathMiningNanos + pathPickingNanos)/1000000000.0);
        System.out.println("count per second:" + countPerSecond);
        System.out.println("ESA:" + esaNanos/1000.0/count);
        System.out.println("PathMining:" + pathMiningNanos/1000.0/count);
        System.out.println("PathPicking:" + pathPickingNanos/1000.0/count);
    }

    public static void main(String[] args) throws ParseException, WikitException {
        String helpMsg = "usage: SpeedTest -c config.xml";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("c", true, "config file"));

        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("c")) {
            helpFormatter.printHelp(helpMsg, options);
            return;
        }
        Conf conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);

        SpeedTest speedTest = new SpeedTest(conf);
        speedTest.test();
    }
}
