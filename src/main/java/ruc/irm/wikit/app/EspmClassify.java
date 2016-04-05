package ruc.irm.wikit.app;

import cc.mallet.classify.Classifier;
import cc.mallet.classify.FeatureSelectingClassifierTrainer;
import cc.mallet.classify.NaiveBayesTrainer;
import cc.mallet.classify.Trial;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.pipe.iterator.FileIterator;
import cc.mallet.types.*;
import org.apache.commons.cli.*;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.math.NumberUtils;
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
 *
 * @author Tian Xia
 * @date Feb 24, 2016 11:17 AM
 */
public class EspmClassify {

    private String filterFileName = null;

    private int topFeatures = 1000;

    private File getFilterFile() {
        return new File("/tmp/features", filterFileName + "." + topFeatures);
    }

    Pipe makePipe(File homeDir, MyPipe.Type type) {
        return new SerialPipes(new Pipe[]{
                new Target2Label(),
                new Input2CharSequence(),
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

    public Classifier trainClassifier(InstanceList trainingInstances) {

        // Here we use a maximum entropy (ie polytomous logistic regression)
        //  classifier. Mallet includes a wide variety of classification
        //  algorithms, see the JavaDoc API for details.

        //NaiveBayesTrainer trainer = new NaiveBayesTrainer();
        //SVMClassifierTrainer trainer = new SVMClassifierTrainer(new LinearKernel());

        SVMClassifierTrainer baseTrainer = new SVMClassifierTrainer(new LinearKernel());
        //NaiveBayesTrainer baseTrainer = new NaiveBayesTrainer();

        RankedFeatureVector.Factory ranker = null;
        //ranker = new FeatureCounts.Factory();
        ranker = new InfoGain.Factory();
        FeatureSelector selector = new FeatureSelector(ranker, topFeatures);
        File f = getFilterFile();
        if (!f.exists()) {
            selector.saveSelectedFeatures(f, trainingInstances);
            return new NaiveBayesTrainer().train(trainingInstances);
        } else {
            return baseTrainer.train(trainingInstances);
        }



        //FeatureSelectingClassifierTrainer trainer = new FeatureSelectingClassifierTrainer(baseTrainer, selector);


        //Classifier c = trainer.train(trainingInstances);
        //Classifier c = baseTrainer.train(trainingInstances);

        //return c;
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

    /**
     * fold: 表示十折交叉验证的第几个
     */
    public Map<String, Double> test(File rawCorpusDir, File miningDir, boolean raw, boolean esa, boolean espm, boolean espmesa, int fold) {
        Map<String, Double> results = new HashedMap();

        StringBuilder sb = new StringBuilder();
        if (raw) {
            System.out.println("process raw data...");
            this.filterFileName = "PURE_BOW-" + fold;
            InstanceList instances = load20NGInstances(rawCorpusDir, miningDir, MyPipe.Type.PURE_BOW);
            double accuracy = testTrainSplit(instances, fold);

            sb.append("PURE_BOW\t==> " + accuracy).append("\n");
            System.out.println("PURE_BOW\t=> " + accuracy);
            results.put("PURE_BOW", accuracy);
        }

        if (esa) {
            System.out.println("process ESA data...");
            this.filterFileName = "PURE_ESA-" + fold;
            InstanceList instances = load20NGInstances(rawCorpusDir, miningDir, MyPipe.Type.PURE_ESA);
            double accuracy = testTrainSplit(instances, fold);

            sb.append("PURE_ESA\t==> " + accuracy).append("\n");
            System.out.println("PURE_ESA\t=> " + accuracy);
            results.put("PURE_ESA", accuracy);


            this.filterFileName = "BOW_ESA-" + fold;
            instances = load20NGInstances(rawCorpusDir, miningDir, MyPipe.Type.BOW_ESA);
            accuracy = testTrainSplit(instances, fold);

            sb.append("BOW_ESA\t==> " + accuracy).append("\n");
            System.out.println("BOW_ESA\t=> " + accuracy);
            results.put("BOW_ESA", accuracy);
        }

        if (espm) {
            System.out.println("process ESPM data...");
            this.filterFileName = "PURE_ESPM-" + fold;
            InstanceList instances = load20NGInstances(rawCorpusDir, miningDir, MyPipe.Type.PURE_ESPM);
            double accuracy = testTrainSplit(instances, fold);

            sb.append("PURE_ESPM\t==> " + accuracy).append("\n");
            System.out.println("PURE_ESPM\t=> " + accuracy);
            results.put("PURE_ESPM", accuracy);


            this.filterFileName = "BOW_ESPM-" + fold;
            instances = load20NGInstances(rawCorpusDir, miningDir, MyPipe.Type.BOW_ESPM);
            accuracy = testTrainSplit(instances, fold);

            sb.append("BOW_ESPM\t==> " + accuracy).append("\n");
            System.out.println("BOW_ESPM\t=> " + accuracy);
            results.put("BOW_ESPM", accuracy);
        }


        if (espmesa) {
            System.out.println("process ESPM and ESA data...");
            this.filterFileName = "PURE_ESPM_ESA-" + fold;
            InstanceList instances = load20NGInstances(rawCorpusDir, miningDir, MyPipe.Type.PURE_ESPM_ESA);
            double accuracy = testTrainSplit(instances, fold);

            sb.append("PURE_ESPM_ESA\t==> " + accuracy).append("\n");
            System.out.println("PURE_ESPM_ESA\t=> " + accuracy);
            results.put("PURE_ESPM_ESA", accuracy);

            this.filterFileName = "BOW_ESPM_ESA-" + fold;
            instances = load20NGInstances(rawCorpusDir, miningDir, MyPipe.Type.BOW_ESPM_ESA);
            accuracy = testTrainSplit(instances, fold);

            sb.append("BOW_ESPM_ESA\t==> " + accuracy).append("\n");
            System.out.println("BOW_ESPM_ESA\t=> " + accuracy);
            results.put("BOW_ESPM_ESA", accuracy);
        }

        String tip = "Fold:" + fold + ", features:" + topFeatures;
        System.out.println(tip);
        System.out.println(sb.toString());
        return results;
    }

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
            System.out.println(entry.getKey() + "==>" + catList.size());
            int index = 0;
            for (Instance instance : catList) {
                list.get(index).add(instance);
                index = (index + 1) % m;
            }
        }

        return list.toArray(new InstanceList[m]);
    }

    public double testTrainSplit(InstanceList instances, int fold) {

        int TRAINING = 0;
        int TESTING = 1;
        int VALIDATION = 2;

        // Split the input list into training (90%) and testing (10%) lists.
        // The division takes place by creating a copy of the list,
        //  randomly shuffling the copy, and then allocating
        //  instances to each sub-list based on the provided proportions.

        //InstanceList[] instanceLists = instances.split(new Randoms(),  new double[] {0.9, 0.1, 0.0});
        //InstanceList[] instanceLists = instances.splitInTwoByModulo(6); //1份测试，9份训练

//        InstanceList[] instanceLists = instances.splitInOrder(new double[]{10, 10, 10, 10, 10,
//                10, 10, 10, 10, 10});

        InstanceList[] instanceLists = split(instances, 10);
        //cross validate
        double accuracy = 0;
        System.out.println(instanceLists.length);
        for (int i = 0; i < instanceLists.length; i++) {
            System.out.println("size:" + instanceLists[i].size());
        }


        InstanceList trainList = instances.cloneEmpty();
        InstanceList testList = instances.cloneEmpty();
        for (int i = 0; i < instanceLists.length; i++) {
            if (i == fold) {
                testList.addAll(instanceLists[i]);
            } else {
                trainList.addAll(instanceLists[i]);
            }
        }
        Classifier classifier = trainClassifier(trainList);
        System.out.println("Training set:" + trainList.size());
        System.out.println("Test set:" + testList.size());
        Trial trial = new Trial(classifier, testList);
        accuracy = trial.getAccuracy();
        System.out.println("accuracy:" + accuracy);

        return accuracy;


//        int folds = 2; //10
//        for(int step=0; step<instanceLists.length && step<folds; step++) {
//            InstanceList trainList = instances.cloneEmpty();
//            InstanceList testList = instances.cloneEmpty();
//
//            for (int i = 0; i < instanceLists.length; i++) {
//                if(i==step) {
//                    testList.addAll(instanceLists[i]);
//                } else{
//                    trainList.addAll(instanceLists[i]);
//                }
//            }
//
//            Classifier classifier = trainClassifier(trainList, name + step);
//            System.out.println("Training set:" + trainList.size());
//            System.out.println("Test set:" + testList.size());
//            Trial trial = new Trial(classifier, testList);
//            System.out.println("accuracy:" + trial.getAccuracy());
//            accuracy  += trial.getAccuracy();
//        }
//
//        return accuracy/folds;

//        InstanceList trainList = instanceLists[1];
//        InstanceList testList = instanceLists[0];

        // The third position is for the "validation" set,
        //  which is a set of instances not used directly
        //  for training, but available for determining
        //  when to stop training and for estimating optimal
        //  settings of nuisance parameters.
        // Most Mallet ClassifierTrainers can not currently take advantage
        //  of validation sets.

//        Classifier classifier = trainClassifier(trainList);
//        return new Trial(classifier, testList);
        //return null;
    }

    private static void test() {
        EspmClassify classify = new EspmClassify();
        File dir = new File("/home/xiatian/data/20news-subject");
        File dir2 = new File("/home/xiatian/data/20news-mining-subject");
        classify.test(dir, dir2, true, false, false, false, 0);
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
        options.addOption(new Option("fold", true, "which fold."));


        String name = EspmClassify.class.getSimpleName();
        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("corpusDir")) {
            String usage = "Usage: run.py " + name + " -corpusDir pathname";
            helpFormatter.printHelp(usage, options);
            return;
        }

        EspmClassify classify = new EspmClassify();
        classify.topFeatures = NumberUtils.toInt(commandLine.getOptionValue("features"), 1000);

        File rawDir = new File(commandLine.getOptionValue("corpusDir"));
        File miningDir = new File(commandLine.getOptionValue("miningDir"));

        File output = new File("/tmp/espm.expt." + classify.topFeatures + ".txt");
        PrintWriter writer = new PrintWriter(new FileWriter(output));
        Map<String, Double> results = new HashedMap();
        for(int fold=0; fold<10; fold++) {
            classify.test(rawDir, miningDir,
                    commandLine.hasOption("raw"),
                    commandLine.hasOption("esa"),
                    commandLine.hasOption("espm"),
                    commandLine.hasOption("espmesa"), fold);

            Map<String, Double> map = classify.test(rawDir, miningDir,
                    commandLine.hasOption("raw"),
                    commandLine.hasOption("esa"),
                    commandLine.hasOption("espm"),
                    commandLine.hasOption("espmesa"), fold);

            for (Map.Entry<String, Double> entry : map.entrySet()) {
                if (results.containsKey(entry.getKey())) {
                    results.put(entry.getKey(), entry.getValue() + results.get(entry.getKey()));
                } else {
                    results.put(entry.getKey(), entry.getValue());
                }

                writer.println(entry.getKey() + "\t==> " + entry.getValue());
            }

            writer.println("\n");
            writer.flush();
        }

        writer.println("AVERAGE for features " + classify.topFeatures);
        for (Map.Entry<String, Double> entry : results.entrySet()) {
            writer.println(entry.getKey() + "\t" + entry.getValue());
        }
        writer.close();
        System.out.println("I'm DONE!");
        //*/
    }
}

