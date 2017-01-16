package ruc.irm.wikit.nlp.libsvm;

import cc.mallet.classify.Classifier;
import cc.mallet.classify.ClassifierTrainer;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.types.InstanceList;
import ruc.irm.wikit.nlp.libsvm.kernel.LinearKernel;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * 
 * @author Syeed Ibn Faiz
 */
public class Main {

    public static void main(String[] args) throws IOException, Exception {
        ArrayList<Pipe> pipes = new ArrayList<Pipe>();
        pipes.add(new Target2Label());
        pipes.add(new CharSequence2TokenSequence());
        pipes.add(new TokenSequence2FeatureSequence());
        pipes.add(new FeatureSequence2FeatureVector());
        SerialPipes pipe = new SerialPipes(pipes);

        //prepare training instances
        InstanceList trainingInstanceList = new InstanceList(pipe);
        trainingInstanceList.addThruPipe(new CsvIterator(new FileReader("webkb-train-stemmed.txt"),
                "(.*)\t(.*)", 2, 1, -1));

        //prepare test instances
        InstanceList testingInstanceList = new InstanceList(pipe);
        testingInstanceList.addThruPipe(new CsvIterator(new FileReader("webkb-test-stemmed.txt"),
                "(.*)\t(.*)", 2, 1, -1));

        ClassifierTrainer trainer = new SVMClassifierTrainer(new LinearKernel());
        Classifier classifier = trainer.train(trainingInstanceList);
        System.out.println("Accuracy: " + classifier.getAccuracy(testingInstanceList));

    }
}
