package ruc.irm.wikit.app;

import cc.mallet.classify.*;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.pipe.iterator.FileIterator;
import cc.mallet.types.*;
import cc.mallet.util.Randoms;
import org.apache.commons.cli.*;
import ruc.irm.wikit.nlp.libsvm.SVMClassifier;
import ruc.irm.wikit.nlp.libsvm.SVMClassifierTrainer;
import ruc.irm.wikit.nlp.libsvm.kernel.LinearKernel;
import ruc.irm.wikit.util.mallet.PorterStemmerPipe;

import java.io.*;
import java.util.Iterator;

/**
 * Test classification effects with ESPM support
 *
 * Example:
 * ./run.py EspmClassify -dir /home/xiatian/data/20news-18828
 *
 * @author Tian Xia
 * @date Feb 24, 2016 11:17 AM
 */
public class EspmClassify {
    /**
     * Load 20Newsgroup corpus
     *
     * @param ngCorpusPath
     * @return
     */
    public InstanceList load20NGInstances(File ngCorpusPath) {
        InstanceList instances = new InstanceList(new SerialPipes(new Pipe[] {
                new Target2Label(),
                new Input2CharSequence(),
                new CharSequence2TokenSequence(),
                new TokenSequenceRemoveStopwords(),
                new PorterStemmerPipe(),
                new TokenSequence2FeatureSequence(),
                new FeatureSequence2FeatureVector ()}));


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

        //SVMClassifierTrainer baseTrainer = new SVMClassifierTrainer(new LinearKernel());
        NaiveBayesTrainer baseTrainer = new NaiveBayesTrainer();

        RankedFeatureVector.Factory ranker = new FeatureCounts.Factory();
        FeatureSelector selector = new FeatureSelector(ranker, 50);

        FeatureSelectingClassifierTrainer trainer = new FeatureSelectingClassifierTrainer
                (baseTrainer, selector);


        Classifier c = trainer.train(trainingInstances);
//        Alphabet alphabet = c.getFeatureSelection().getAlphabet();
//        Iterator it = alphabet.iterator();
//        while (it.hasNext()) {
//            Object obj = it.next();
//            System.out.println(obj);
//        }
        return c;
    }


    public Classifier loadClassifier(File serializedFile)
            throws FileNotFoundException, IOException, ClassNotFoundException {
        Classifier classifier;

        ObjectInputStream ois =  new ObjectInputStream (new FileInputStream(serializedFile));
        classifier = (Classifier) ois.readObject();
        ois.close();

        return classifier;
    }

    public void saveClassifier(Classifier classifier, File serializedFile)
            throws IOException {
        ObjectOutputStream oos =
                new ObjectOutputStream(new FileOutputStream (serializedFile));
        oos.writeObject (classifier);
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

            for (int rank = 0; rank < labeling.numLocations(); rank++){
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
        System.out.println("Macro:" + macro/labelAlphabet.size());
    }

    public Trial testTrainSplit(InstanceList instances) {

        int TRAINING = 0;
        int TESTING = 1;
        int VALIDATION = 2;

        // Split the input list into training (90%) and testing (10%) lists.
        // The division takes place by creating a copy of the list,
        //  randomly shuffling the copy, and then allocating
        //  instances to each sub-list based on the provided proportions.

        //InstanceList[] instanceLists = instances.split(new Randoms(),  new double[] {0.9, 0.1, 0.0});
        InstanceList[] instanceLists = instances.splitInTwoByModulo(5); //1份测试，9份训练

        InstanceList trainList = instanceLists[1];
        InstanceList testList = instanceLists[0];

        // The third position is for the "validation" set,
        //  which is a set of instances not used directly
        //  for training, but available for determining
        //  when to stop training and for estimating optimal
        //  settings of nuisance parameters.
        // Most Mallet ClassifierTrainers can not currently take advantage
        //  of validation sets.

        Classifier classifier = trainClassifier(trainList);
        return new Trial(classifier, testList);
    }


    public static void main(String[] args) throws ParseException {
        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("dir", true, "classification home folder."));


        String name = EspmClassify.class.getSimpleName();
        CommandLine commandLine = parser.parse(options, args);
        if(!commandLine.hasOption("dir")) {
            String usage = "Usage: run.py " + name + " -dir pathname";
            helpFormatter.printHelp(usage, options);
            return;
        }

        EspmClassify classify = new EspmClassify();
        File corpusDir = new File(commandLine.getOptionValue("dir"));
        InstanceList instances = classify.load20NGInstances(corpusDir);
        Trial trial = classify.testTrainSplit(instances);
        classify.printTrial(trial);
    }
}

