package ruc.irm.wikit.data.dump;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.data.dump.parse.WikiPage;
import ruc.irm.wikit.data.dump.parse.WikiPageFilter;
import ruc.irm.wikit.util.ProgressCounter;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

/**
 * Wiki Page dump processor, there are 2 ways to visit the dump content:
 *
 * The first one is accessed by traverse as follows:
 * <pre>
 * WikiPageDump dump = new OneWikiPageDumpImpl();
 * dump.traverse(filters);
 * </pre>
 *
 * The second one is used by iterator:
 * <pre>
 *     WikiPageDump dump = new OneWikiPageDumpImpl();
 *     dump.open();
 *     while(dump.hasNext()){
 *         WikiPage page = dump.next();
 *         //do some process
 *     }
 *     dump.close();
 * </pre>
 * @author Tian Xia
 * @date Jul 31, 2015 7:46 PM
 */
public abstract class WikiPageDump implements Iterator<WikiPage>, Closeable {
    protected Logger LOG = LoggerFactory.getLogger(this.getClass());

    protected Conf conf = null;
    protected String dumpFile = null;

    /**
     * Get dump file name for this dump object, such as seq.gz, 20150702
     * .page-article.gz
     * @return
     */
    public final String getDumpName(){
        return dumpFile;
    }

    public abstract void open() throws IOException;

    public void reset() throws IOException {
        close();
        open();
    }

    /**
     * Traverse page dump file with filters
     *
     * @param filters
     */
    public final void traverse(WikiPageFilter... filters) throws IOException{
        LOG.info("Use " + this.getClass().getSimpleName() + " to traverse " +
                dumpFile);

        open();
        int index = 0;
        ProgressCounter counter = new ProgressCounter();
        while (hasNext()) {
            WikiPage page = next();
            for (WikiPageFilter filter : filters) {
                filter.process(page, index);
            }
            index++;
            counter.increment();
        }

        for (WikiPageFilter filter : filters) {
            filter.close();
        }

        close();

        LOG.info("Done for traverse " + dumpFile + ", "+ index + " pages has " +
                        "been visited.");
    };
}
