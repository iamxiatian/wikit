package ruc.irm.wikit.data.export;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.tuple.Pair;
import ruc.irm.wikit.cache.ArticleCache;
import ruc.irm.wikit.cache.CategoryCache;
import ruc.irm.wikit.cache.impl.CategoryCacheRedisImpl;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.common.exception.MissedException;
import ruc.irm.wikit.data.dump.WikiPageDump;
import ruc.irm.wikit.data.dump.impl.PageSequenceDump;
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

/**
 * Export Wiki titles
 *
 * @author Tian Xia
 * @date Jul 19, 2016 10:51
 */
public class TitleExporter {
    //private CategoryTreeGraph treeGraph = null;
    private Conf conf = null;
    private CategoryCache categoryCache = null;
    private CategoryTreeGraph treeCache = null;
    private Set<String> visitedCategories = new HashSet<>();
    private Set<Integer> visitedIds = new HashSet<>();
    private ArticleCache articleCache = null;
    private ConceptCache conceptCache = null;


    public TitleExporter(Conf conf) throws MissedException {
        this.conf = conf;
        this.categoryCache = new CategoryCacheRedisImpl(conf);
        this.treeCache = new CategoryTreeGraphRedisImpl(conf);
        this.conceptCache = new ConceptCacheRedisImpl(conf);
    }

    public void exportCategoryTree(File dir, String rootName) throws MissedException, IOException {
        //CategoryTreeGraph treeGraph = new CategoryTreeGraphRedisImpl(conf);
        this.visitedCategories.clear();
        int rootId = categoryCache.getIdByName(rootName, 0);
        //this.visitedIds.add(rootId);

        Set<Integer> parentIds = categoryCache.getChildIds(rootId);
        for (int parentId : parentIds) {
            String name = categoryCache.getNameById(parentId);
            //this.visitedIds.add(parentId);

            File f = new File(dir, name + ".txt");

            PrintWriter writer = new PrintWriter(Files.newWriter(f, Charsets.UTF_8));
            exportCategoriesBFS(writer, parentId, rootName + "=>" + name);
            writer.close();
        }
    }

    /**
     * 按照广度优先遍历方式输出一个节点下的所有路径
     *
     * @param writer
     * @throws MissedException
     */
    private void exportCategoriesBFS(PrintWriter writer, int rootId, String rootPrefix) throws MissedException {
        Set<Integer> visited = new HashSet<>();
        visited.add(rootId);
        Queue<Pair<Integer, String>> queue = new LinkedList<>();
        queue.add(Pair.of(rootId, rootPrefix));

        while (!queue.isEmpty()) {
            Pair<Integer, String> node = queue.poll();
            int parentId = node.getLeft();
            String prefix = node.getRight();
            Set<Integer> childIds = categoryCache.getChildIds(parentId);

            boolean allVisited = true;
            if (childIds != null && childIds.size() > 0) {
                for (int id : childIds) {
                    if(visited.contains(id))
                        continue;
                    else
                        visited.add(id);

                    allVisited = false;

                    String name = categoryCache.getNameById(id);
                    if (name.equalsIgnoreCase("Years") || name.equalsIgnoreCase("Decades")) continue;

                    queue.add(Pair.of(id, prefix  + "=>" + name));
                }
            }

            if(allVisited) {
                writer.println(prefix);

                //output articles
                Set<Integer> articleIds = categoryCache.getArticleIds(parentId);
                for (int aid : articleIds) {
                    String articleName = conceptCache.getNameById(aid);
                    if (articleName != null) {
                        writer.println(prefix + "=>[" + articleName + "]");
                    }
                }

            }
        }


    }

    /**
     * 递归导出某个类别下的所有子类，按照距离父类的距离用Tab划分层级
     *
     * @param writer
     * @param parentId
     * @throws MissedException
     */
    private void exportCategoriesDFS(PrintWriter writer, int parentId, String prefix) throws MissedException {
        Set<Integer> childIds = categoryCache.getChildIds(parentId);

        if (childIds != null && childIds.size() > 0) {
            for (int id : childIds) {
                if(visitedIds.contains(id))
                    continue;
                else
                    visitedIds.add(id);

                String name = categoryCache.getNameById(id);
                if (name.equalsIgnoreCase("Years") || name.equalsIgnoreCase("Decades")) continue;


                exportCategoriesDFS(writer, id, (prefix + "=>" + name));
            }
        } else {
            writer.println(prefix);

            //output articles
            Set<Integer> articleIds = categoryCache.getArticleIds(parentId);
            for (int aid : articleIds) {
                String articleName = articleCache.getNameById(aid, null);
                if (articleName != null) {
                    writer.println(prefix + "=>[" + articleName + "]");
                }
            }

        }
    }

    public void exportCategoryTreeOld(File f, String name) throws MissedException, IOException {
        //CategoryTreeGraph treeGraph = new CategoryTreeGraphRedisImpl(conf);
        this.visitedCategories.clear();
        int parentId = categoryCache.getIdByName(name, 0);
        this.visitedIds.add(parentId);

        PrintWriter writer = new PrintWriter(Files.newWriter(f, Charsets.UTF_8));
        exportCategoryTree(writer, parentId, 0);
        writer.close();
    }

    /**
     * 递归导出某个类别下的所有子类，按照距离父类的距离用Tab划分层级
     *
     * @param writer
     * @param parentId
     * @param indentCount
     * @throws MissedException
     */
    private void exportCategoryTree(PrintWriter writer, int parentId, int indentCount) throws MissedException {
        if (indentCount <= 2) {
            Set<Integer> childIds = categoryCache.getChildIds(parentId);

            if (childIds != null || childIds.size() > 0) {
                for (int id : childIds) {
                    String name = categoryCache.getNameById(id);
                    if (name.equalsIgnoreCase("Years") || name.equalsIgnoreCase("Decades")) continue;

                    for (int i = 0; i < indentCount; i++) {
                        writer.print("\t");
                    }
                    writer.println(name);

                    exportCategoryTree(writer, id, (indentCount + 1));
                }
            }
        } else {
            Set<Integer> childIds = treeCache.getChildIds(parentId);

            if (childIds != null || childIds.size() > 0) {
                for (int id : childIds) {
                    String name = treeCache.getNameById(id);
                    if (name.equalsIgnoreCase("Years") || name.equalsIgnoreCase("Decades")
                            || name.startsWith("Timelines")
                            || name.contains(" lists")) continue;

                    for (int i = 0; i < indentCount; i++) {
                        writer.print("\t");
                    }
                    writer.println(name);

                    exportCategoryTree(writer, id, (indentCount + 1));
                }
            }
        }
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
        if (name == null)
            return;

        Set<Integer> childIds = treeGraph.getChildIds(startCategoryId);

        if (childIds == null || childIds.size() == 0) {
            if (treeGraph.getConceptCount(startCategoryId) > 5) {
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
        options.addOption(new Option("title", true, "category title."));
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
        if (commandLine.hasOption("ec") && commandLine.hasOption("cf") && commandLine.hasOption("title")) {
            System.out.println("save categories to file:" + commandLine.getOptionValue("cf"));
            File cf = new File(commandLine.getOptionValue("cf"));
            String title = commandLine.getOptionValue("title");
            exporter.exportCategoryTree(cf, title);
            System.out.println("I'm DONE!");
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
