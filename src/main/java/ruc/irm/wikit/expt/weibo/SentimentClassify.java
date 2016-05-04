package ruc.irm.wikit.expt.weibo;

import cc.mallet.classify.Classifier;
import cc.mallet.classify.NaiveBayesTrainer;
import cc.mallet.classify.Trial;
import cc.mallet.pipe.*;
import cc.mallet.types.*;
import com.alibaba.fastjson.JSON;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import org.apache.commons.cli.*;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.common.exception.WikitException;
import ruc.irm.wikit.esa.ESAModel;
import ruc.irm.wikit.esa.ESAModelImpl;
import ruc.irm.wikit.esa.concept.ConceptCache;
import ruc.irm.wikit.esa.concept.ConceptCacheRedisImpl;
import ruc.irm.wikit.esa.concept.vector.ConceptIterator;
import ruc.irm.wikit.esa.concept.vector.ConceptVector;
import ruc.irm.wikit.nlp.libsvm.SVMClassifierTrainer;
import ruc.irm.wikit.nlp.libsvm.kernel.LinearKernel;
import ruc.irm.wikit.util.mallet.ChineseSequence2TokenSequence;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Tian Xia
 * @date Apr 13, 2016 16:27
 */
public class SentimentClassify {
    private int Top_Concepts = 10;
    private ModelType modelType = ModelType.NB;

    private enum ModelType {
        NB, SVM
    };

    private static class DataMaker {
        private ESAModel esaModel = null;
        private ConceptCache conceptCache = null;

        private int conceptLimit = 50;

        private DataMaker(Conf conf) {
            this.esaModel = new ESAModelImpl(conf);
            this.conceptCache = new ConceptCacheRedisImpl(conf);
        }

        private List<String> getESAConcepts(String doc) {
            List<String> concepts = new LinkedList<>();
            try {
                ConceptVector cv = esaModel.getCombinedVector(doc, conceptLimit);
                if (cv == null) {
                    return concepts;
                }

                ConceptIterator it = cv.orderedIterator();
                while (it.next()) {
                    int id = it.getId();
                    String name = conceptCache.getNameById(id);
                    concepts.add(name);
                }
            } catch (WikitException e) {
                e.printStackTrace();
            }
            return concepts;
        }

        /**
         * lineDocsFile中每行表示一个短文本，读取每一行，生成对应的ESA文本，并把每行的ESA结果
         * 以文件的形式输出到outDir目录中
         *
         * @param lineDocsFile
         * @param outputFile
         */
        public void generateESA(File lineDocsFile, File outputFile) throws IOException {
            List<String> docs = Files.readLines(lineDocsFile, Charsets.UTF_8);
            int idx = 0;

            PrintWriter writer = new PrintWriter(new FileWriter(outputFile));
            for (String doc : docs) {
                List<String> concepts = getESAConcepts(doc);
                String line = JSON.toJSONString(concepts);
                writer.println(line);

                idx++;
            }
            writer.flush();
            writer.close();
        }

    }

    private static final String[] DENY_POS_ARRAY = new String[]{"nx", "w", "uj"};

    private boolean deny(String pos) {
        for (String name : DENY_POS_ARRAY) {
            if(name.equals(pos))
                return true;
        }
        return false;
    }

    /**
     * 加载原始的微博文本，通过正面和负面两个文件分别加载
     *
     * @return
     */
    public InstanceList loadRawInstances(File posDocsFile, File negDocsFile, int fold, boolean testSet) throws IOException {
        Pipe pipe = new SerialPipes(new Pipe[]{
                new Target2Label(),
                new ChineseSequence2TokenSequence(DENY_POS_ARRAY),
                new TokenSequence2FeatureSequence(),
                new FeatureSequence2FeatureVector()});

        InstanceList instances = new InstanceList(pipe);

        List<String> docs = Files.readLines(posDocsFile, Charsets.UTF_8);
        int idx = 0;
        for (String doc : docs) {
            if((idx % fold ==0 && testSet) //测试集情况
                    || (idx % fold !=0 && !testSet)) { //训练集情况
                Instance instance = new Instance(doc, "Positive", "pos-" + idx, null);
                instances.addThruPipe(instance);
            }

            idx++;
        }

        docs = Files.readLines(negDocsFile, Charsets.UTF_8);
        idx = 0;
        for (String doc : docs) {
            if((idx % fold ==0 && testSet) //测试集情况
                    || (idx % fold !=0 && !testSet)) { //训练集情况
                Instance instance = new Instance(doc, "Negative", "neg-" + idx, null);
                instances.addThruPipe(instance);
            }

            idx++;
        }

        return instances;
    }

