package ruc.irm.wikit.esa.dataset;

import ruc.irm.wikit.data.dump.WikiPageDump;
import ruc.irm.wikit.data.dump.parse.WikiPage;
import ruc.irm.wikit.esa.concept.domain.FullConcept;

import java.io.IOException;

public class WikiDumpDataset implements ESADataset {

    private WikiPageDump dump = null;
    private int count = 0;
    private WikiPage page = null;

    public WikiDumpDataset(WikiPageDump dump) {
        this.dump = dump;
    }

    @Override
    public void open() throws IOException {
        this.count = 0;
        this.dump.open();
    }


    @Override
    public boolean hasNext() {
        return dump.hasNext();
    }

    @Override
    public FullConcept next() {
        WikiPage page = dump.next();

        count++;
        FullConcept c = new FullConcept();
        c.setId(count);
        c.setTitle(page.getTitle());
        c.setPlainContent(page.getPlainText());
        c.setRawContent(page.getText());
        c.setOutLinks(page.getInternalLinks());
        c.setOutId(Integer.toString(page.getId()));
        c.setCategories(page.getCategories());
        c.setAliasNames(page.getAliases());

        return c;
    }

    @Override
    public String name() {
        return "Dataset:" + dump.getDumpName();
    }

    @Override
    public void close() throws IOException {
        if(dump!=null)
            dump.close();
    }
}
