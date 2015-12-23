package ruc.irm.wikit.esa.dataset;

import ruc.irm.wikit.esa.concept.domain.FullConcept;
import ruc.irm.wikit.util.ProgressCounter;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

/**
 * Dataset that can be used for building ESA model
 *
 * @author Tian Xia <a href="mailto:xiat@ruc.edu.cn">xiat@ruc.edu.cn</a>
 * @date Apr 11, 2014 7:00 PM
 */
public interface ESADataset extends Closeable, Iterator<FullConcept> {
    /**
     * Open
     * @throws IOException
     */
    void open() throws IOException;

    boolean hasNext();

    FullConcept next();

    String name();

    default void traverse(DatasetVisitor... visitors) throws IOException {
        open();

        ProgressCounter counter = new ProgressCounter();
        while (hasNext()) {
            FullConcept concept = next();
            for (DatasetVisitor visitor : visitors) {
                if(!visitor.filter(concept))
                    break;
            }
            counter.increment();
        }

        for (DatasetVisitor v : visitors) {
            v.close();
        }

        close();
        System.out.printf("DONE:" + counter.getCount() + " items are visited.");
    }
}
