package ruc.irm.wikit.espm.graph;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import ruc.irm.wikit.cache.Cache;
import ruc.irm.wikit.cache.CategoryCache;
import ruc.irm.wikit.cache.NameIdMapping;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.common.exception.MissedException;
import ruc.irm.wikit.common.exception.WikitException;
import ruc.irm.wikit.esa.concept.ConceptCache;
import ruc.irm.wikit.esa.concept.ConceptCacheRedisImpl;
import ruc.irm.wikit.model.Category;
import ruc.irm.wikit.util.ConsoleLoop;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 * This is the interface for Tree-like category graph. The tree like graph
 * has a unique root with depth 0, every other nodes can have several
 * child nodes and parent nodes, for any node v, all its parent nodes have
 * the same depth, and all child nodes have the same depth too.
 *
 * User: xiatian
 * Date: 7/16/14
 * Time: 7:43 PM
 */
public interface CategoryTreeGraph extends NameIdMapping, Cache {

    /**
     * get all category names of wikipedia
     */
    Set<String> listNames();
    
    /**
     * Put specified articleId into category
     * @param catId
     * @param conceptId
     */
    void saveConceptRelation(int catId, int conceptId);

    /**
     * Get all article ids under specified category
     *
     * @param catId
     * @return
     */
    Set<Integer> getConceptIds(int catId);

    /**
     * Get the count of concepts which belongs to this specified category id
     * @param catId
     * @return
     */
    int getConceptCount(int catId);

    /**
     * Get the count of concepts which belongs to this category id or its
     * descendant category.
     * @param catId
     * @return
     */
    int getRecursiveConceptCount(int catId);

    Set<Integer> getCategoryIdsByConceptId(int conceptId);

    Set<Integer> getParentIds(int catId);

    Set<String> getParentNames(String catName);

    Set<Integer> getChildIds(int catId);

    /**
     * Get all children by given catName
     */
    Set<String> getChildNames(String catName);

    /**
     * get category's depth to root category, if no depth specified, return -1
     */
    public int getDepth(int catId);

    /**
     * 根据分类的名称获取分类对象
     */
    Category getCategory(String catName) throws MissedException;

    /**
     * 根据分类的页面Id获取分类对象
     */
    Category getCategory(int catId) throws MissedException;

    /**
     * determine parent-child relation
     * @param parent
     * @param child
     * @return
     */
    public boolean isChild(String parent, String child);

    public boolean isChild(int parentId, int childId);

    /**
     * Get the first level categories, all the first level categories has the same parent, for English wiki,
     * it is "Main topic classifications", for Chinese: "页面分类", this name is configured in conf xml file
     * with property name "wiki.article.category.root"
     * @return
     */
    public Set<String> getLevelOneCategoryNames();

    public Set<Integer> getLevelOneCategoryIds();

    public Collection<Integer> getLeafCategoryIds();

    int getChildCount(String catName);

    int getChildCount(int catId);

    /**
     * 获得一个类别的所有路径信息
     */
    public List<String> getPaths(int catId);

    /**
     * 是否已经建立了类别之间的父子关系
     * @return
     */
    public boolean hasEdgeRelationCreated();

    /**
     * 是否已经建立概念到类别的关系
     * @return
     */
    public boolean hasConceptRelationCreated();

    public void buildEdgeRelation(CategoryCache categoryCache) throws WikitException;

    public void buildConceptRelation(ConceptCache conceptCache) throws WikitException;

    /**
     * 统计每个节点及其子类别所拥有的概念的总数量
     * @throws WikitException
     */
    public void buildRecursiveCountInfo() throws WikitException;

    /**
     * 完整构建类树图
     */
    public void build() throws WikitException;


    /**
     * 输出深度小于等于3的类别,并进行编号
     */
    default public void exportLevel3Categories() {
        Set<String> names1 = getLevelOneCategoryNames();
        int startL1Id = 21;
        for (String name1 : names1) {
            System.out.println((startL1Id++) + "\t" + name1);
            Set<String> names2 = getChildNames(name1);
            int startL2Id = 1;
            for (String name2 : names2) {
                System.out.println("\t" + String.format("%03d",startL2Id++) + "\t" + name2);

                Set<String> names3 = getChildNames(name2);
                int startL3Id = 1;
                for (String name3 : names3) {
                    System.out.println("\t\t" + String.format("%03d",startL3Id++) + "\t" + name3);
                }
            }
        }
    }


    /**
     * 把某个类别下的所有类别信息输出到文件中，以Technology为例，输出格式为：
     * /Technology/computer/.../
     * /Technology/mobile/...
     *
     * @param writer
     * @param prefix
     * @param startCategoryId
     * @param onlyLeaf 如果为true，则只有叶子节点会被输出
     */
    default void exportSubCategory(PrintWriter writer, final String prefix, int startCategoryId, boolean onlyLeaf) {
        if (!hasDone()) {
            System.out.println("Category cache is not constructed yet.");
        }


        String name = getNameById(startCategoryId, null);
        if(name == null)
            return;

        String fullName = prefix + "/" + name;
        Set<Integer> childIds = getChildIds(startCategoryId);

        if (childIds == null || childIds.size() == 0) {
            if(getConceptCount(startCategoryId) > 5) {
                writer.println(fullName);
            }
        } else {
            if(!onlyLeaf) {
                writer.println(fullName);
            }
            for (int id : childIds) {
                exportSubCategory(writer, fullName, id, onlyLeaf);
            }
        }
    }


