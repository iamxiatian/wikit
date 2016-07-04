package ruc.irm.wikit.nlp.word2vec;

import org.deeplearning4j.models.word2vec.Word2Vec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tian Xia
 * @date Jun 30, 2016 16:24
 */
public class Word2VecBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(Word2VecBuilder.class);
    public void build() {
        int batchSize = 1000;
        int iterations = 3;
        int layerSize = 150;

        LOG.info("Build model....");
        Word2Vec vec = new Word2Vec.Builder()
                .batchSize(batchSize) //# words per minibatch.
                .minWordFrequency(5) //
                .useAdaGrad(false) //
                .layerSize(layerSize) // word feature vector size
                .iterations(iterations) // # iterations to train
                .learningRate(0.025) //
                .minLearningRate(1e-3) // learning rate decays wrt # words. floor learning
                .negativeSample(10) // sample size 10 words
                .iterate(iter) //
                .tokenizerFactory(tokenizer)
                .build();
        vec.fit();
    }
}
