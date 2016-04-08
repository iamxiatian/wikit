package ruc.irm.wikit.mining.keyword;

import com.hankcs.hanlp.HanLP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.nlp.segment.SegmentFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Automatic keyword extractor interface.
 *
 * User: xiatian
 * Date: 3/31/13 6:15 PM
 */
public class KeywordExtractor {
    private Logger LOG = LoggerFactory.getLogger(KeywordExtractor.class);

    private float alpha = 0.1f;
    private float beta = 0.9f;
    private float gamma = 0.0f;
    private float lambda = 30.0f;
    private int maxReadWordCount = 2000;

    private Conf conf = null;

    public KeywordExtractor(Conf conf) {
        this.conf = conf;

        this.alpha = conf.getFloat("nlp.keyword.alpha", 0.1f);
        this.beta = conf.getFloat("nlp.keyword.beta", 0.9f);
        this.gamma = conf.getFloat("nlp.keyword.gamma", 0.0f);
        this.lambda = conf.getFloat("nlp.keyword.lambda", 30.0f);
        this.maxReadWordCount = conf.getInt("nlp.keyword.valid.word.count", 2000);
    }

    /**
     * 设置人工指定的权重
     *
     * @param word
     * @param weight
     */
    public static void setSpecifiedWordWeight(Conf conf, String word, String pos, float weight) {
        SpecifiedWeight.setWordWeight(word, weight);

        //同时插入分词程序
        SegmentFactory.getSegment(conf).insertUserDefinedWord(word, pos, 10);
    }

    public List<String> extractAsList(String title, String content, int topN) {
        List<String> keywords = new ArrayList<String>();

        //use improved text rank method proposed by xiatian
        RankBuilder builder = new RankBuilder(conf, alpha, beta, gamma, true);
        builder.build(title, lambda);
        builder.build(content, 1.0f);

        PageRankGraph g = builder.makePageRankGraph();
        g.iterateCalculation(20, 0.15f);
        g.quickSort();
        for (int i = 0; i < g.labels.length && i < topN; i++) {
            keywords.add(g.labels[i]);
        }

        return keywords;
    }

    public String extractAsString(String title, String content, int topN) {
        StringBuilder sb = new StringBuilder();
        List<String> keywords = extractAsList(title, content, topN);
        for (String keyword : keywords) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(keyword);
        }
        return sb.toString();
    }

}
