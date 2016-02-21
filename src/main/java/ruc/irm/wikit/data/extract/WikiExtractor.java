package ruc.irm.wikit.data.extract;

import org.apache.commons.cli.*;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.data.dump.WikiPageDump;
import ruc.irm.wikit.data.dump.impl.PageXmlDump;

import java.io.File;
import java.io.IOException;

/**
 * @author Tian Xia
 * @date Feb 01, 2016 7:56 PM
 */
public class WikiExtractor {

    public static void main(String[] args) throws ParseException, IOException {
        String helpMsg = "usage: WikiExtractor -c config.xml";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("c", true, "config file"));
        options.addOption(new Option("link", true, "Link output json file"));
        options.addOption(new Option("redirect", true, "redirect output text " +
                "file"));
        options.addOption(new Option("label", true, "label output text file"));

        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("c") || !commandLine.hasOption("link")
                ||!commandLine.hasOption("redirect")
                ||!commandLine.hasOption("label")) {
            helpFormatter.printHelp(helpMsg, options);
            return;
        }

        Conf conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);
        File linkFile = new File(commandLine.getOptionValue("link"));
        File rdFile = new File(commandLine.getOptionValue("redirect"));
        File labelFile = new File(commandLine.getOptionValue("label"));

        WikiPageDump dump = new PageXmlDump(conf);
        ExtractLinksFilter linkFilter = new ExtractLinksFilter(conf, linkFile);
        ExtractLabelsFilter labelFilter = new ExtractLabelsFilter(conf, labelFile);
        ExtractRedirectsFilter rdFilter = new ExtractRedirectsFilter(conf, rdFile);

        dump.traverse(linkFilter, labelFilter, rdFilter);
        System.out.println("I'm DONE!");
    }
}
