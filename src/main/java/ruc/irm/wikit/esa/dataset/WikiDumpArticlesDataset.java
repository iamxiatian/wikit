package ruc.irm.wikit.esa.dataset;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.data.dump.parse.WikiPage;
import ruc.irm.wikit.data.dump.parse.WikiPageReader;
import ruc.irm.wikit.esa.concept.domain.FullConcept;

import javax.xml.stream.XMLStreamException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class WikiDumpArticlesDataset implements ESADataset {
    private static Logger LOG = LoggerFactory.getLogger
            (WikiDumpArticlesDataset.class);

    private String dumpFile = null;
    private WikiPageReader reader = null;
    private int count = 0;
    private Conf conf = null;

    public WikiDumpArticlesDataset(Conf conf, String dumpFile) {
        this.conf = conf;
        this.dumpFile = dumpFile;
    }

    @Override
    public void open() throws IOException {
        InputStream stream = new FileInputStream(dumpFile);
        if (dumpFile.endsWith(".bz2")) {
            boolean multiStream = dumpFile.contains("multistream");
            stream = new BZip2CompressorInputStream(stream, multiStream);
        }
        reader = new WikiPageReader(conf, stream);

        this.count = 0;
    }

    private WikiPage page = null;

    @Override
    public boolean hasNext() {
        try {
            while (reader.hasMoreWikiPage()) {
                page = reader.nextWikiPage();
                if (page.isArticle() && !page.isRedirect()) {
                    return true;
                }
            }
            return false;
        } catch (XMLStreamException e) {
            LOG.error("dump article data set", e);
            return false;
        }
    }

    @Override
    public FullConcept next() {
        count++;
        FullConcept c = new FullConcept();
        c.setId(count);
        c.setTitle(page.getTitle());
        c.setPlainContent(page.getPlainText());
        c.setRawContent(page.getText());
        c.setOutLinks(page.getInternalLinks());
        c.setOutId(Integer.toString(page.getId()));
        c.setCategories(page.getCategories());
        return c;
    }

    @Override
    public String name() {
        return "Wiki Dataset:" + dumpFile;
    }


    @Override
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }
}
