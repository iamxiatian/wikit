package ruc.irm.wikit.util.text.analysis;


import org.apache.lucene.search.similarities.DefaultSimilarity;

public class ESASimilarity extends DefaultSimilarity {

    private static final long serialVersionUID = 1L;

    @Override
    public float idf(long docFreq, long numDocs) {
        return (float) Math.log(numDocs / (double) docFreq);
    }

    @Override
    public float tf(float freq) {
        return (float) (1.0 + Math.log(freq));
    }

}
