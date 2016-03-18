package ruc.irm.wikit.cache;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.common.exception.MissedException;
import ruc.irm.wikit.cache.impl.ArticleCacheRedisImpl;
import ruc.irm.wikit.cache.impl.CategoryCacheRedisImpl;
import ruc.irm.wikit.data.dump.impl.PageSequenceDump;
import ruc.irm.wikit.data.dump.parse.WikiPage;
import ruc.irm.wikit.data.dump.parse.WikiPageFilter;
import ruc.irm.wikit.model.Category;

import java.io.*;
import java.text.NumberFormat;
import java.util.*;

/**
 *
 * Maintain the category relationships, include:
 * <ul>
 *     <li>category ==> wiki page id</li>
 *     <li>category ==> parent categories</li>
 *     <li>category ==> child categories</li>
 *     <li>category ==> articles which belongs to this category</li>
 *     <li>category ==> article count which belongs to this category</li>
 * </ul>
 *
 * <p>
 *     To build the above relationships, we should call the following methods
 *     by traverse the dump wiki page file:
 *     <ul>
 *         <li>addCategory(category, pageId)</li>
 *         <li>saveParents()</li>
 *         <li>saveChildren()</li>
 *
 *         <li>addArticleRelation()</li>
 *         <li>incArticleCount()</li>
 *     </ul>
 * </p>
 *
 * @author Tian Xia <a href="mailto:xiat@ruc.edu.cn">xiat@ruc.edu.cn</a>
 * @date Apr 14, 2014 12:41 PM
 */
public interface CategoryCache extends NameIdMapping, Cache {
    /**
     * get all category names of wikipedia
     */
    Set<String> listNames();

    void saveParents(int catId, String... parents);

    void saveParents(int catId, Collection<String> parents);

    void saveChildren(int catId, String... childNames);

    void saveChildren(int catId, int... childIds);

    /**
     * Put specified articleId into category
     * @param catId
     * @param articleId
     */
    void addArticleRelation(int catId, int articleId);

    /**
     * Get all article ids under specified category
     *
     * @param catId
     * @return
     */
    Set<Integer> getArticleIds(int catId);

    void incArticleCount(int catId);

    /**
     * Increase article count of specified category
     * @param count
     */
    void incArticleCount(int catId, int count);


    Set<Integer> getParentIds(int catId);

    Set<String> getParentNames(String catName);

    Set<Integer> getChildIds(int catId);

    /**
     * Get all children by given catName
     * @param catName
     * @return
     */
    Set<String> getChildNames(String catName);


    int getArticleCount(int catId);

    /**
     * get category's depth to root category, if category does not exist,
     * return defaultDepth
     */
    public int getDepth(int catId, int defaultDepth);

    /**
     * 根据分类的名称获取分类对象
     */
    Category getCategory(String catName) throws MissedException;

    /**
     * 根据分类的页面Id获取分类对象
     */
    Category getCategory(int catId) throws MissedException;

    /**
     * 为类别赋予depth深度属性
     */
    void assignDepth();

    /**
     * 从给定的类别开始，通过深度优先遍历，寻找是否存在环，如果存在，则输出
     *
     */
    void findCycles(String startCatName) throws MissedException;

