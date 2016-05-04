package ruc.irm.wikit.app;

import cc.mallet.classify.Classifier;
import cc.mallet.classify.Trial;
import cc.mallet.classify.evaluate.ConfusionMatrix;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.pipe.iterator.FileIterator;
import cc.mallet.types.*;
import org.apache.commons.cli.*;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.nlp.libsvm.SVMClassifierTrainer;
import ruc.irm.wikit.nlp.libsvm.kernel.LinearKernel;
import ruc.irm.wikit.util.mallet.PorterStemmerPipe;

import java.io.*;
import java.util.*;

/**
 * Test classification effects with ESPM support
 * <p>
 * Example:
 * ./run.py EspmClassify -dir /home/xiatian/data/20news-18828
 *  ./run.py EspmClassify -corpusDir /home/xiatian/data/20news-subject -miningDir /home/xiatian/data/20news-mining-subject -raw -esa -espm -espmesa -features 2000
 *
 * @author Tian Xia
 * @date Feb 24, 2016 11:17 AM
 */
public class EspmClassify {
    private static final Logger LOG = LoggerFactory.getLogger(EspmClassify.class);

    private String filterFileName = null;

    private int topFeatures = 1000;
    private String redisKeyStartName = "expt:20ng_full:svm:";
    private Jedis jedis = null;

    public EspmClassify(){
        Conf conf = ConfFactory.defaultConf();
        this.jedis = new Jedis(conf.getRedisHost(), conf.getRedisPort(), conf.getRedisTimeout());
    }

    private File getFilterFile() {
        return new File("/home/xiatian/git/ESPM/features", filterFileName + "." + topFeatures);
    }

    Pipe makePipe(File homeDir, MyPipe.Type type) {
        return new SerialPipes(new Pipe[]{
                new Target2Label(),
                new Input2CharSequence(),
                new CharSequenceLowercase(),
                new CharSequence2TokenSequence(),
                new TokenSequenceRemoveStopwords(),
                new PorterStemmerPipe(),
                new MyPipe(homeDir, type),
                new TokenSequenceLowercase(),
                new FilterTokenSequence(getFilterFile()), //只有出现在文件中的词语会保留
                new TokenSequence2FeatureSequence(),
                new FeatureSequence2FeatureVector()});
    }

    /**
     * Load 20Newsgroup corpus
     *
     * @param ngCorpusPath
     * @return
     */
    public InstanceList load20NGInstances(File ngCorpusPath, File miningDir, MyPipe.Type type) {


        InstanceList instances = new InstanceList(makePipe(miningDir, type));

        File[] dirs = ngCorpusPath.listFiles();
        instances.addThruPipe(new FileIterator(dirs, FileIterator.LAST_DIRECTORY));

        return instances;
    }

    public Classifier loadClassifier(File serializedFile)
            throws FileNotFoundException, IOException, ClassNotFoundException {
        Classifier classifier;

        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(serializedFile));
        classifier = (Classifier) ois.readObject();
        ois.close();

