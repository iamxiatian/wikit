package ruc.irm.wikit.cache;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import ruc.irm.wikit.cache.impl.ArticleCacheRedisImpl;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.common.exception.MissedException;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 * ArticleCache store all original wikipedia page id and title mappings.
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
     * if not exist, return 0
     * @param name
     * @return
     */
    int getIdByNameOrAlias(String name);

    void saveCategories(int pageId, Set<String> categories) throws MissedException;

    Set<Integer> getCategories(int pageId) throws MissedException;

    public static void main(String[] args) throws ParseException, IOException {
        String helpMsg = "usage: ArticleCache -c config.xml";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("c", true, "config file"));
        options.addOption(new Option("clear", false, "clear article cache"));
        options.addOption(new Option("test", false, "loop test on terminal"));

        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("c")) {
            helpFormatter.printHelp(helpMsg, options);
            return;
        }

        Conf conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);
        ArticleCache cache = new ArticleCacheRedisImpl(conf);

        if(commandLine.hasOption("clear")) {
            cache.clearAll();
        } else if (commandLine.hasOption("test")) {
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


        System.out.println("I'm DONE!");
    }
}
