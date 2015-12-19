package ruc.irm.wikit.data.dump;

import org.apache.commons.cli.*;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.zip.GZIPInputStream;

/**
 * Parse wiki langlinks dump file, such as zhwiki-20140315-langlinks.sql
 * the field info:
 * <ul>
 *  <li>ll_from: the page id of current language.</li>
 *  <li>ll_lang: the link-to target language</li>
 *  <li>ll_title: the target language title</li>
 * </ul>
 *
 * Be sure to create database specified in conf file first, like:
 * CREATE SCHEMA `wiki` DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ;
 *
 * <p>
 * select count(*) from article a, langlinks b
 * where a.wiki_id=b.ll_from and a.redirected=0 and b.ll_title<>'';
 * </p>
 * <p>
 * select a.wiki_id as id, a.title as chinese, b.ll_title as english
 * from article a, langlinks b where a.wiki_id=b.ll_from limit 50000,100;
 * </p>
 * User: xiatian
 * Date: 3/23/14
 * Time: 5:30 PM
 */
public class LangLinkSqlDump {
    private Conf conf = null;

    public LangLinkSqlDump(Conf conf) {
        this.conf = conf;
    }

    private Connection getConnection(Conf conf) throws ClassNotFoundException,
            SQLException {
        // Load the JDBC driver
        String driverName = "com.mysql.jdbc.Driver"; // MySQL Connector
        Class.forName(driverName);

        String serverName = conf.get("mysql.host", "localhost");
        String mydatabase = conf.get("mysql.dbname", "wiki");
        String username = conf.get("mysql.username", "root");
        String password = conf.get("mysql.password", "");
        int port = conf.getInt("mysql.port", 3306);

        // Create a connection to the database
        String jdbcURL = "jdbc:mysql://" + serverName + ":" + port + "/" + mydatabase + "?useUnicode=yes&characterEncoding=UTF-8";
        Connection connection = DriverManager.getConnection(jdbcURL, username, password);

        return connection;
    }

    public void dumpToMysql(File langlinkSqlFile)
            throws SQLException, ClassNotFoundException, IOException {
        Connection connection = getConnection(conf);
        connection.createStatement().execute("DROP table if exists langlinks");
        connection.createStatement().execute("CREATE TABLE `"
                 + "langlinks` (\n" +
                "  `ll_from` int(8) unsigned NOT NULL DEFAULT '0',\n" +
                //"  `ll_lang` varbinary(20) NOT NULL DEFAULT '',\n" +
                "  `ll_lang` varchar(20) NOT NULL DEFAULT '',\n" +
                //"  `ll_title` varbinary(255) NOT NULL DEFAULT '',\n" +
                "  `ll_title` varchar(255) NOT NULL DEFAULT '',\n" +
                "  UNIQUE KEY `ll_from` (`ll_from`,`ll_lang`),\n" +
                "  KEY `ll_lang` (`ll_lang`,`ll_title`)\n" +
                ") DEFAULT CHARSET=utf8");

        BufferedReader reader = null;
        if (langlinkSqlFile.getAbsolutePath().endsWith(".gz")) {
            reader = new BufferedReader(new InputStreamReader(
                    new GZIPInputStream(new FileInputStream(langlinkSqlFile))));
        } else {
            reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(langlinkSqlFile)));
        }
        String line = null;
        int count = 0;
        while ((line = reader.readLine()) != null) {
            if (line.contains("VALUES") && line.contains(",'en',")) {
                connection.createStatement().execute(line);
                count++;
                System.out.printf("parse %d lines.\n", count);
            }
        }

        //remove non-en lang links
        connection.createStatement().execute("delete from langlinks"
                + " where ll_lang<>'en'");
        connection.close();
        reader.close();
    }

    public static void main(String[] args)
            throws SQLException, ClassNotFoundException, IOException, ParseException {
        System.out.println("Dump language mapping links to Mysql.");
        System.out.println("Example: LnagLinkSqlDump -f " +
                "zhwiki-20140315-langlinks.sql");
        String helpMsg = "usage: LangLinkSqlDump -c config.xml -f langlink.sql";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("c", true, "config file"));
        options.addOption(new Option("f", true, "lang link dump sql file " +
                "downloaded from http://dumps.wikimedia.org/zhwiki/"));

        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("c")) {
            helpFormatter.printHelp(helpMsg, options);
            return;
        }

        Conf conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);

        LangLinkSqlDump dump = new LangLinkSqlDump(conf);
        if(commandLine.hasOption("f")) {
            File sqlFile = new File(commandLine.getOptionValue("f"));
            if (!sqlFile.exists()) {
                System.out.println("sql input file does not exist:"
                        + commandLine.getOptionValue("f"));
                return;
            }
            dump.dumpToMysql(sqlFile);
        }

        System.out.println("I'm DONE!");
    }
}
