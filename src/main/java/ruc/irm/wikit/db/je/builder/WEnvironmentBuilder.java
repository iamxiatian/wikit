package ruc.irm.wikit.db.je.builder;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.BooleanUtils;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.data.dump.WikiPageDump;
import ruc.irm.wikit.data.dump.impl.PageXmlDump;
import ruc.irm.wikit.db.je.WDatabase.DatabaseType;
import ruc.irm.wikit.db.je.WEnvironment;

import java.io.IOException;

/**
 * Build Wiki Environment by Wikipedia dump file, we traverse wiki dump file
 * specified in wikit-conf.xml or user defined config file with parameter
 * name "wiki.dump.file"
 *
 * @author Tian Xia
 * @date Jan 01, 2016 3:37 PM
 */
public class WEnvironmentBuilder {
    private Conf conf = null;
    private boolean overwrite;

    public WEnvironmentBuilder(Conf conf, boolean overwrite) {
        this.conf = conf;
        this.overwrite = overwrite;
    }


    public void build(Conf conf) throws IOException {
        WikiPageDump dump = new PageXmlDump(conf);

        WEnvironment env = new WEnvironment(conf);

        TitleDbFilter articleTitleDbFilter = new TitleDbFilter(env,
                DatabaseType.articlesByTitle, overwrite);
        TitleDbFilter categoryTitleDbFilter = new TitleDbFilter(env,
                DatabaseType.categoriesByTitle, overwrite);
        //PageDbFilter pageDbFilter = new PageDbFilter(env, overwrite);
        dump.traverse(articleTitleDbFilter, categoryTitleDbFilter);

        env.cleanAndCheckpoint() ;
        env.close();
    }

    public static void main(String[] args) throws ParseException, IOException {
        String helpMsg = "usage: WEnvironmentBuilder -c config.xml -overwrite" +
                " true " +
                "-build";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("c", true, "config file"));
        options.addOption(new Option("build", false, "build Environment"));
        options.addOption(new Option("overwrite", true, "true or false"));

        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("c") || !commandLine.hasOption("overwrite")) {
            helpFormatter.printHelp(helpMsg, options);
            return;
        }

        Conf conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);
        boolean overwrite = BooleanUtils.toBoolean(commandLine.getOptionValue("overwrite"));

        if(commandLine.hasOption("build")) {
            WEnvironmentBuilder builder = new WEnvironmentBuilder(conf, overwrite);
            builder.build(conf);
            System.out.println("I'm DONE for build WEnvironment!");
        } else {
            System.out.println("Please specify -build parameter to build the " +
                    "environment");
        }
    }

}
