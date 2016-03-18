package ruc.irm.wikit.nlp.libsvm.kernel;


import ruc.irm.wikit.nlp.libsvm.svm_node;

/**
 * Interface for a custom kernel function
 * @author Syeed Ibn Faiz
 */
public interface CustomKernel {
    double evaluate(svm_node x, svm_node y);
}
