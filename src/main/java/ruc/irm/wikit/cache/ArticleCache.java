package ruc.irm.wikit.cache;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.common.exception.MissedException;
import ruc.irm.wikit.cache.impl.ArticleCacheRedisImpl;

import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 * Wiki cache. When build cache, we need to add title-pageId mapping first,
 * otherwise, when save link relations, we can not get title's ID.
 *
 * @Version 1.1 use id(number) to replace string title, so the memory usage
 * can be reduced.
 *
 * User: xiatian
 * Date: 4/20/14
 * Time: 11:56 PM
 */
public interface ArticleCache extends NameIdMapping, Cache {

    List<Integer> getAllIdsByName(String name);

    /**
     * Save redirect info:<br/>
     * For example:
     *  100, "习大大", redirect to 101, "习近平", then we have:
     *  <ul>
     *      <li>习大大: {id:100, rto: {101,...}}</li>
     *      <li>习近平: {id:1001, rf:{100,...}}</li>
     *  </ul>
     */
    void saveAlias(int pageId, Iterable<String> aliasNames)
            throws MissedException;

    void saveAlias(int pageId, String aliasName)
            throws MissedException;

    boolean hasAlias(int pageId);

    Collection<String> getAliasNames(int pageId);

    /**
     * Get target page id by alias name, if the alias does not exist, return 0.
     * @param name
     * @return
     */
    int getIdByAliasName(String name);

    void saveLinkRelations(int pageId, Collection<String> outlinkAnchors);

    long getInlinkCount(int pageId);

    long getInlinkCount(String name);

    long getOutlinkCount(int pageId);

    long getOutlinkCount(String name);

    Set<Integer> getInlinks(int pageId);

    Set<Integer> getOutlinks(int pageId);


    void saveCategories(int pageId, Set<String> categories) throws MissedException;

    Set<Integer> getCategories(int pageId) throws MissedException;

    /**
     * 构建完缓存之后的后处理步骤, 处理以下内容：
     * <ul>
     * <li>建立文章别名信息到id的映射，可以根据名称获取id</li>
     * </ul>
     */
    void postProcess();


    public static void main(String[] args) throws ParseException {
        String helpMsg = "usage: ArticleCache -c config.xml";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("c", true, "config file"));
        options.addOption(new Option("test", false, "loop test on terminal"));

        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("c")) {
            helpFormatter.printHelp(helpMsg, options);
            return;
        }

        Conf conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);
        ArticleCache cache = new ArticleCacheRedisImpl(conf);

        if (commandLine.hasOption("test")) {
            Scanner scanner = new Scanner(System.in);
            String input = null;

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
                        System.out.println("Name ==> " + cache.getNameById
                                (Integer.parseInt(input)));
                    } else {
                        System.out.println("Id ==> " + cache.getIdByName
                                (input));
                    }
                } catch (MissedException e) {
                    System.out.println(e);
                }

                System.out.println("___________________\n\n");
                System.out.print("Input article id or name>");
            }

            System.out.println("Bye!");
        }
    }
}