    /**
     * 加载微博文本的ESA分析结果，通过正面和负面两个文件分别加载
     *
     * @return
     */
    public InstanceList loadEsaInstances(File posDocsFile, File negDocsFile, int fold, boolean testSet) throws IOException {
        Pipe pipe = new SerialPipes(new Pipe[]{
                new Target2Label(),
                new TokenSequence2FeatureSequence(),
                new FeatureSequence2FeatureVector()});

        InstanceList instances = new InstanceList(pipe);

        List<String> docs = Files.readLines(posDocsFile, Charsets.UTF_8);
        int idx = 0;
        for (String doc : docs) {
            if((idx % fold ==0 && testSet) //测试集情况
                    || (idx % fold !=0 && !testSet)) { //训练集情况
                List<String> concepts = JSON.parseArray(doc, String.class);
                TokenSequence ts = new TokenSequence(concepts.size());
                int count = 0;
                for (String concept : concepts) {
                    count++;
                    ts.add(new Token(concept));
                    if(count==Top_Concepts) break;
                }
                Instance instance = new Instance(ts, "Positive", "pos-" + idx, null);
                instances.addThruPipe(instance);
            }

            idx++;
        }

        docs = Files.readLines(negDocsFile, Charsets.UTF_8);
        idx = 0;
        for (String doc : docs) {
            if((idx % fold ==0 && testSet) //测试集情况
                    || (idx % fold !=0 && !testSet)) { //训练集情况
                List<String> concepts = JSON.parseArray(doc, String.class);
                TokenSequence ts = new TokenSequence(concepts.size());
                for (String concept : concepts) {
                    ts.add(new Token(concept));
                }

                Instance instance = new Instance(ts, "Negative", "neg-" + idx, null);
                instances.addThruPipe(instance);
            }

            idx++;
        }

        return instances;
    }


    /**
     * 加载微博文本的原始内容和ESA分析结果，通过正面和负面两个文件分别加载
     *
     * @return
     */
    public InstanceList loadMixInstances(File posDocsRawFile,
                                         File negDocsRawFile,
                                         int fold, boolean testSet) throws IOException {
        Pipe pipe = new SerialPipes(new Pipe[]{
                new Target2Label(),
                new TokenSequence2FeatureSequence(),
                new FeatureSequence2FeatureVector()});

        InstanceList instances = new InstanceList(pipe);

        File posDocsEsaFile = new File(posDocsRawFile.getParentFile(), "positive.esa");

        List<String> rawDocs = Files.readLines(posDocsRawFile, Charsets.UTF_8);
        List<String> esaDocs = Files.readLines(posDocsEsaFile, Charsets.UTF_8);

        for (int idx = 0; idx<rawDocs.size(); idx++) {
            String rawDoc = rawDocs.get(idx);
            String esaString = esaDocs.get(idx);

            TokenSequence ts = new TokenSequence();
            if ((idx % fold == 0 && testSet) //测试集情况
                    || (idx % fold != 0 && !testSet)) { //训练集情况
                List<Term> terms = HanLP.segment(rawDoc);
                for (Term term : terms) {
                    if (term.word.length() < 2) continue;
                    if (deny(term.nature.name())) {
                        continue;
                    }

                    ts.add(new Token(term.word));
                }
            }

            List<String> concepts = JSON.parseArray(esaString, String.class);

            int count = 0;
            for (String concept : concepts) {
                count++;
                ts.add(new Token(concept));
                if (count == Top_Concepts) break;
            }
            Instance instance = new Instance(ts, "Positive", "pos-" + idx, null);
            instances.addThruPipe(instance);
        }

        File negDocsEsaFile = new File(negDocsRawFile.getParentFile(), "negative.esa");

        rawDocs = Files.readLines(negDocsRawFile, Charsets.UTF_8);
        esaDocs = Files.readLines(negDocsEsaFile, Charsets.UTF_8);

        for (int idx = 0; idx<rawDocs.size(); idx++) {
            String rawDoc = rawDocs.get(idx);
            String esaString = esaDocs.get(idx);

            TokenSequence ts = new TokenSequence();
            if ((idx % fold == 0 && testSet) //测试集情况
                    || (idx % fold != 0 && !testSet)) { //训练集情况
                List<Term> terms = HanLP.segment(rawDoc);
                for (Term term : terms) {
                    if (term.word.length() < 2) continue;
                    if (deny(term.nature.name())) {
                        continue;
                    }

                    ts.add(new Token(term.word));
                }
            }

            List<String> concepts = JSON.parseArray(esaString, String.class);

            int count = 0;
            for (String concept : concepts) {
                count++;
                ts.add(new Token(concept));
                if (count == Top_Concepts) break;
            }
            Instance instance = new Instance(ts, "Negative", "neg-" + idx, null);
            instances.addThruPipe(instance);
        }


        return instances;
    }


