package ruc.irm.wikit.nlp.word2vec;

import com.hankcs.hanlp.seg.common.wrapper.SegmentWrapper;
import org.deeplearning4j.text.tokenization.tokenizer.TokenPreProcess;
import org.deeplearning4j.text.tokenization.tokenizer.Tokenizer;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import ruc.irm.wikit.nlp.segment.Segment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Tian Xia
 * @date Jul 06, 2016 17:58
 */
public class ChineseTokenizer  implements Tokenizer {
    private Segment segment;
    private Iterator<String> tokens;
    private int tokenCount = 0;
    private TokenPreProcess tokenPreProcess;

    public ChineseTokenizer(String tokenText) {
        List<String> list = segment.segment(tokenText);
        this.tokenCount = list.size();
        this.tokens = list.iterator();
    }

    public boolean hasMoreTokens() {
        return this.tokens.hasNext();
    }

    public int countTokens() {
        return tokenCount;
    }

    public String nextToken() {
        String base = this.tokens.next();
        if(this.tokenPreProcess != null) {
            base = this.tokenPreProcess.preProcess(base);
        }

        return base;
    }

    public List<String> getTokens() {
        ArrayList tokens = new ArrayList();

        while(this.hasMoreTokens()) {
            tokens.add(this.nextToken());
        }

        return tokens;
    }

    public void setTokenPreProcessor(TokenPreProcess tokenPreProcessor) {
        this.tokenPreProcess = tokenPreProcessor;
    }
}