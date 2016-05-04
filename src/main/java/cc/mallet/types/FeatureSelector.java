/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */




/**
	 Given an arbitrary scheme for ranking features, set of feature selection of
	 an InstanceList.

   @author Andrew McCallum <a href="mailto:mccallum@cs.umass.edu">mccallum@cs.umass.edu</a>
 */

package cc.mallet.types;

import cc.mallet.util.MalletLogger;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

public class FeatureSelector
{
	private static Logger logger = MalletLogger.getLogger(FeatureSelector.class.getName());
	// Only one of the following two will be non-null
	RankedFeatureVector.Factory ranker;
	RankedFeatureVector.PerLabelFactory perLabelRanker;
	// Only one of the following two will be changed
	int numFeatures = -1;
	double minThreshold = Double.POSITIVE_INFINITY;	

	public FeatureSelector (RankedFeatureVector.Factory ranker,
													int numFeatures)
	{
		this.ranker = ranker;
		this.numFeatures = numFeatures;
	}

	public FeatureSelector (RankedFeatureVector.Factory ranker,
													double minThreshold)
	{
		this.ranker = ranker;
		this.minThreshold = minThreshold;
	}
	
	public FeatureSelector (RankedFeatureVector.PerLabelFactory perLabelRanker,
													int numFeatures)
	{
		this.perLabelRanker = perLabelRanker;
		this.numFeatures = numFeatures;
	}

	public FeatureSelector (RankedFeatureVector.PerLabelFactory perLabelRanker,
													double minThreshold)
	{
		this.perLabelRanker = perLabelRanker;
		this.minThreshold = minThreshold;
	}

	
	public void selectFeaturesFor (InstanceList ilist)
	{
		if (perLabelRanker != null)
			selectFeaturesForPerLabel (ilist);
		else
			selectFeaturesForAllLabels (ilist);
	}

	public void saveSelectedFeatures(File f, InstanceList ilist)  {
		f.getParentFile().mkdirs();
		try {
			PrintWriter writer = new PrintWriter(new FileWriter(f));

			List<String> lines = new LinkedList<>();
			RankedFeatureVector ranking = ranker.newRankedFeatureVector(ilist);
			FeatureSelection fs = new FeatureSelection(ilist.getDataAlphabet());
			if (numFeatures != -1) { // Select by number of features.
				int nf = Math.min(numFeatures, ranking.singleSize());
				for (int i = 0; i < nf; i++) {
					//logger.info("adding feature " + i + " word=" + ilist.getDataAlphabet().lookupObject(ranking.getIndexAtRank(i)));
					writer.println(ilist.getDataAlphabet().lookupObject(ranking.getIndexAtRank(i)).toString());
				}
			} else { // Select by threshold.
				for (int i = 0; i < ranking.singleSize(); i++) {
					if (ranking.getValueAtRank(i) > minThreshold)
						writer.println(ilist.getDataAlphabet().lookupObject(ranking.getIndexAtRank(i)).toString());
				}
			}

			logger.info("Selected " + fs.cardinality() + " features from " +
					ilist.getDataAlphabet().size() + " features");
			writer.close();
			logger.info("Selected features are saved to file " + f.getAbsolutePath());
		} catch (IOException e) {
			throw new IOError(e);
		}
	}

	public void selectFeaturesForAllLabels (InstanceList ilist)
		
	{
		System.out.println("select for all all labels");
		RankedFeatureVector ranking = ranker.newRankedFeatureVector (ilist);
		FeatureSelection fs = new FeatureSelection (ilist.getDataAlphabet());
		if (numFeatures != -1) { // Select by number of features.
			int nf = Math.min (numFeatures, ranking.singleSize());
			for (int i = 0; i < nf; i++) {
				logger.info ("adding feature "+i+" word="+ilist.getDataAlphabet().lookupObject(ranking.getIndexAtRank(i)));
				fs.add (ranking.getIndexAtRank(i));
			}
		} else { // Select by threshold.
			for (int i = 0; i < ranking.singleSize(); i++) {
				if (ranking.getValueAtRank(i) > minThreshold)
					fs.add (ranking.getIndexAtRank(i));
			}
		}
		logger.info("Selected " + fs.cardinality() + " features from " +
								ilist.getDataAlphabet().size() + " features");

		//过滤Instance中多余的Feature
//		for (Instance instance : ilist) {
//			FeatureVector fv = (FeatureVector) instance.getData ();
//			List<Integer> indexList = new ArrayList();
//			List<Double> valueList = new ArrayList();
//			for (int idx : fv.getIndices()) {
//				if (fs.contains(idx)) {
//					indexList.add(idx);
//					valueList.add(fv.value(idx));
//				}
//			}
//			int[] indices = ArrayListUtils.toIntArray(indexList);
//			double[] values = ArrayListUtils.toDoubleArray(valueList);
//			fv.setIndices(indices);
//			fv.setValues(values);
//		}

		ilist.setPerLabelFeatureSelection (null);
		ilist.setFeatureSelection (fs);
	}

	public void selectFeaturesForPerLabel (InstanceList ilist)
	{
		RankedFeatureVector[] rankings = perLabelRanker.newRankedFeatureVectors (ilist);
		int numClasses = rankings.length;
		FeatureSelection[] fs = new FeatureSelection[numClasses];
		for (int i = 0; i < numClasses; i++) {
			fs[i] = new FeatureSelection (ilist.getDataAlphabet());
			RankedFeatureVector ranking = rankings[i];
			int nf = Math.min (numFeatures, ranking.singleSize());
			if (nf >= 0) {
				for (int j = 0; j < nf; j++)
					fs[i].add (ranking.getIndexAtRank(j));
			} else {
				for (int j = 0; j < ranking.singleSize(); j++) {
					if (ranking.getValueAtRank(j) > minThreshold)
						fs[i].add (ranking.getIndexAtRank(j));
					else
						break;
				}
			}
		}
		ilist.setFeatureSelection (null);
		ilist.setPerLabelFeatureSelection (fs);
	}
	
}
