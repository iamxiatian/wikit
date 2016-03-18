package ruc.irm.wikit.nlp.libsvm.kernel;

import ruc.irm.wikit.nlp.libsvm.common.SparseVector;
import ruc.irm.wikit.nlp.libsvm.svm_node;

import java.io.Serializable;

/**
 *  <code>LinearKernel</code> implements a linear kernel function.
 * @author Syeed Ibn Faiz
 */
public class LinearKernel implements CustomKernel, Serializable {

    @Override
    public double evaluate(svm_node x, svm_node y) {
        if (!(x.data instanceof SparseVector) || !(y.data instanceof SparseVector)) {
            throw new RuntimeException("Could not find sparse vectors in svm_nodes");
        }        
        SparseVector v1 = (SparseVector) x.data;
        SparseVector v2 = (SparseVector) y.data;
                        
        return v1.dot(v2);
    }    
}