    /**
     * Output the category info to text file, each line is:
     *
     * Parent name|Child name
     *
     * @param file
     */
    default void exportSimpleInfoToTxtFile(File file) {
        if (!hasDone()) {
            System.out.println("Category cache is not constructed yet.");
        }
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            int count = 0;
            for (int id : listIds()) {
                String name = getNameById(id);
                for(int childId: getChildIds(id)) {
                    String child = getNameById(childId);
                    writer.println(name + "|" + child);
                }

                if (++count % 1000 == 0) {
                    System.out.print(count);
                    System.out.print(" . ");
                }
            }
            System.out.println(count);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MissedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Output all the category name and corresponded article count
     * @param file
     */
    default void exportFullInfoToTxtFile(File file) {
        if (!hasDone()) {
            System.out.println("Category cache is not constructed yet.");
        }
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("id \t name \t parents \t children \t articles \t " +
                    "depth");
            int count = 0;
            for (int id : listIds()) {
                String name = getNameById(id);
                writer.print(id + "\t");
                writer.print(name.replaceAll("\t", "__").replaceAll(" ", "_"));
                writer.print("\t");
                writer.print(getParentIds(id).size());
                writer.print("\t");
                writer.print(getChildIds(id).size());
                writer.print("\t");
                writer.print(getArticleCount(id));
                writer.print("\t");
                writer.println(getDepth(id, -1));

                if (++count % 1000 == 0) {
                    System.out.print(count);
                    System.out.print(" . ");
                }
            }
            System.out.println(count);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MissedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws ParseException, IOException, MissedException {
        String helpMsg = "usage: CategoryCache -c config.xml";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("c", true, "config file"));
        options.addOption(new Option("build", false, "build category cache"));
        options.addOption(new Option("test", false, "loop test on terminal"));
        options.addOption(new Option("dd", false, "output depth distribution"));
        options.addOption(new Option("export", true, "export category to file"));
        options.addOption(new Option("cycle", true, "Find cycle in category"));
        options.addOption(new Option("isolate", false, "Find isolated node"));
        options.addOption(new Option("stat", false, "show statistics info"));

        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("c")) {
            helpFormatter.printHelp(helpMsg, options);
            return;
        }

        Conf conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);
        CategoryCache cache = new CategoryCacheRedisImpl(conf);
        ArticleCache articleCache = new ArticleCacheRedisImpl(conf);

        if (commandLine.hasOption("build")) {

            PageSequenceDump dump = new PageSequenceDump(conf,
                    conf.get("wiki.dump.seq.file.category"));

            if(!cache.nameIdMapped()) {
                //make name-id mapping
                dump.traverse(new WikiPageFilter() {
                    @Override
                    public void process(WikiPage wikiPage, int index) {
                        if (wikiPage.isCategory()) {
                            cache.saveNameIdMapping(wikiPage.getCategoryTitle(),
                                    wikiPage.getId());
                        }
                    }

                    @Override
                    public void close() {
                        cache.finishNameIdMapping();
                    }
                });
            }

            if(!cache.hasDone()) {
                System.out.println("Add category link edge...");
                //make parent-child relations
                dump.traverse(new WikiPageFilter() {
                    @Override
                    public void process(WikiPage wikiPage, int index) {
                        if (!wikiPage.isCommonCategory()) return;

                        String title = wikiPage.getCategoryTitle();
                        //DO NOT use wikiPage.getId() because only one id is
                        // kept in redis for the same title categories.
                        int catId = cache.getIdByName(title, -1);
                        if (catId < 0) return;

                        Collection<String> parents = wikiPage.getCategories();
                        for (String p : parents) {
                            int pid = cache.getIdByName(p, -1);
                            if (pid > 0) {
                                cache.saveChildren(pid, catId);
                            }
                        }
                        cache.saveParents(catId, parents);
                    }

                    @Override
                    public void close() {
                        cache.done();
                    }
                });

                //后续处理
                System.out.println("Add category depth...");
                cache.assignDepth();

                System.out.println("Add category article link...");
                //增加类别到文章的链接关系
                dump = new PageSequenceDump(conf,
                        conf.get("wiki.dump.seq.file.article"));
                dump.traverse(new WikiPageFilter() {
                    @Override
                    public void process(WikiPage wikiPage, int index) {
                        int articleId  =wikiPage.getId();
                        for (String c : wikiPage.getCategories()) {
                            int cid = cache.getIdByName(c, 0);
                            if(cid>0) {
                                cache.addArticleRelation(cid, articleId);
                            }
                        }
                    }
                });
            }
            System.out.println("Build complete!");
        } else if (commandLine.hasOption("cycle")) {
            String name = commandLine.getOptionValue("cycle");
            cache.findCycles(name);
        } else if(commandLine.hasOption("isolate")) {
            for (int id : cache.listIds()) {
                if (cache.getParentIds(id).size() == 0) {
                    System.out.println(cache.getNameById(id, "Null"));
                }
            }
        } else if (commandLine.hasOption("stat")) {
            Set<Integer> ids = cache.listIds();
            System.out.println("node count:\t" + ids.size());
            int edgeCount = 0;
            for (int id : ids) {
                edgeCount += cache.getChildIds(id).size();
            }
            System.out.println("edge count:\t" + edgeCount);
        }