    public void testEsa(File positiveFile, File negativeFile, int type) throws IOException {
        InstanceList trainInstances = null;
        InstanceList testInstances = null;

        if(type == 1) {
            System.out.println("Test Raw...");
            trainInstances = loadRawInstances(positiveFile, negativeFile, 5, false);
            testInstances = loadRawInstances(positiveFile, negativeFile, 5, true);
        } else if (type == 2) {
            System.out.println("Test Esa...");
            trainInstances = loadEsaInstances(positiveFile, negativeFile, 5, false);
            testInstances = loadEsaInstances(positiveFile, negativeFile, 5, true);
        } else if(type == 3) {
            System.out.println("Test Raw + Esa...");
            trainInstances = loadMixInstances(positiveFile, negativeFile, 5, false);
            testInstances = loadMixInstances(positiveFile, negativeFile, 5, true);
        }

        Classifier classifier = null;

        if(modelType == ModelType.NB) {
            NaiveBayesTrainer trainer = new NaiveBayesTrainer();
            classifier = trainer.train(trainInstances);
        } else {
            SVMClassifierTrainer trainer = new SVMClassifierTrainer(new LinearKernel());
            classifier = trainer.train(trainInstances);
        }


        Trial trial = new Trial(classifier, testInstances);

        // The Trial class implements many standard evaluation
        //  metrics. See the JavaDoc API for more details.

        System.out.println("Accuracy: " + trial.getAccuracy());

        // precision, recall, and F1 are calcuated for a specific
        //  class, which can be identified by an object (usually
        //  a String) or the integer ID of the class

        //System.out.println("F1 for class 'good': " + trial.getF1("good"));

        LabelAlphabet labelAlphabet = classifier.getLabelAlphabet();
        double macro = 0;
        for (int i = 0; i < labelAlphabet.size(); i++) {
            System.out.println("Class '" + labelAlphabet.lookupLabel(i) + "'\t "
                    + "Precision: " +  trial.getPrecision(i)
                    + ", Recall: " + trial.getRecall(i)
                    + ", F1: " + trial.getF1(i)
            ) ;

            macro += trial.getF1(i);
        }

        System.out.println("MacroF:" + macro / labelAlphabet.size());
    }

    public static void main(String[] args) throws ParseException, IOException {
        String helpMsg = "usage: SentimentClassify -c config.xml";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("c", true, "config file"));
        options.addOption(new Option("in", true, "input weibo file."));
        options.addOption(new Option("out", true, "file for ESA analysis results"));
        options.addOption(new Option("t", true, "type: 1 for generate ESA, 2 for classify raw, 3 for classfy esa"));


        options.addOption(new Option("pf", true, "positive file."));
        options.addOption(new Option("nf", true, "negative file"));
        options.addOption(new Option("top", true, "keep topN concepts"));
        options.addOption(new Option("model", true, "nb or svm"));

        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("t")) {
            helpFormatter.printHelp(helpMsg, options);
            return;
        }

        int type = Integer.parseInt(commandLine.getOptionValue("t"));
        if (type == 1) {
            if(!commandLine.hasOption("c") || !commandLine.hasOption("in") || !commandLine.hasOption("out")) {
                System.out.println("Please specify input and output file.");
                helpFormatter.printHelp(helpMsg, options);
                return;
            }

            Conf conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);

            File in = new File(commandLine.getOptionValue("in"));
            File out = new File(commandLine.getOptionValue("out"));
            DataMaker maker = new DataMaker(conf);
            maker.generateESA(in, out);
        } else if (type == 2 || type == 3 || type==4) {
            SentimentClassify classify = new SentimentClassify();
            File positiveFile = new File(commandLine.getOptionValue("pf"));
            File negativeFile = new File(commandLine.getOptionValue("nf"));
            int topN = Integer.parseInt(commandLine.getOptionValue("top", "10"));
            System.out.println("TopN ==> " + topN);
            classify.Top_Concepts = topN;
            if("svm".equalsIgnoreCase(commandLine.getOptionValue("model"))) {
                classify.modelType = ModelType.SVM;
            }

            classify.testEsa(positiveFile, negativeFile, type-1);
        }

        System.out.println("I'm DONE!");
    }
}
