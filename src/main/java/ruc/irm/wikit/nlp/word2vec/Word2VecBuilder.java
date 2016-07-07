package ruc.irm.wikit.nlp.word2vec;

import org.apache.commons.cli.*;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.tokenization.tokenizer.TokenPreProcess;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.EndingPreProcessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.data.dump.WikiPageDump;
import ruc.irm.wikit.data.dump.impl.PageSequenceDump;
import ruc.irm.wikit.data.dump.impl.PageXmlDump;

import java.io.IOException;

/**
 * @author Tian Xia
 * @date Jun 30, 2016 16:24
 */
public class Word2VecBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(Word2VecBuilder.class);

    public static void build(WikiPageDump dump) throws IOException {
        dump.open();

        WikiSentenceIterator sentenceIterator = new WikiSentenceIterator(dump);
        ChineseTokenizerFactory tokenizer = new ChineseTokenizerFactory();

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
                .iterate(sentenceIterator) //
                .tokenizerFactory(tokenizer)
                .build();
        vec.fit();

        LOG.info("Save vectors....");
        WordVectorSerializer.writeWordVectors(vec, "/home/xiatian/data/words.txt");
        dump.close();
    }

    public static void main(String[] args) throws ParseException, IOException {
        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("c", true, "config file"));
        options.addOption(new Option("f", true, "wiki sequence dump file"));

        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("c") || !commandLine.hasOption("f")) {
            helpFormatter.printHelp("Options", options);
            return;
        }

        Conf conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);
        String dumpFile = commandLine.getOptionValue("f");
        PageSequenceDump dump = new PageSequenceDump(conf, dumpFile);
        Word2VecBuilder.build(dump);
    }
}