        if (commandLine.hasOption("export")) {
            String f = commandLine.getOptionValue("export");
            cache.exportSimpleInfoToTxtFile(new File(f));
            System.out.println("I'm DONE for export.");
        }

        if (commandLine.hasOption("dd")) {
            TreeMap<Integer, Integer> map = Maps.newTreeMap();
            int total = 0;
            for (int id : cache.listIds()) {
                int depth = cache.getDepth(id, -1);
                if(depth<0) continue;
                map.put(depth, map.getOrDefault(depth, 0) + 1);
                total++;
            }

            NumberFormat format = NumberFormat.getInstance();
            format.setMinimumFractionDigits( 3 );
            format.setMaximumFractionDigits(3);
            System.out.println("Depth Distribution, total category: " + total);
            for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
                System.out.println(entry.getKey() + "\t\t" + entry.getValue()
                        + "\t\t" + format.format(entry.getValue()*100.0/total));
            }
        }

        if (commandLine.hasOption("test")) {
            Scanner scanner = new Scanner(System.in);
            String input = null;
            Category category = null;

            System.out.println("============================");
            System.out.println("  Name <-> Id mapping:" + cache.nameIdMapped());
            System.out.println("  Cache finished:" + cache.hasDone());
            System.out.println("============================");

            System.out.print("Input category id or name>");
            while ((input = scanner.nextLine()) != null) {
                if (input.equalsIgnoreCase("exit")) {
                    break;
                }

                try {
                    if (StringUtils.isNumeric(input)) {
                        category = cache.getCategory(NumberUtils.toInt(input));
                    } else {
                        category = cache.getCategory(input);
                    }
                    if (category != null) {
                        System.out.println("\t id:\t\t" + category.getPageId());
                        System.out.println("\t title:\t\t" + category.getTitle());
                        System.out.println("\t depth:\t\t" + cache.getDepth(category.getPageId(), -1));
                        System.out.println("\t articleCount:\t\t" + category.getArticleCount());

                        System.out.println("\t ---------------");
                        Set<Integer> parentIds = category.getParentIds();
                        System.out.println("\t parent: ");
                        int index = 1;
                        for (int id : parentIds) {
                            System.out.print("\t\t" + (index++) + "\t==>\t" +id + ":");
                            System.out.println(cache.getNameById(id) +
                                    "(" + cache.getDepth(id, -1) + ")");
                        }

                        System.out.println("\t ---------------");
                        Set<Integer> childrenIds = cache.getChildIds(category
                                .getPageId());
                        System.out.println("\t children: ");
                        index = 1;
                        for (int childId : childrenIds) {
                            System.out.print("\t\t" + (index++) + "\t==>\t" + childId + ":");
                            System.out.println(cache.getNameById(childId) +
                                    "(" + cache.getDepth(childId, -1) + ")");
                        }

                        System.out.println("\t ---------------");
                        Set<Integer> articleIds = category.getArticleIds();
                        System.out.println("\t articles: ");
                        index = 1;
                        for (int id : articleIds) {
                            System.out.print("\t\t" + (index++) + "\t==>\t" +id + ":");
                            System.out.println(articleCache.getNameById(id));
                        }
                    }
                } catch (MissedException e) {
                    System.out.println(e);
                }

                System.out.println("=========================\n\n");
                System.out.print("Input category id or name>");
            }
        }


        System.out.println("Bye!");
    }
}
