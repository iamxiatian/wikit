package ruc.irm.wikit.esa.concept;

import ruc.irm.wikit.esa.concept.vector.ConceptIterator;
import ruc.irm.wikit.esa.concept.vector.ConceptVector;

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
        double sum = 0.0;
        ConceptIterator it0 = v0.iterator();
        while (it0.next()) {
            double value1 = v1.get(it0.getId());
            if (value1 > 0) {
                sum +=  it0.getValue() * value1;
            }
        }

        return sum / ( v0.getNorm2() *  v1.getNorm2());
    }

}
