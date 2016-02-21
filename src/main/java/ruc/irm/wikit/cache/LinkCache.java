package ruc.irm.wikit.cache;

import gnu.trove.set.TIntSet;
import org.apache.commons.cli.*;
import ruc.irm.wikit.cache.impl.LinkCacheRedisImpl;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.common.exception.MissedException;
import ruc.irm.wikit.data.dump.impl.PageSequenceDump;
import ruc.irm.wikit.util.ConsoleLoop;

import java.io.File;
import java.io.FileNotFoundException;
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

    public TIntSet getOutlinks(int pageId);

    public int getTotalPages();

    public void writeNeighborsToJson(int pageId, File f) throws
            IOException;

    public void saveGraph(int id1, int id2, File outFile) throws FileNotFoundException, MissedException, IOException;

    void build(PageSequenceDump dump) throws IOException;

    public static void main(String[] args) throws ParseException, IOException, MissedException {
        String helpMsg = "usage: LinkCache -c config.xml -build\n" +
                "LinkCache -c config.xml -lookup" +
                "LinkCache -c config.xml -id1 100 -id2 200";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("c", true, "config file"));
        options.addOption(new Option("build", false, "build link db"));
        options.addOption(new Option("lookup", false, "lookup link info"));
        options.addOption(new Option("id1", true, "the first id"));
        options.addOption(new Option("id2", true, "the second id"));
        options.addOption(new Option("neighbor", true, "given neighbor id, " +
                "write its neighbors to temp file."));
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
                    TIntSet outlinks = linkCache.getOutlinks(pageId);
                    System.out.println("inlinks:" + inlinks);
                    System.out.println("outlinks:" + outlinks);
                }
            });
        } else if (commandLine.hasOption("id1") && commandLine.hasOption("id2")) {
            int id1 = Integer.parseInt(commandLine.getOptionValue("id1"));
            int id2 = Integer.parseInt(commandLine.getOptionValue("id2"));
            File f = new File("/tmp/" + id1 + "_" + id2 + ".csv");
            linkCache.saveGraph(id1, id2, f);
            System.out.println("Save graph to file " +  f.getAbsolutePath());
        } else if (commandLine.hasOption("neighbor")) {
            int id = Integer.parseInt(commandLine.getOptionValue("neighbor"));
            File f = new File("/tmp/" + id + ".json");
            linkCache.writeNeighborsToJson(id, f);
            System.out.println("Save neighbors to file " + f.getAbsolutePath());
        }
    }
}
