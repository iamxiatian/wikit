package ruc.irm.wikit.esa.dataset;

import ruc.irm.wikit.esa.concept.domain.FullConcept;

import java.io.Closeable;
import java.io.IOException;

/**
 * User: xiatian
 * Date: 4/13/14
 * Time: 11:59 AM
 */
public interface DatasetVisitor extends Closeable {

    /**
     * 过滤当前concept，如果返回true，则会把concept传递到下一个Visitor继续访问，否则，后续的Visitor
     * 将不再访问当前的concept
     * @param concept
     * @return
     */
    public boolean filter(FullConcept concept);

    default public void close() throws IOException {

    }
}
