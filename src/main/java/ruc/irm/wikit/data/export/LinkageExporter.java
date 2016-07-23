package ruc.irm.wikit.data.export;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.common.exception.MissedException;
import ruc.irm.wikit.data.dump.WikiPageDump;
import ruc.irm.wikit.data.dump.impl.PageSequenceDump;
import ruc.irm.wikit.data.dump.parse.WikiPage;
import ruc.irm.wikit.espm.graph.CategoryTreeGraph;
import ruc.irm.wikit.espm.graph.CategoryTreeGraphRedisImpl;
import ruc.irm.wikit.util.ProgressCounter;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * 输出维基百科的链接对到文件中
 *
 * @author Tian Xia
 * @date Jul 19, 2016 10:51
 */
public class LinkageExporter {
    //private CategoryTreeGraph treeGraph = null;
    private Conf conf = null;

    public LinkageExporter(Conf conf) {
        this.conf = conf;
    }

    /**
     * 把指定的维基百科词条集合中出现的页面的入链和出链关系输出到文件中, 一对标题中必须有一个标题在kernelPages集合中，所有的标题必须
     * 都在allPages中出现
     *
     */
    public void export(Set<String> kernelPages, Set<String> allPages, WikiPageDump dump, File linkFile) throws IOException {
        dump.open();
        PrintWriter linkWriter = new PrintWriter(new FileWriter(linkFile));

        ProgressCounter counter = new ProgressCounter();
        while (dump.hasNext()) {
            WikiPage page = dump.next();
            counter.increment();
            if (!page.isArticle()) {
                continue;
            }

            String current = page.getTitle();
            List<String> links = page.getInternalLinks();
            if (kernelPages.contains(page.getTitle())) {
                for (String link : links) {
                    if(allPages.contains(link))
                        linkWriter.println(current + ", " + link);
                }
            } else {
                for (String link : links) {
                    if(kernelPages.contains(link)) {
                        linkWriter.println(current + ", " + link);
                    }
                }
            }
        }
        counter.done();
        dump.close();
        linkWriter.close();
    }

    private Set<String> loadCandidatePages(File f) throws IOException {
        Set<String> bag = new HashSet<>();
        BufferedReader reader = new BufferedReader(new FileReader(f));
        String line = null;
        while ((line = reader.readLine()) != null) {
            if (StringUtils.isBlank(line)) {
                continue;
            }

            int pos = line.indexOf(", ");
            String title = line.substring(0, pos);
            bag.add(title);
        }
        System.out.println("Load " + bag.size() + " kernel concepts.");
        return bag;
    }

    private void saveAllPagesToFile(WikiPageDump dump, File f) throws IOException {
        dump.open();
        PrintWriter writer = new PrintWriter(new FileWriter(f));

        ProgressCounter counter = new ProgressCounter();
        while (dump.hasNext()) {
            WikiPage page = dump.next();
            counter.increment();
            if (!page.isArticle()) {
                continue;
            }

            writer.println(page.getTitle());
        }
        counter.done();
        dump.close();
        writer.close();
    }



    public static void main(String[] args) throws ParseException, MissedException, IOException {
        String helpMsg = "Usage: \n" +
                "\tLinkageExporter -c config.xml -af xxx -df xxx -lf xxx\n";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("c", true, "config file"));
        options.addOption(new Option("af", true, "kernel article - category file."));
        options.addOption(new Option("df", true, "wiki dump file."));
        options.addOption(new Option("lf", true, "output link file."));
        options.addOption(new Option("tf", true, "all valid wiki title file."));

        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("c")) {
            helpFormatter.printHelp(helpMsg, options);
            return;
        }


        Conf conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);
        LinkageExporter exporter = new LinkageExporter(conf);

        if (commandLine.hasOption("df") && commandLine.hasOption("tf") && !commandLine.hasOption("lf")) {
            String dumpFile = commandLine.getOptionValue("df");
            File wikiTitleFile = new File(commandLine.getOptionValue("tf"));
            WikiPageDump dump = new PageSequenceDump(conf, dumpFile);
            exporter.saveAllPagesToFile(dump, wikiTitleFile);
        }

        //exporter.loadCandidatePages(new File("/home/xiatian/data/wiki.txt"));
        if (commandLine.hasOption("af") && commandLine.hasOption("df") && commandLine.hasOption("lf")
                && commandLine.hasOption("tf")) {
            File kernelFile = new File(commandLine.getOptionValue("af"));
            String dumpFile = commandLine.getOptionValue("df");
            File linkFile = new File(commandLine.getOptionValue("lf"));
            File wikiTitleFile = new File(commandLine.getOptionValue("tf"));

            WikiPageDump dump = new PageSequenceDump(conf, dumpFile);
            Set<String> bag = exporter.loadCandidatePages(kernelFile);

            Set<String> allPages = Sets.newHashSet(FileUtils.readLines(wikiTitleFile));

            exporter.export(bag, allPages, dump, linkFile);
            System.out.println("I'm DONE!");
        }
    }
}
