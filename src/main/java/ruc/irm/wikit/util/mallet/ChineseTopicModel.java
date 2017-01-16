package ruc.irm.wikit.util.mallet;

import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.FileIterator;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.topics.TopicAssignment;
import cc.mallet.topics.TopicInferencer;
import cc.mallet.types.*;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import java.io.File;
import java.net.URL;
import java.util.*;

public class ChineseTopicModel {

	public static void main(String[] args) throws Exception {
        String path = null;
        path = "/home/xiatian/workspace/data/sohu-dataset";

		// Begin by importing documents from text to feature sequences
		ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

		// Pipes: lowercase, tokenize, remove stopwords, map to features
		//add English stopwords
		URL url = Resources.getResource(ChineseTopicModel.class, "/stoplists/en.txt");
		List<String> lines = Resources.readLines(url, Charsets.UTF_8);
        TokenSequenceRemoveStopwords stopwordPipe = new TokenSequenceRemoveStopwords(false);
        stopwordPipe.addStopWords(lines.toArray(new String[lines.size()]));

		//add Chinese stopwords
		url = Resources.getResource(ChineseTopicModel.class, "/stoplists/cn.txt");
		lines = Resources.readLines(url, Charsets.UTF_8);
		stopwordPipe.addStopWords(lines.toArray(new String[lines.size()]));

        pipeList.add( new Input2CharSequence());
		pipeList.add( new CharSequenceLowercase() );
		pipeList.add( new ChineseSequence2TokenSequence(new String[]{"r", "w", "q", "m", "p", "f", "d", "t"}) );
		pipeList.add( new TokenSequence2FeatureSequence() );

		InstanceList instances = new InstanceList(new SerialPipes(pipeList));

		instances.addThruPipe(new FileIterator(new File(path))); // data, label, name fields

		// Create a model with 100 topics, alpha_t = 0.01, beta_w = 0.01
		//  Note that the first parameter is passed as the sum over topics, while
		//  the second is 
		int numTopics = 10;
		ParallelTopicModel model = new ParallelTopicModel(numTopics, 1.0, 0.01);

		model.addInstances(instances);

		// Use two parallel samplers, which each look at one half the corpus and combine
		//  statistics after every iteration.
		model.setNumThreads(2);

		// Run the model for 50 iterations and stop (this is for testing only, 
		//  for real applications, use 1000 to 2000 iterations)
		model.setNumIterations(100);
		model.estimate();

		// Show the words and topics in the first instance

		// The data alphabet maps word IDs to strings
		Alphabet dataAlphabet = instances.getDataAlphabet();

		TopicAssignment topicAssignment = model.getData().get(0);
		FeatureSequence tokens = (FeatureSequence) topicAssignment.instance.getData();
		LabelSequence topics = topicAssignment.topicSequence;

//		model.printState(new File("/tmp/lda.gz"));
//		model.topicPhraseXMLReport(new PrintWriter(new File("/tmp/hello.txt")), 1000);
		model.printTopicWordWeights(new File("/tmp/words.txt"));

		Formatter out = new Formatter(new StringBuilder(), Locale.US);
		for (int position = 0; position < tokens.getLength(); position++) {
			out.format("%s-%d ", dataAlphabet.lookupObject(tokens.getIndexAtPosition(position)), topics.getIndexAtPosition(position));
		}
		System.out.println(out);
		
		// Estimate the topic distribution of the first instance, 
		//  given the current Gibbs state.
		double[] topicDistribution = model.getTopicProbabilities(0);

		// Get an array of sorted sets of word ID/count pairs
		ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();
		
		// Show top 5 words in topics with proportions for the first document
		for (int topic = 0; topic < numTopics; topic++) {
			Iterator<IDSorter> iterator = topicSortedWords.get(topic).iterator();
			
			out = new Formatter(new StringBuilder(), Locale.US);
			out.format("%d\t%.3f\t", topic, topicDistribution[topic]);
			int rank = 0;
			while (iterator.hasNext() && rank < 5) {
				IDSorter idCountPair = iterator.next();
				out.format("%s (%.0f) ", dataAlphabet.lookupObject(idCountPair.getID()), idCountPair.getWeight());
				rank++;
			}
			System.out.println(out);
		}
	
		// Create a new instance with high probability of topic 0
		StringBuilder topicZeroText = new StringBuilder();
		Iterator<IDSorter> iterator = topicSortedWords.get(0).iterator();

		int rank = 0;
		while (iterator.hasNext() && rank < 5) {
			IDSorter idCountPair = iterator.next();
			topicZeroText.append(dataAlphabet.lookupObject(idCountPair.getID()) + " ");
			rank++;
		}

		// Create a new instance named "test instance" with empty target and source fields.
		InstanceList testing = new InstanceList(instances.getPipe());
		testing.addThruPipe(new Instance(topicZeroText.toString(), null, "test instance", null));

		TopicInferencer inferencer = model.getInferencer();
		double[] testProbabilities = inferencer.getSampledDistribution(testing.get(0), 10, 1, 5);
		System.out.println("0\t" + testProbabilities[0]);
	}

}