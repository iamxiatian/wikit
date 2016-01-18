package ruc.irm.wikit.data.dump.impl;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.data.dump.WikiPageDump;
import ruc.irm.wikit.data.dump.filter.FirstStopFilter;
import ruc.irm.wikit.data.dump.filter.SecondStopFilter;
import ruc.irm.wikit.data.dump.filter.SplitSequenceFilter;
import ruc.irm.wikit.data.dump.parse.WikiPage;
import ruc.irm.wikit.data.dump.parse.WikiPageFilter;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 * Wiki page dump stored in sequence file format. We convert original xml
 * dumps to this binary format for the consideration of speed. Unimportant
 * pages are also removed during the dump construction phase.
 *
 * @author Tian Xia
 * @date Jul 31, 2015 2:28 PM
 */
public class PageSequenceDump extends WikiPageDump {
    private static final Logger LOG = LoggerFactory.getLogger(PageSequenceDump.class);

    private Conf conf = null;
    private DataInputStream input = null;
    private WikiPage lastPage = null;

    public PageSequenceDump(Conf conf) {
        this.conf = conf;
        this.dumpFile = conf.get("wiki.dump.seq.file", "seq.gz");
    }

    public PageSequenceDump(Conf conf, String dumpFile) {
        this.conf = conf;
        this.dumpFile = dumpFile;
    }

    @Override
    public void open() throws IOException {
        if (!new File(dumpFile).exists()) {
            throw new IOException("sequence dump file does not exist " +
                    "==>" + dumpFile + "\n please check conf " +
                    "parameter wiki.dump.seq.file");
        }

        if (dumpFile.endsWith(".gz")) {
            this.input = new DataInputStream(new GZIPInputStream(new
                    FileInputStream(dumpFile)));
        } else {
            this.input = new DataInputStream(new FileInputStream(dumpFile));
        }
        lastPage = WikiPage.readFrom(input, conf);
    }

    @Override
    public void close() throws IOException {
        if (input != null) {
            input.close();
        }
    }

    @Override
    public boolean hasNext() {
        return lastPage!=null;
    }

    @Override
    public WikiPage next() {
        WikiPage oldPage = lastPage;
        lastPage = WikiPage.readFrom(input, conf);
        return oldPage;
    }

    public static void main(String[] args) throws ParseException, IOException {
        String helpMsg = "usage: PageSequenceDump -c config.xml";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("c", true, "config file"));
        options.addOption(new Option("ba", false, "build first and second " +
                "dumps"));
        options.addOption(new Option("b1", false, "build first sequence " +
                "dumps"));
        options.addOption(new Option("b2", false, "build second sequence " +
                "dumps"));
        options.addOption(new Option("split", false, "split the seq " +
                "dump file to article file and category file"));
        options.addOption(new Option("view", false, "view dump content"));

        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("c")) {
            helpFormatter.printHelp(helpMsg, options);
            return;
        }

        Conf conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);
        System.out.println("minLinks:" + conf.getInt("wiki.stop.filter.min.links", 10));
        System.out.println("minWords:" + conf.getInt("wiki.stop.filter.min.words", 50));

        if(commandLine.hasOption("ba")) {
            PageXmlDump pxd = new PageXmlDump(conf);
            pxd.traverse(new FirstStopFilter(conf));
            LOG.warn("DONE for build first sequence dump!");

            LOG.warn("automatic set seq1 as default sequence file for traverse.");
            String seq1 = conf.get("wiki.dump.file.seq1");
            conf.set("wiki.dump.seq.file", seq1);

            PageSequenceDump psd = new PageSequenceDump(conf);
            psd.traverse(new SecondStopFilter(conf));
            LOG.warn("DONE for build second sequence dump!");
        } else if (commandLine.hasOption("b1")) {
            PageXmlDump pxd = new PageXmlDump(conf);
            pxd.traverse(new FirstStopFilter(conf));
            LOG.debug("DONE for build first sequence dump!");
        } else if (commandLine.hasOption("b2")) {
            LOG.warn("automatic set seq1 as default sequence file for traverse.");
            String seq1 = conf.get("wiki.dump.file.seq1");
            conf.set("wiki.dump.seq.file", seq1);

            PageSequenceDump psd = new PageSequenceDump(conf);
            psd.traverse(new SecondStopFilter(conf));
            LOG.warn("DONE for build second sequence dump!");
        }

        //reset config
        conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);
        if(commandLine.hasOption("split")){
            LOG.warn("split the seq file into article and category separately");
            PageSequenceDump psd = new PageSequenceDump(conf);
            psd.traverse(new SplitSequenceFilter(conf));
            LOG.warn("DONE for split process.");
        }

        if (commandLine.hasOption("view")) {
            final PageSequenceDump psd = new PageSequenceDump(conf);

            psd.traverse(new WikiPageFilter() {
                private Set<Integer> ids = new HashSet<>();

                int visited = 0;
                @Override
                public void process(WikiPage wikiPage, int index) {
                    visited++;
                    if (ids.contains(wikiPage.getId())) {
                        System.out.println("______________________");
                        System.out.println(wikiPage.getId() + "\t\t" + wikiPage
                                .getTitle());
                        if (wikiPage.isCategory()) {
                            System.out.println("category title:" + wikiPage.getCategoryTitle());
                        }
                        System.out.println("inlinks:" + wikiPage.getInlinkCount());
                        System.out.println("Categories:" + wikiPage.getCategories());
                        System.out.println("Links:" + wikiPage.getInternalLinks());


                        System.out.println("This id has already existed.");
                        System.out.print("type exit to terminate>>>");
                        Scanner in = new Scanner(System.in);
                        String line = in.next();
                        if (line.equalsIgnoreCase("exit")) {
                            return;
                        }
                    } else {
                        if (ids.size() > 2000) {
                            System.out.print(wikiPage.getId() + " ");
                            ids.clear();
                            System.out.println();
                        }
                        ids.add(wikiPage.getId());
                    }
                }
            });
            LOG.warn("Bye!");
        }
    }

}
