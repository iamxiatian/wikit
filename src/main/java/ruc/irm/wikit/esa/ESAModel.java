package ruc.irm.wikit.esa;

import org.apache.commons.cli.*;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.common.exception.WikitException;
import ruc.irm.wikit.esa.concept.ConceptCache;
import ruc.irm.wikit.esa.concept.ConceptCacheRedisImpl;
import ruc.irm.wikit.esa.concept.vector.ConceptIterator;
import ruc.irm.wikit.esa.concept.vector.ConceptVector;
import ruc.irm.wikit.util.text.analysis.ESAAnalyzer;

import java.io.IOException;
import java.io.StringReader;
import java.util.Scanner;

/**
 * ESA model, provide the core method of ESA
 *
 * User: xiatian
 * Date: 7/5/14
 * Time: 12:36 PM
 */
public interface ESAModel {
    /**
     * Get the concept vector to represents current text.
     *
     * @param text
     * @return Returns concept vector results exist, otherwise null
     */
    public ConceptVector getConceptVector(String text) throws WikitException;

    /**
     * Trim vector to keep only most important top LIMIT concepts
     * @param cv
     * @param LIMIT
     * @return
     */
    public ConceptVector trimVector(ConceptVector cv, int LIMIT);

    /**
     * Computes secondary interpretation vector of regular features by links
     *
     * @param cv
     * @param ALPHA
     * @param limit
     * @return
     * @throws java.sql.SQLException
     */
    public ConceptVector getLinkVector(ConceptVector cv, double ALPHA, int limit) ;

    /**
     * computes secondary interpretation vector of regular features by links
     * with default ALPHA parameter(0.5)
     * @param cv
     * @param limit
     * @return
     */
    public ConceptVector getLinkVector(ConceptVector cv, int limit);

    /**
     * Get concept vector, the links between concepts are taken into account.
     */
    public ConceptVector getCombinedVector(String text) throws WikitException;

    /**
     * Get concept vector and take link relationship into account, return top limit concepts.
     * @param text
     * @param limit
     * @return
     * @throws WikitException
     */
    public ConceptVector getCombinedVector(String text, int limit) throws WikitException;

    public static void main(String[] args) throws ParseException, WikitException, IOException {
        String helpMsg = "usage: ESAModel -c config.xml";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("c", true, "config file"));
        options.addOption(new Option("build", false, "Build ESA model."));

        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("c")) {
            helpFormatter.printHelp(helpMsg, options);
            return;
        }

        Conf conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);
        ESAModel model = new ESAModelImpl(conf);
        ConceptCache cache = new ConceptCacheRedisImpl(conf);

        String text = null;
        Scanner scanner = new Scanner(System.in);

        ESAAnalyzer analyzer = new ESAAnalyzer(conf);
        System.out.print(">>>");
        while ((text = scanner.nextLine()) != null) {
            if (text.equalsIgnoreCase("exit")) {
                System.out.println("Bye!");
                break;
            }

            //output tokens extracted from text
            TokenStream tokenStream = analyzer.tokenStream("contents", new StringReader(text));
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                CharTermAttribute t = tokenStream.getAttribute(CharTermAttribute.class);
                String term = t.toString();
                System.out.print(term + " ");
            }
            System.out.println();

            ConceptVector cv = model.getCombinedVector(text, 50);
            if (cv == null) {
                System.out.println("No valid concept");
            } else {
                ConceptIterator it = cv.orderedIterator();
                int count = 0;
                while (it.next() && count<200) {
                    count++;
                    int id = it.getId();
                    String outId = cache.getOutIdById(it.getId());
                    double value = it.getValue();

                    System.out.println(count + "\t" + cache.getNameById(id) +
                            "\t\t" + outId + "/" + value);
                }
            }
            System.out.print(">>>");
        }
    }
}
