package ruc.irm.wikit.data.export;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import org.apache.commons.cli.*;
import ruc.irm.wikit.cache.ArticleCache;
import ruc.irm.wikit.cache.CategoryCache;
import ruc.irm.wikit.cache.impl.CategoryCacheRedisImpl;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.common.exception.MissedException;
import ruc.irm.wikit.data.dump.WikiPageDump;
import ruc.irm.wikit.data.dump.impl.PageSequenceDump;
import ruc.irm.wikit.data.dump.impl.PageXmlDump;
import ruc.irm.wikit.data.dump.parse.WikiPage;
import ruc.irm.wikit.esa.concept.ConceptCache;
import ruc.irm.wikit.esa.concept.ConceptCacheRedisImpl;
import ruc.irm.wikit.espm.graph.CategoryTreeGraph;
import ruc.irm.wikit.espm.graph.CategoryTreeGraphRedisImpl;
import ruc.irm.wikit.util.ProgressCounter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 导出指定类别和子类别下所出现的所有维基百科词条
 *
 * @author Tian Xia
 * @date Jul 19, 2016 10:51
 */
public class PageGraphExporter {
    //private CategoryTreeGraph treeGraph = null;
    private Conf conf = null;
    private CategoryCache categoryCache = null;
    private CategoryTreeGraph treeCache = null;
    private Set<String> visitedCategories = new HashSet<>();
    private ArticleCache articleCache = null;
    private ConceptCache conceptCache = null;

    public PageGraphExporter(Conf conf) throws MissedException {
        this.conf = conf;
        this.categoryCache = new CategoryCacheRedisImpl(conf);
        this.treeCache = new CategoryTreeGraphRedisImpl(conf);
        this.conceptCache = new ConceptCacheRedisImpl(conf);
    }

    /**
     * 导出指定类别和子类别下所出现的所有维基百科词条，该类别树下所出现的所有文章保存在了文件中
     *
     */
    public void exportPageTitleByCategoryTree() throws MissedException, IOException {
        //CategoryTreeGraph treeGraph = new CategoryTreeGraphRedisImpl(conf);
        this.visitedCategories.clear();
        //this.visitedIds.add(rootId);

        String[] startNames = new String[]{
                "Applied genetics",
                "Applied linguistics",
                "Applied mathematics",
                "Computational science",
                "Computer science",
                "Engineering",
                "Health sciences",
                "Information science",
                "Mathematical sciences",
                "Measurement",
                "Military science",
                "Pharmaceutical sciences",
                "Systems science",
                "Technology",
                "Management"
        };

        Set<Integer> parentIds = new HashSet<>();
        for (String name : startNames) {
            int id = categoryCache.getIdByName(name);
            parentIds.add(id);
        }


        Set<String> visitedNames = collectCategoryIds(parentIds);

        File f = new File("category_name.txt");
        PrintWriter writer = new PrintWriter(Files.newWriter(f, Charsets.UTF_8));
        for (String name : visitedNames) {
            writer.println(name);
        }
        writer.close();
    }


    private static Pattern pattern1 = Pattern.compile(" in [\\d]{4}s?$");
    private static Pattern pattern2 = Pattern.compile("\\d+");

    private boolean skip(String name) {
        if (name.equalsIgnoreCase("Years") || name.equalsIgnoreCase("Decades"))
            return true;
        if (pattern1.matcher(name).find())
            return true;
        if (pattern2.matcher(name).find())
            return true;


        return false;
    }

    /**
     * 按照广度优先遍历方式输出一个节点下的所有路径
     *
     * @throws MissedException
     */
    private Set<String> collectCategoryIds(Set<Integer> startIds) throws MissedException {
        Set<String> visited = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();
        queue.addAll(startIds);

        while (!queue.isEmpty()) {
            int parentId = queue.poll();

            // 类别名称
            String name = categoryCache.getNameById(parentId);

            if(visited.contains(name)) continue;

            if (skip(name)) continue;

            // 标记为已经访问
            visited.add(name);

            Set<Integer> childIds = categoryCache.getChildIds(parentId);

            if (childIds != null && childIds.size() > 0) {
                for (int id : childIds) {
                    queue.add(id);
                }
            }
        }
        return visited;
    }

    public void generateArticles(WikiPageDump dump) throws IOException {
        Set<String> cache = new HashSet<>();
        cache.addAll(Files.readLines(new File("category_name.txt"), Charsets.UTF_8));

        try(PrintWriter writer = new PrintWriter("article_info.txt", "utf-8")) {
            dump.open();
            ProgressCounter counter = new ProgressCounter();
            while (dump.hasNext()) {
                counter.increment();
                WikiPage page = dump.next();
                if (page.isArticle() && !page.isRedirect() && page.getInlinkCount()>10) {
                    Set<String> categories = page.getCategories();
                    for (String c : categories) {
                        if (cache.contains(c)) {
                            //output this article's basic information
                            writer.println(page.getId());
                            writer.println(page.getTitle());
                            writer.println(page.getInlinkCount());

                            //output categories
                            int count = 0;
                            for (String c2: categories) {
                                if(count++>0) writer.print("|");

                                writer.print(c2);
                            }
                            writer.println();

                            //output outlinks
                            count = 0;
                            for (String link: page.getInternalLinks()) {
                                if(count++>0) writer.print("|");

                                writer.print(link);
                            }
                            writer.println();

                            writer.println();
                            break;
                        }
                    }
                }
            }
            counter.done();
            dump.close();
        }
    }


    public static void main(String[] args) throws ParseException, MissedException, IOException {
//        PageGraphExporter exporter = new PageGraphExporter(ConfFactory.defaultConf());
//        System.out.println(exporter.skip("2"));
        String helpMsg = "Usage: \n PageGraphExporter -c conf.xml";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("c", true, "config file"));

        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("c")) {
            helpFormatter.printHelp(helpMsg, options);
            return;
        }

        Conf conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);

        PageGraphExporter exporter = new PageGraphExporter(conf);
        //exporter.exportPageTitleByCategoryTree();
        WikiPageDump dump = new PageSequenceDump(conf, "/home/xiatian/esa/english/data/wiki/seq/seq-article.gz");
        exporter.generateArticles(dump);
        System.out.println("I'm DONE.");
    }
}
