package ruc.irm.wikit.util.text.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.LengthFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ruc.irm.wikit.common.conf.Conf;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


public class ESAAnalyzer extends Analyzer {
    private static final Logger LOG = LoggerFactory.getLogger(ESAAnalyzer.class);

    boolean enablePorterStemming;
    public Set<String> filterWords = new HashSet<>();

    /**
     * An unmodifiable set containing some common English words that are not usually useful
     * for searching.
     */
    public final CharArraySet ENGLISH_STOP_WORDS_SET;
    private Conf conf = null;

    public ESAAnalyzer(Conf conf) {
        this.conf = conf;
        ArrayList<String> stopWords = new ArrayList<String>(500);
        String line = null;

        try {
            // read stop words
            InputStream is = this.getClass().getResourceAsStream("/dict/stopwords.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.equals("")) {
                    stopWords.add(line.trim());
                }
            }
            br.close();
        } catch (IOException e) {
            LOG.error("Init ESAAnalyzer error.", e);
        }

        final CharArraySet stopSet = new CharArraySet(Conf.LUCENE_VERSION, stopWords.size(), false);
        stopSet.addAll(stopWords);
        ENGLISH_STOP_WORDS_SET = CharArraySet.unmodifiableSet(stopSet);
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
        if ("English".equalsIgnoreCase(conf.getEsaLanguage())) {
            Tokenizer tokenizer = new StandardTokenizer(Conf.LUCENE_VERSION, reader);

            TokenStream filter = new StandardFilter(Conf.LUCENE_VERSION, tokenizer);
            filter = new LowerCaseFilter(Conf.LUCENE_VERSION, filter);
            filter = new PorterStemFilter(filter);
            filter = new StopFilter(Conf.LUCENE_VERSION, filter, ENGLISH_STOP_WORDS_SET);
            filter = new LengthFilter(Conf.LUCENE_VERSION, filter, 2, 50);
            filter = new ESAIndexFilter(Conf.LUCENE_VERSION, filter);

            return new TokenStreamComponents(tokenizer, filter);
        } else {
            Tokenizer tokenizer = new ChineseTokenizer(com.hankcs.hanlp.tokenizer.StandardTokenizer.SEGMENT
                    .enableOffset(true), reader, filterWords, enablePorterStemming);

            TokenStream filter = new StandardFilter(Conf.LUCENE_VERSION, tokenizer);

            //Only english words are kept.
            //filter = new ESAIndexFilter(Config.LUCENE_VERSION, filter);
            filter = new LowerCaseFilter(Conf.LUCENE_VERSION, filter);
            filter = new StopFilter(Conf.LUCENE_VERSION, filter, ENGLISH_STOP_WORDS_SET);
            filter = new LengthFilter(Conf.LUCENE_VERSION, filter, 2, 50);
            filter = new ESAIndexFilter(Conf.LUCENE_VERSION, filter);

            return new TokenStreamComponents(tokenizer, filter);
        }
    }

}