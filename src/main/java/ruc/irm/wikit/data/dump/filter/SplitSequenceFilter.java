package ruc.irm.wikit.data.dump.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.data.dump.parse.WikiPage;
import ruc.irm.wikit.data.dump.parse.WikiPageFilter;
import ruc.irm.wikit.util.ProgressCounter;

import java.io.*;
import java.util.zip.GZIPOutputStream;

/**
 * @author Tian Xia
 * @date Aug 07, 2015 11:20 PM
 */
public class SplitSequenceFilter implements WikiPageFilter {
    private static Logger LOG = LoggerFactory.getLogger(SplitSequenceFilter.class);
    private Conf conf = null;
    private DataOutputStream artOut = null;
    private DataOutputStream catOut = null;
    private ProgressCounter counter = new ProgressCounter();

    public SplitSequenceFilter(Conf conf) throws IOException {
        this.conf = conf;

        File articleFile = new File(conf.get("wiki.dump.seq.file.article",
                "seq-article.gz"));
        File categoryFile = new File(conf.get("wiki.dump.seq.file.category",
                "seq-category.gz"));

        if (!articleFile.getParentFile().exists()) {
            articleFile.getParentFile().mkdirs();
        }

        if (!categoryFile.getParentFile().exists()) {
            categoryFile.getParentFile().mkdirs();
        }

        if (articleFile.getAbsolutePath().endsWith(".gz")) {
            this.artOut = new DataOutputStream(new GZIPOutputStream(
                    new BufferedOutputStream(new FileOutputStream(articleFile))));
        } else {
            this.artOut = new DataOutputStream(new BufferedOutputStream(new
                    FileOutputStream(articleFile)));
        }

        if (categoryFile.getAbsolutePath().endsWith(".gz")) {
            this.catOut = new DataOutputStream(new GZIPOutputStream(
                    new BufferedOutputStream(new FileOutputStream(categoryFile))));
        } else {
            this.catOut = new DataOutputStream(
                    new BufferedOutputStream(new FileOutputStream(categoryFile)));
        }
    }

    @Override
    public void process(WikiPage wikiPage, int index) {
        try {
            counter.increment();
            if (wikiPage.isArticle()) {
                wikiPage.writeIn(artOut);
            } else if (wikiPage.isCategory()) {
                wikiPage.writeIn(catOut);
            } else {
                LOG.warn("Why? " + wikiPage);
            }
        } catch (IOException e) {
            LOG.error("split sequence file error:", e);
        }
    }

    @Override
    public void close() throws IOException {
        artOut.close();
        catOut.close();
        System.out.println();
    }
}
