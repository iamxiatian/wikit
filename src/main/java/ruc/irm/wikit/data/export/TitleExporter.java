package ruc.irm.wikit.data.export;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import org.apache.commons.cli.*;
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
import java.util.Set;

import static java_cup.emit.prefix;

/**
 * Export Wiki titles
 * @author Tian Xia
 * @date Jul 19, 2016 10:51
 */
public class TitleExporter {
    //private CategoryTreeGraph treeGraph = null;
    private Conf conf = null;

    public TitleExporter(Conf conf) {
        this.conf = conf;
    }

    private void exportSubCategory(PrintWriter writer, String fromCategoryName) throws MissedException {
        CategoryTreeGraph treeGraph = new CategoryTreeGraphRedisImpl(conf);
        int startId = treeGraph.getIdByName("Technology", 0);
        exportSubCategory(writer, startId);
    }

    /**
     * 把某个类别下的所有类别名称输出到文件中，以Technology为例，输出格式为：
     * Technology
     * Computer
     * mobile
     * ...
     *
     * @param writer
     * @param startCategoryId
     */
    private void exportSubCategory(PrintWriter writer, int startCategoryId) throws MissedException {
        CategoryTreeGraph treeGraph = new CategoryTreeGraphRedisImpl(conf);
        String name = treeGraph.getNameById(startCategoryId, null);
        if(name == null)
            return;

        Set<Integer> childIds = treeGraph.getChildIds(startCategoryId);

        if (childIds == null || childIds.size() == 0) {
            if(treeGraph.getConceptCount(startCategoryId) > 5) {
                writer.println(name);
            }
        } else {
            writer.println(name);
            for (int id : childIds) {
                exportSubCategory(writer, id);
            }
        }
    }

    public void exportArticleTitle(WikiPageDump dump, File inCategoryTitleFile, File outArticleTitleFile) throws IOException {
        Set<String> acceptCategories = Sets.newHashSet(Files.readLines(inCategoryTitleFile, Charsets.UTF_8));
        dump.open();
        PrintWriter writer = new PrintWriter(new FileWriter(outArticleTitleFile));

        ProgressCounter counter = new ProgressCounter();
        while (dump.hasNext()) {
            WikiPage page = dump.next();
            counter.increment();
            if (!page.isArticle()) {
                continue;
            }

            Set<String> categories = page.getCategories();
            for (String c : categories) {
                if (acceptCategories.contains(c)) {
                    writer.println(page.getTitle() + ", " + c);
                }
            }

        }
        counter.done();
        dump.close();
        writer.close();
    }

    public static void main(String[] args) throws ParseException, MissedException, IOException {
        String helpMsg = "Usage: \n" +
                "\t1) Extract category title to file:\n" +
                "\t\tTitleExporter -c config.xml -cf output_category_file -ec\n" +
                "\t2) Extract article title under specified category:\n" +
                "\t\tTitleExporter -c config.xml  -df wiki_sequence_dump -cf input_category_file -af output_article_file -ea";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("c", true, "config file"));
        options.addOption(new Option("cf", true, "category file."));
        options.addOption(new Option("ec", false, "export category to file."));
        options.addOption(new Option("af", true, "article file."));
        options.addOption(new Option("df", true, "wiki dump file."));
        options.addOption(new Option("ea", false, "export category to file."));


        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("c")) {
            helpFormatter.printHelp(helpMsg, options);
            return;
        }

        Conf conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);
        TitleExporter exporter = new TitleExporter(conf);
        if(commandLine.hasOption("ec") && commandLine.hasOption("cf")) {
            System.out.println("save categories to file:" + commandLine.getOptionValue("cf"));
            File cf = new File(commandLine.getOptionValue("cf"));
            PrintWriter writer = new PrintWriter(Files.newWriter(cf, Charsets.UTF_8));
            exporter.exportSubCategory(writer, "Technology");
            writer.close();
            return;
        }

        if (commandLine.hasOption("ea") && commandLine.hasOption("cf") && commandLine.hasOption("af")) {
            File categoryFile = new File(commandLine.getOptionValue("cf"));
            File articleFile = new File(commandLine.getOptionValue("af"));
            String dumpFile = commandLine.getOptionValue("df");
            WikiPageDump dump = new PageSequenceDump(conf, dumpFile);
            exporter.exportArticleTitle(dump, categoryFile, articleFile);
            System.out.println("I'm DONE!");
        }
    }
}
