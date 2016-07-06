package ruc.irm.wikit.nlp.word2vec;

import org.deeplearning4j.text.tokenization.tokenizer.DefaultStreamTokenizer;
import org.deeplearning4j.text.tokenization.tokenizer.DefaultTokenizer;
import org.deeplearning4j.text.tokenization.tokenizer.TokenPreProcess;
import org.deeplearning4j.text.tokenization.tokenizer.Tokenizer;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;

import java.io.InputStream;

/**
 * @author Tian Xia
 * @date Jul 06, 2016 18:07
 */
public class ChineseTokenizerFactory implements TokenizerFactory {
    private TokenPreProcess tokenPreProcess;

    public ChineseTokenizerFactory() {
    }

    public Tokenizer create(String toTokenize) {
        ChineseTokenizer t = new ChineseTokenizer(toTokenize);
        t.setTokenPreProcessor(this.tokenPreProcess);
        return t;
    }

    public Tokenizer create(InputStream toTokenize) {
        DefaultStreamTokenizer t = new DefaultStreamTokenizer(toTokenize);
        t.setTokenPreProcessor(this.tokenPreProcess);
        return t;
    }

    public void setTokenPreProcessor(TokenPreProcess preProcessor) {
        this.tokenPreProcess = preProcessor;
    }

    public TokenPreProcess getTokenPreProcessor() {
        return this.tokenPreProcess;
    }
}
