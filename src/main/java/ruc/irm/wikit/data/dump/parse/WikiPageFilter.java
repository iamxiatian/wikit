package ruc.irm.wikit.data.dump.parse;

import java.io.Closeable;
import java.io.IOException;

public interface WikiPageFilter extends Closeable {
    /**
     * process the #index wiki page
     * @param wikiPage
     * @param index
     */
    public void process(WikiPage wikiPage, final int index);

    public default void close() throws IOException {

    };
}
