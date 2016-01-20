package ruc.irm.wikit.cache;

import gnu.trove.set.TIntSet;
import org.apache.commons.cli.*;
import ruc.irm.wikit.cache.impl.LinkCacheRedisImpl;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.data.dump.impl.PageSequenceDump;
import ruc.irm.wikit.util.ConsoleLoop;

import java.io.File;
import java.io.IOException;

/**
 * The Cache which provide link access method. Be sure to build the link db
 * first.
 *
 * @author Tian Xia
 * @date Jan 19, 2016 10:37 AM
 */
public interface LinkCache {
    public TIntSet getInlinks(int pageId);

    public TIntSet getOutLinks(int pageId);

    public int getTotalPages();

    void build(PageSequenceDump dump) throws IOException;

    public static void main(String[] args) throws ParseException, IOException {
        String helpMsg = "usage: LinkDb -c config.xml";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("c", true, "config file"));
        options.addOption(new Option("build", false, "build link db"));
        options.addOption(new Option("lookup", false, "lookup link info"));
        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("c")) {
            helpFormatter.printHelp(helpMsg, options);
            return;
        }

        Conf conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);
        LinkCache linkCache = new LinkCacheRedisImpl(conf);
        if (commandLine.hasOption("build")) {
            String dumpFile = conf.get("wiki.dump.seq.file.article");
            System.out.println("Start to build link database from " + dumpFile);
            File f = new File(dumpFile);
            if(!f.exists()) {
                System.out.println("Dump file does not exist! terminate.");
            } else if(!f.canRead()) {
                System.out.println("Dump file can not be read! terminate.");
            } else {
                PageSequenceDump dump = new PageSequenceDump(conf, dumpFile);
                linkCache.build(dump);
            }
            System.out.println("Build process complete.");
        } else if (commandLine.hasOption("lookup")) {
            System.out.println("input page id to view in and out links.");
            ConsoleLoop.loop(new ConsoleLoop.Handler() {
                @Override
                public void handle(String input) throws IOException {
                    int pageId = Integer.parseInt(input);
                    TIntSet inlinks = linkCache.getInlinks(pageId);
                    TIntSet outlinks = linkCache.getOutLinks(pageId);
                    System.out.println("inlinks:" + inlinks);
                    System.out.println("outlinks:" + outlinks);
                }
            });
        }
    }
}
