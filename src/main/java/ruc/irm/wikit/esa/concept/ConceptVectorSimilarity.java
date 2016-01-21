package ruc.irm.wikit.esa.concept;

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import ruc.irm.wikit.esa.concept.vector.ConceptIterator;
import ruc.irm.wikit.esa.concept.vector.ConceptVector;
import ruc.irm.wikit.util.SimUtils;

public class ConceptVectorSimilarity {
    public ConceptVectorSimilarity() {

    }

    /**
     * cosine similarity
     * @param v0
     * @param v1
     * @return
     */
    public double calcSimilarity(ConceptVector v0, ConceptVector v1) {
//        double sum = 0.0;
//        ConceptIterator it0 = v0.iterator();
//        while (it0.next()) {
//            double value1 = v1.get(it0.getId());
//            if (value1 > 0) {
//                sum +=  it0.getValue() * value1;
//            }
//        }
//
//        return sum / ( v0.getNorm2() *  v1.getNorm2());
        TIntDoubleMap X = new TIntDoubleHashMap();
        TIntDoubleMap Y = new TIntDoubleHashMap();
        ConceptIterator it = v0.iterator();
        while (it.next()) {
            X.put(it.getId(), it.getValue());
        }
        it = v1.iterator();
        while (it.next()) {
            Y.put(it.getId(), it.getValue());
        }
        return SimUtils.cosineSimilarity(X, Y);
    }

}