    public static void main(String[] args) throws ParseException, WikitException, IOException {
        String helpMsg = "usage: CategoryTreeGraph -c config.xml";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("c", true, "config file"));
        options.addOption(new Option("build", false, "build tree graph"));
        options.addOption(new Option("path", false, "show all path for given " +
                "category node."));
        options.addOption(new Option("test", false, "loop test on terminal"));
        options.addOption(new Option("stat", false, "show statistics info"));

        options.addOption(new Option("exp", false, "Output all categories if their depth is less or equal 3."));

        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("c")) {
            helpFormatter.printHelp(helpMsg, options);
            return;
        }

        Conf conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);
        CategoryTreeGraph treeGraph = new CategoryTreeGraphRedisImpl(conf);
        ConceptCache articleCache = new ConceptCacheRedisImpl(conf);

        if (commandLine.hasOption("build")) {
            treeGraph.build();
        } else if (commandLine.hasOption("stat")) {
            Set<Integer> ids = treeGraph.listIds();
            System.out.println("node count:\t" + ids.size());
            int edgeCount = 0;
            for (int id : ids) {
                edgeCount += treeGraph.getChildCount(id);
            }
            System.out.println("edge count:\t" + edgeCount);
        } else if (commandLine.hasOption("path")) {
            ConsoleLoop.loop(new ConsoleLoop.Handler() {
                @Override
                public void handle(String input) throws IOException {
                    int catId = 0;
                    if (StringUtils.isNumeric(input)) {
                        catId = Integer.parseInt(input);
                    } else {
                        catId = treeGraph.getIdByName(input, 0);
                    }
                    List<String> paths = treeGraph.getPaths(catId);
                    for (String path : paths) {
                        System.out.println(path);
                    }
                }
            });
        } else if (commandLine.hasOption("test")) {
            ConceptCache conceptCache = new ConceptCacheRedisImpl(conf);

            Scanner scanner = new Scanner(System.in);
            String input = null;
            Category category = null;

            System.out.println("============================");
            System.out.println("  Name <-> Id mapping:" + treeGraph.nameIdMapped());
            System.out.println("  created:" + treeGraph.hasDone());
            System.out.println("============================");

            System.out.print("Input category id or name>");
            while ((input = scanner.nextLine()) != null) {
                if (input.equalsIgnoreCase("exit")) {
                    break;
                }

                try {
                    if (StringUtils.isNumeric(input)) {
                        category = treeGraph.getCategory(NumberUtils.toInt(input));
                    } else {
                        category = treeGraph.getCategory(input);
                    }
                    if (category != null) {
                        System.out.println("\t id:\t\t" + category.getPageId());
                        System.out.println("\t title:\t\t" + category.getTitle());
                        System.out.println("\t depth:\t\t" + treeGraph.getDepth(category.getPageId()));
                        System.out.println("\t articleCount:\t\t" + category.getArticleCount());

                        System.out.println("\t ---------------");
                        Set<Integer> parentIds = category.getParentIds();
                        System.out.println("\t parent: ");
                        int index = 1;
                        for (int id : parentIds) {
                            System.out.print("\t\t" + (index++) + "\t==>\t" + id + ":");
                            System.out.println(treeGraph.getNameById(id) +
                                    "(" + treeGraph.getDepth(id) + ")");
                        }

                        System.out.println("\t ---------------");
                        Set<Integer> childrenIds = treeGraph.getChildIds(category
                                .getPageId());
                        System.out.println("\t children: ");
                        index = 1;
                        for (int childId : childrenIds) {
                            System.out.print("\t\t" + (index++) + "\t==>\t" + childId + ":");
                            System.out.println(treeGraph.getNameById(childId) +
                                    "(" + treeGraph.getDepth(childId) + ")");
                        }

                        System.out.println("\t ---------------");
                        Set<Integer> articleIds = category.getArticleIds();
                        System.out.println("\t concepts: ");
                        index = 1;
                        for (int id : articleIds) {
                            System.out.print("\t\t" + (index++) + "\t==>\t" + id + ":");
                            System.out.println(conceptCache.getNameById(id));
                        }
                    }
                } catch (MissedException e) {
                    System.out.println(e);
                }

                System.out.println("=========================\n\n");
                System.out.print("Input category id or name>");
            }

            System.out.println("Bye!");
        }


        if (commandLine.hasOption("exp")) {
//            PrintWriter writer = new PrintWriter(Files.newWriter(new File("/tmp/tech.txt"), Charsets.UTF_8));
//            int startId = treeGraph.getIdByName("Technology", 0);
//            treeGraph.exportSubCategory(writer, "", startId, true);
//            writer.close();
            treeGraph.exportLevel3Categories();
        }
    }
}
