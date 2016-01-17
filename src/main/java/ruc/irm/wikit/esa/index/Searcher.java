package ruc.irm.wikit.esa.index;

import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import org.apache.commons.cli.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.neo4j.index.impl.lucene.Hits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.common.exception.WikitException;
import ruc.irm.wikit.util.ConsoleLoop;
import ruc.irm.wikit.util.HeapSort;
import ruc.irm.wikit.util.text.analysis.ESAAnalyzer;

import java.io.*;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Search content from indexed articles. Usage:
 * ./run.py Searcher -c xxx-conf.xml
 *
 * @author Cagatay Calli <ccalli@gmail.com>
 */
public class Searcher implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(Searcher.class);

    private IndexReader reader = null;
    private IndexSearcher searcher = null;
    private Analyzer analyzer = null;

    private Conf conf = null;

    public Searcher(Conf conf) throws IOException {
        this.conf = conf;
        this.analyzer =   new ESAAnalyzer(conf);
        this.open();
    }


    private void open() throws IOException {
        this.reader = DirectoryReader.open(FSDirectory.open(new File(conf.getEsaIndexDir())));
        this.searcher = new IndexSearcher(reader);
    }


    public void close() throws IOException {
        if (reader != null) {
            reader.close();
            System.out.println("Indexer was closed successfully.");
        }
    }

    public void showSearchResult(String q) {
        try   {
            BooleanQuery booleanQuery = new BooleanQuery();

            QueryParser parser = new QueryParser(Conf.LUCENE_VERSION, "title",
                    analyzer);
            Query titleQuery = parser.parse(q);

            booleanQuery.add(titleQuery, BooleanClause.Occur.SHOULD);

            parser = new QueryParser(Conf.LUCENE_VERSION, "contents", analyzer);
            Query contentQuery = parser.parse(q);
            booleanQuery.add(contentQuery, BooleanClause.Occur.SHOULD);

            TopDocs topDocs = searcher.search(booleanQuery, 100);
            for (int i = 0; i < topDocs.totalHits && i < 10; i++) {
                ScoreDoc scoreDoc = topDocs.scoreDocs[i];
                Document doc = searcher.doc(scoreDoc.doc);
                System.out.println(i + ":\t" + doc.get("id") + "\t" + doc.get("title"));
                System.out.println("-----------------");
                String content = doc.get("contents");
                if (content.length() > 250) {
                    content = content.substring(0, 200);
                }
                System.out.println(content);
                System.out.println("\n");
            }

            System.out.println("total hits:" + topDocs.totalHits);
        }  catch  (ParseException e)  {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
            throws org.apache.commons.cli.ParseException,
            WikitException,
            IOException {
        String helpMsg = "usage: Searcher -c config.xml";

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
        //Searcher searcher = new Searcher(conf);

        try(Searcher searcher = new Searcher(conf)){

            ConsoleLoop.loop(new ConsoleLoop.Handler() {
                @Override
                public void handle(String input) throws IOException {
                    searcher.showSearchResult(input);
                }
            });
        }
    }

}