        return classifier;
    }

    public void saveClassifier(Classifier classifier, File serializedFile)
            throws IOException {
        ObjectOutputStream oos =
                new ObjectOutputStream(new FileOutputStream(serializedFile));
        oos.writeObject(classifier);
        oos.close();
    }

    public void printLabelings(Classifier classifier, File file) throws IOException {

        // Create a new iterator that will read raw instance data from
        //  the lines of a file.
        // Lines should be formatted as:
        //
        //   [name] [label] [data ... ]
        //
        //  in this case, "label" is ignored.

        CsvIterator reader =
                new CsvIterator(new FileReader(file),
                        "(\\w+)\\s+(\\w+)\\s+(.*)",
                        3, 2, 1);  // (data, label, name) field indices

        // Create an iterator that will pass each instance through
        //  the same pipe that was used to create the training data
        //  for the classifier.
        Iterator instances =
                classifier.getInstancePipe().newIteratorFrom(reader);

        // Classifier.classify() returns a Classification object
        //  that includes the instance, the classifier, and the
        //  classification results (the labeling). Here we only
        //  care about the Labeling.
        while (instances.hasNext()) {
            Labeling labeling = classifier.classify(instances.next()).getLabeling();

            // print the labels with their weights in descending order (ie best first)

            for (int rank = 0; rank < labeling.numLocations(); rank++) {
                System.out.print(labeling.getLabelAtRank(rank) + ":" +
                        labeling.getValueAtRank(rank) + " ");
            }
            System.out.println();

        }
    }

    public void evaluate(Classifier classifier, File file) throws IOException {

        // Create an InstanceList that will contain the test data.
        // In order to ensure compatibility, process instances
        //  with the pipe used to process the original training
        //  instances.

        InstanceList testInstances = new InstanceList(classifier.getInstancePipe());

        // Create a new iterator that will read raw instance data from
        //  the lines of a file.
        // Lines should be formatted as:
        //
        //   [name] [label] [data ... ]

        CsvIterator reader =
                new CsvIterator(new FileReader(file),
                        "(\\w+)\\s+(\\w+)\\s+(.*)",
                        3, 2, 1);  // (data, label, name) field indices

        // Add all instances loaded by the iterator to
        //  our instance list, passing the raw input data
        //  through the classifier's original input pipe.

        testInstances.addThruPipe(reader);

        Trial trial = new Trial(classifier, testInstances);

        // The Trial class implements many standard evaluation
        //  metrics. See the JavaDoc API for more details.

        System.out.println("Accuracy: " + trial.getAccuracy());

        // precision, recall, and F1 are calcuated for a specific
        //  class, which can be identified by an object (usually
        //  a String) or the integer ID of the class

        System.out.println("F1 for class 'good': " + trial.getF1("good"));

        System.out.println("Precision for class '" +
                classifier.getLabelAlphabet().lookupLabel(1) + "': " +
                trial.getPrecision(1));
    }


    public void printTrial(Trial trial) {
        System.out.println("Accuracy(Micro): " + trial.getAccuracy());
        trial.getAverageRank();
        LabelAlphabet labelAlphabet = trial.getClassifier().getLabelAlphabet();
        double macro = 0;
        for (int i = 0; i < labelAlphabet.size(); i++) {
            System.out.println("F1 for class '" +
                    labelAlphabet.lookupLabel(i) + "': " +
                    trial.getF1(i));
            macro += trial.getF1(i);
        }
        System.out.println("Macro:" + macro / labelAlphabet.size());
    }


    private void setFilterFileName(String filename) {
        this.filterFileName = filename;
    }


    private void generateTopFeatures(File rawCorpusDir, File miningDir, int fold, MyPipe.Type type) {
        //生成topFeatures
        getFilterFile().delete();

        InstanceList instances = load20NGInstances(rawCorpusDir, miningDir, type);
        InstanceList[] instanceLists = split(instances, 10);

        InstanceList trainList = instances.cloneEmpty();
        for (int i = 0; i < instanceLists.length; i++) {
            if (i == fold) {
                ;
            } else {
                trainList.addAll(instanceLists[i]);
            }
        }

        RankedFeatureVector.Factory ranker = null;
        //ranker = new FeatureCounts.Factory();
        ranker = new InfoGain.Factory();
        FeatureSelector selector = new FeatureSelector(ranker, topFeatures);
        File f = getFilterFile();
        selector.saveSelectedFeatures(f, trainList);
    }

    public void runTask(String taskName, File rawCorpusDir, File miningDir, int fold, MyPipe.Type pipeType) {
        LOG.info("Method: " + taskName + ", Fold: " + fold);
        setFilterFileName(taskName + "-" + fold);

        //生成topFeatures
        getFilterFile().delete();

        LOG.debug("Generate features...");
        generateTopFeatures(rawCorpusDir, miningDir, fold, pipeType);

        LOG.debug("Evaluate...");
        InstanceList instances = load20NGInstances(rawCorpusDir, miningDir, pipeType);
        trainAndEvaluate(taskName, instances, fold);
    }


    /**
     * fold: 表示十折交叉验证的第几个
     */
    public void doFullTest(File rawCorpusDir, File miningDir, boolean raw, boolean esa, boolean espm, boolean espmesa, int fold) {

        Map<String, Double> results = new HashedMap();

        StringBuilder sb = new StringBuilder();
        if (raw) {
            runTask("BoW", rawCorpusDir, miningDir, fold, MyPipe.Type.PURE_BOW);
        }

        if (esa) {
            runTask("ESA", rawCorpusDir, miningDir, fold, MyPipe.Type.PURE_ESA);
            runTask("BoW+ESA", rawCorpusDir, miningDir, fold, MyPipe.Type.BOW_ESA);
        }

        if (espm) {
            runTask("ESPM", rawCorpusDir, miningDir, fold, MyPipe.Type.PURE_ESPM);
            runTask("BoW+ESPM", rawCorpusDir, miningDir, fold, MyPipe.Type.BOW_ESPM);
        }


        if (espmesa) {
            runTask("ESPM+ESA", rawCorpusDir, miningDir, fold, MyPipe.Type.PURE_ESPM_ESA);
            runTask("BoW+ESPM+ESA", rawCorpusDir, miningDir, fold, MyPipe.Type.BOW_ESPM_ESA);
        }
    }

    /**
     * 把数据列表拆分为m等份，每等份里面的类别分布也保持均匀
     */
    InstanceList[] split(InstanceList instances, int m) {
        List<InstanceList> list = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            list.add(new InstanceList(instances.getPipe()));
        }

        Map<Object, List<Instance>> map = new HashMap<>();
        for (Instance instance : instances) {
            List<Instance> catList = map.get(instance.getTarget());
            if (catList == null) {
                catList = new ArrayList<>();
            }
            catList.add(instance);
            map.put(instance.getTarget(), catList);
        }

        for (Map.Entry<Object, List<Instance>> entry : map.entrySet()) {
            List<Instance> catList = entry.getValue();
            //System.out.println(entry.getKey() + "==>" + catList.size());
            int index = 0;
            for (Instance instance : catList) {
                list.get(index).add(instance);
                index = (index + 1) % m;
            }
        }

        return list.toArray(new InstanceList[m]);
    }


    public void trainAndEvaluate(String name, InstanceList instances, int fold) {
        InstanceList[] instanceLists = split(instances, 10);

        InstanceList trainList = instances.cloneEmpty();
        InstanceList testList = instances.cloneEmpty();
        for (int i = 0; i < instanceLists.length; i++) {
            if (i == fold) {
                testList.addAll(instanceLists[i]);
            } else {
                trainList.addAll(instanceLists[i]);
            }
        }

        SVMClassifierTrainer trainer = new SVMClassifierTrainer(new LinearKernel());
        Classifier classifier = trainer.train(trainList);

        Trial trial = new Trial(classifier, testList);
        double accuracy = trial.getAccuracy();

        //把P、R和F值等信息保存到Redis中，方便计算
        String prefix = redisKeyStartName + "features:" + topFeatures + ":" +name + ":fold:" + fold + ":";
        LabelAlphabet labelAlphabet = classifier.getLabelAlphabet();
        double macroF = 0;

        for (int i = 0; i < labelAlphabet.size(); i++) {
            String category = labelAlphabet.lookupLabel(i).toString();
            String key = prefix + "cat:" + category;
            jedis.hset(key, "P", String.format("%2.7f",trial.getPrecision(i)));
            jedis.hset(key, "R", String.format("%2.7f",trial.getRecall(i)));
            jedis.hset(key, "F", String.format("%2.7f",trial.getF1(i)));
            macroF = macroF + trial.getF1(i);
        }

        ConfusionMatrix matrix = new ConfusionMatrix(trial);
        jedis.set(prefix + "confusion", matrix.toString());
        jedis.set(prefix + "accuracy", String.format("%2.7f", accuracy));
        jedis.set(prefix + "macroF", String.format("%2.7f", macroF/labelAlphabet.size()));
    }

    private double getF1(String name, int topFeatures, String category) {
        double total = 0;
        for(int fold=0; fold<10; fold++) {
            String key = redisKeyStartName + "features:" + topFeatures + ":" +name + ":fold:" + fold + ":cat:" + category;
            total += Double.parseDouble(jedis.hget(key, "F"));
        }
        return total/10;
    }

    public void showLaTexResult(int topFeatures) {
        StringBuilder sb = new StringBuilder();
        Map<String, Double> map = new HashedMap();
        int catLength = NewsGroupCorpus.Categories.length;

        for (String cat : NewsGroupCorpus.Categories) {
            sb.append(cat).append(" & ");

            double f1 = getF1("BoW", topFeatures, cat);
            map.put("BoW", map.getOrDefault("BoW", 0.0) + f1);
            sb.append(String.format("%2.2f",f1*100)).append(" & ");

            f1 = getF1("ESA", topFeatures, cat);
            map.put("ESA", map.getOrDefault("ESA", 0.0) + f1);
            sb.append(String.format("%2.2f",f1*100)).append(" & ");

            f1 = getF1("ESPM", topFeatures, cat);
            map.put("ESPM", map.getOrDefault("ESPM", 0.0) + f1);
            sb.append(String.format("%2.2f",f1*100)).append(" & ");

            f1 = getF1("ESPM+ESA", topFeatures, cat);
            map.put("ESPM+ESA", map.getOrDefault("ESPM+ESA", 0.0) + f1);
            sb.append(String.format("%2.2f",f1*100)).append(" & ");

            f1 = getF1("BoW+ESA", topFeatures, cat);
            map.put("BoW+ESA", map.getOrDefault("BoW+ESA", 0.0) + f1);
            sb.append(String.format("%2.2f",f1*100)).append(" & ");

            f1 = getF1("BoW+ESPM", topFeatures, cat);
            map.put("BoW+ESPM", map.getOrDefault("BoW+ESPM", 0.0) + f1);
            sb.append(String.format("%2.2f",f1*100)).append(" & ");

            f1 = getF1("BoW+ESPM+ESA", topFeatures, cat);
            map.put("BoW+ESPM+ESA", map.getOrDefault("BoW+ESPM+ESA", 0.0) + f1);
            sb.append(String.format("%2.2f",f1*100)).append(" \\\\\n");
        }

        sb.append("\\hline\n");
        sb.append("Macro F1 & ");
        sb.append(String.format("%2.2f",100 * map.get("BoW")/catLength)).append(" & ");
        sb.append(String.format("%2.2f",100 * map.get("ESA")/catLength)).append(" & ");
        sb.append(String.format("%2.2f",100 * map.get("ESPM")/catLength)).append(" & ");
        sb.append(String.format("%2.2f",100 * map.get("ESPM+ESA")/catLength)).append(" & ");
        sb.append(String.format("%2.2f",100 * map.get("BoW+ESA")/catLength)).append(" & ");
        sb.append(String.format("%2.2f",100 * map.get("BoW+ESPM")/catLength)).append(" & ");
        sb.append(String.format("%2.2f",100 * map.get("BoW+ESPM+ESA")/catLength));
        sb.append("\\\\\n");

        System.out.println(sb.toString());
    }


    public static void main(String[] args) throws ParseException, IOException {
        //test();

        ///*
        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("corpusDir", true, "raw classification home folder."));
        options.addOption(new Option("miningDir", true, "mining result home folder."));
        options.addOption(new Option("raw", false, "view raw classification result."));
        options.addOption(new Option("esa", false, "view esa classification result."));
        options.addOption(new Option("espm", false, "view espm classification result."));
        options.addOption(new Option("espmesa", false, "view espmesa classification result."));
        options.addOption(new Option("features", true, "Top number features to users."));
        options.addOption(new Option("view", false, "View Results"));
        options.addOption(new Option("corpusType", true, "full|title(full text or title only)"));
        //options.addOption(new Option("fold", true, "which fold."));


        String name = EspmClassify.class.getSimpleName();
        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("corpusDir") && !commandLine.hasOption("view")) {
            String usage = "Usage: run.py " + name + " -corpusDir pathname";
            helpFormatter.printHelp(usage, options);
            return;
        }

        EspmClassify classify = new EspmClassify();
        String featuresParam = commandLine.getOptionValue("features", "1000");

        if(featuresParam.equals("all")) {
            classify.topFeatures = Integer.MAX_VALUE;
        } else {
            classify.topFeatures = NumberUtils.toInt(featuresParam);
        }

        if("title".equals(commandLine.getOptionValue("corpusType"))){
            classify.redisKeyStartName = "expt:espm:svm"; //it should be "expt:20ng_title:svm:"
        } else if ("full".equals(commandLine.getOptionValue("corpusType"))) {
            classify.redisKeyStartName = "expt:20ng_full:svm:";
        } else {
            System.out.println("corpusType must be specified: full or title.");
            return;
        }

        if(commandLine.hasOption("view")) {
            classify.showLaTexResult(classify.topFeatures);
            return;
        }


        for(int fc: new int[]{1000, 2000, 3000, 4000, 5000, Integer.MAX_VALUE}) {
            System.out.println("process feature top :" + fc);
            classify.topFeatures = fc;
            File rawDir = new File(commandLine.getOptionValue("corpusDir"));
            File miningDir = new File(commandLine.getOptionValue("miningDir"));

            for (int fold = 0; fold < 10; fold++) {
                classify.doFullTest(rawDir, miningDir,
                        commandLine.hasOption("raw"),
                        commandLine.hasOption("esa"),
                        commandLine.hasOption("espm"),
                        commandLine.hasOption("espmesa"), fold);
            }

        }
        System.out.println("I'm DONE!");
    }
}

