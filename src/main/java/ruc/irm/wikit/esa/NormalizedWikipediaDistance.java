package ruc.irm.wikit.esa;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.util.text.analysis.ESAAnalyzer;

import java.io.File;
import java.io.IOException;

/**
 * Normalized Wikipedia Distance just like Normalized Google Distance
 * ref1: http://en.wikipedia.org/wiki/Normalized_Google_distance
 * ref2: http://arxiv.org/pdf/cs/0412098.pdf
 */
public class NormalizedWikipediaDistance {

    private IndexSearcher searcher;
    private QueryParser qparser;
    private Query wQuery;
    private TopDocs wResults;

    int numWikiDocs;

    public class NumRes {
        public int res1;
        public int res2;
        public int resCommon;

        public NumRes() {
            res1 = res2 = resCommon = 0;
        }

        public void reset() {
            res1 = res2 = resCommon = 0;
        }
    }

    NumRes nres = new NumRes();

    public NormalizedWikipediaDistance(Conf conf) {
        try {
            IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(conf.getEsaIndexDir())));
            searcher = new IndexSearcher(reader);
            numWikiDocs = reader.maxDoc();
            qparser = new QueryParser(Version.LUCENE_47, "contents", new ESAAnalyzer(conf));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private int freqSearch(String phrase) throws IOException, ParseException {
        wQuery = qparser.parse("\"" + QueryParser.escape(phrase) + "\"");
        // wQuery = qparser.parse(QueryParser.escape(phrase));
        wResults = searcher.search(wQuery, 1);
        return wResults.totalHits;
    }

    /**
     * Search to find the probability of occurrence for two phrases
     *
     * @param phrase1 the first query string
     * @param phrase2 the second query string
     * @return
     * @throws ParseException
     * @throws IOException
     */
    private int occurSearch(String phrase1, String phrase2) throws ParseException, IOException {
        wQuery = qparser.parse("\"" + QueryParser.escape(phrase1) + "\" AND " + "\"" + QueryParser.escape(phrase2) + "\"");
        // wQuery = qparser.parse("(" + QueryParser.escape(phrase1)+") AND (" + QueryParser.escape(phrase2) + ")");
        wResults = searcher.search(wQuery, 1);
        return wResults.totalHits;
    }

    public double getDistance(String label1, String label2) {
        float f1 = 0.0f, f2 = 0.0f;
        float fCommon = 0.0f;

        nres.reset();

        try {
            nres.res1 = freqSearch(label1);
            f1 = nres.res1;
            nres.res2 = freqSearch(label2);
            f2 = nres.res2;
            nres.resCommon = occurSearch(label1, label2);
            fCommon = nres.resCommon;

        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (f1 == 0 || f2 == 0) {
            return -1f;    // undefined
            // return 10000.0f;	// no information, assume inf distance
        }

        // if((fCommon == 0) && (f1 > 0 || f2 > 0) ){
        if (fCommon == 0) {
            return 10000.0f;    // infinite distance
        }

        f1 *= 2;
        f2 *= 2;
        fCommon *= 2;    // just generalize

        double log1, log2, logCommon, maxlog, minlog;
        log1 = Math.log(f1);
        log2 = Math.log(f2);
        logCommon = Math.log(fCommon);
        maxlog = Math.max(log1, log2);
        minlog = Math.min(log1, log2);

        return (maxlog - logCommon) / (Math.log(numWikiDocs) - minlog);

    }

    public NumRes getMatches() {
        return nres;
    }

}
