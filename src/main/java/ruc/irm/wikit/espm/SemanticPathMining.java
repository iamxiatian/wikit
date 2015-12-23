package ruc.irm.wikit.espm;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.math.NumberUtils;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.common.exception.WikitException;
import ruc.irm.wikit.esa.ESAModel;
import ruc.irm.wikit.esa.ESAModelImpl;
import ruc.irm.wikit.esa.concept.ConceptCacheRedisImpl;
import ruc.irm.wikit.esa.concept.vector.ConceptIterator;
import ruc.irm.wikit.esa.concept.vector.ConceptVector;
import ruc.irm.wikit.espm.impl.CategoryDistribution;
import ruc.irm.wikit.espm.impl.DynamicPathTree;
import ruc.irm.wikit.espm.impl.SemanticPathMiningWikiImpl;
import ruc.irm.wikit.util.ConsoleLoop;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.SortedMap;

/**
 * Explicit Semantic Path Mining (ESPM) Model, find semantic path for any given
 * text based on ESA and wiki category
 * <p>
 * User: xiatian
 * Date: 7/16/14
 * Time: 11:30 PM
 */
public interface SemanticPathMining {

    /**
     * Given concept vector which represents a text, calculate the probability
     * distribution of each possible categories.
     *
     * @param cv
     * @param topN
     * @return
     */
    public SortedMap<Integer, Double> getCategoryDistribution(ConceptVector cv,
                                                              int topN)
            throws WikitException;

    /**
     * Print the category distribution as string
     *
     * @param categoryDistribution
     */
    public String printCategoryDistribution(SortedMap<Integer, Double> categoryDistribution);

    /**
     * Construct category tree, semantic path analysis based on this tree
     *
     * @param categoryDistribution
     * @return
     */
    public DynamicPathTree constructCategoryTree(SortedMap<Integer, Double> categoryDistribution) throws WikitException;

    public List<SemanticPath> getSemanticPaths(DynamicPathTree categoryTree, int topN) throws WikitException;

    public CategoryDistribution getLevelOneDistribution(DynamicPathTree categoryTree) throws WikitException;

    /**
     * Get topN semantic paths by given category tree, this method equals:
     * <p>
     * ESAModel.getCombinedVector(text)
     * getCategoryDistribution(ConceptVector cv, int keptPageVectorCount)
     * constructCategoryTree(categoryDistribution)
     * getSemanticPaths(categoryTree, int topN)
     *
     * @param text
     * @param keptPageVectorCount
     * @param topPaths
     * @return
     * @throws WikitException
     */
    public List<SemanticPath> getSemanticPaths(String text,
                                               int keptPageVectorCount,
                                               int topPaths) throws WikitException;

    public List<SemanticPath> getSemanticPaths(ConceptVector cv,
                                               int conceptLimit,
                                               int topPaths) throws WikitException;

    /**
     * GML (Graph Modeling Language) is a text file format supporting network
     * data with a very easy syntax. It is used by Graphlet, Pajek, yEd,
     * LEDA and NetworkX.
     */
    public void exportAsGML(File gmlFile, List<SemanticPath> paths);


    public static void main(String[] args) throws ParseException, WikitException {

        String helpMsg = "usage: SmanticPathMining -c config.xml";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("c", true, "config file"));
        options.addOption(new Option("limit", true, "concept limit number," +
                " default is 20"));

        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("c")) {
            helpFormatter.printHelp(helpMsg, options);
            return;
        }
        final int cptLimit = NumberUtils.toInt(commandLine.getOptionValue("limit"), 20);

        Conf conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);

        SemanticPathMining ESPM = new SemanticPathMiningWikiImpl(conf);
        ESAModel esaModel = new ESAModelImpl(conf);
        ConceptCacheRedisImpl conceptCache = new ConceptCacheRedisImpl(conf);

        ConsoleLoop.loop(new ConsoleLoop.Handler() {
            private String[] demos = new String[]{
                    " Construction of world's biggest optical telescope " +
                            "starts  with a bang: The Giant Magellan " +
                            "Telescope  Organization announced today that its" +
                            " 11 international partners have committed more " +
                            " than \\$500 million to begin construction of " +
                            "the first of a new generation of extremely large" +
                            " telescopes. Once it is built, the Giant " +
                            "Magellan Telescope is poised to be the largest " +
                            "optical telescope in the world.",
                    "Our Solar System May Have Had A Fifth 'Giant' Planet:The" +
                            " four gas giant planets in our solar system -- " +
                            "Jupiter, Saturn, Uranus, and Neptune -- may have" +
                            " a long-lost relative. According to a new study," +
                            " our system was once home to a fifth gas giant " +
                            "that suddenly vanished some 4 billion years ago " +
                            "after a run-in with Neptune.\n Indirect evidence" +
                            " for this lost world is seen in a strange " +
                            "cluster of icy objects -- called the \"kernel\" " +
                            "-- in the Kuiper Belt. That's the vast region of" +
                            " primordial debris that encircles the sun beyond" +
                            " the orbit of Neptune.",
                    "Iraq’s Top Shiite Cleric Calls for New Government: " +
                            "Iraq’s most influential Shiite cleric suggested " +
                            "on Friday\n that Prime MinisterNouri al-Maliki " +
                            "needed to recast his approach or step down, " +
                            "adding his powerful voice to growing criticism\n" +
                            "of the Shiite-dominated government’s leader",
                    "iPhone 6s base model to have 16GB of storage.",
                    "It’s game day in V Nation! Come out and support #UTRGV Women’s Soccer at 5 p.m. and Men’s Soccer at 7 p.m."
            };

            @Override
            public void handle(String text) throws IOException {
                try {
                    //demo text, for quick test purpose.
                    if (text.startsWith("demo")) {
                        int index = 0;
                        if (text.length() > 4)
                            index = Integer.parseInt(text.substring(4));
                        text = demos[index];
                        System.out.println(text);
                    }

                    ConceptVector cv = esaModel.getCombinedVector(text, cptLimit); //20
                    if (cv == null) {
                        System.out.println("No terms occurred in the input " +
                                "text, current limit is " + cptLimit);
                        return;
                    }
                    ConceptIterator it = cv.orderedIterator();

                    int count = 0;
                    while (it.next() && count < cptLimit) {
                        System.out.println((count + 1) + "\t"
                                + conceptCache.getOutIdById(it.getId()) + "\t\t"
                                + conceptCache.getNameById(it.getId()) + "\t\t"
                                + it.getValue());
                        count++;
                        if (count >= 6) break;
                    }

                    SortedMap<Integer, Double> probability = ESPM.getCategoryDistribution(cv, cptLimit);
                    //System.out.println(ESPM.printCategoryDistribution(probability));

                    System.out.println("Final valid paths:");
                    List<SemanticPath> paths = ESPM.getSemanticPaths(ESPM.constructCategoryTree(probability), 20);
                    count = 0;
                    for (SemanticPath path : paths) {
                        System.out.println("------------------------------");
                        System.out.print((++count) + "\t");
                        System.out.println(path.getPathString('/'));
                        System.out.print("\t\t\t");
                        System.out.println(path.getAvgWeight());
                        if (count >= 6) break;
                    }
                    ESPM.exportAsGML(new File("/tmp/path.gexf"), paths);
                } catch (WikitException e) {
                    e.printStackTrace();
                }
            }
        });

    }

}
