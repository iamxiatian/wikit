package ruc.irm.wikit.cache;

import org.apache.commons.cli.*;
import ruc.irm.wikit.cache.impl.RedirectCacheRedisImpl;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * @author Tian Xia
 * @date Feb 03, 2016 11:31 AM
 */
public interface RedirectCache extends Cache {

    public int getRedirectToId(String fromName, int notExistId);

    public Collection<String> getRedirectNames(int pageId);

    /**
     * Please use <code>WikiExtractor</code> to generate redirected text file first,
     * then use that file to build redirect cache.
     *
     * @param redirectTextFile
     * @throws IOException
     */
    public void build(File redirectTextFile) throws IOException;


    public static void main(String[] args) throws ParseException, IOException {
        String helpMsg = "usage: RedirectCache -c config.xml";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("c", true, "config file"));
        options.addOption(new Option("build", false, "Build cache."));
        options.addOption(new Option("f", true, "redirect text file"));

        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("c") || !commandLine.hasOption("f")) {
            helpFormatter.printHelp(helpMsg, options);
            return;
        }

        Conf conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);
        File f = new File(commandLine.getOptionValue("f"));

        RedirectCache cache = new RedirectCacheRedisImpl(conf);
        cache.build(f);
        System.out.println("I'm DONE!");
    }
}
