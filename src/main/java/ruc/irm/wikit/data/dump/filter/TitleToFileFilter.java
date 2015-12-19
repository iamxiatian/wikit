package ruc.irm.wikit.data.dump.filter;

import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.data.dump.parse.WikiPage;
import ruc.irm.wikit.data.dump.parse.WikiPageFilter;
import ruc.irm.wikit.util.Big5GB;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class TitleToFileFilter implements WikiPageFilter {
    private BufferedWriter writer = null;
    private Conf conf;

    public TitleToFileFilter(Conf conf, File output) throws IOException {
        this.conf = conf;
        writer = new BufferedWriter(new FileWriter(output));
    }

    @Override
    public void process(WikiPage wikiPage, int index) {
        if (wikiPage.isArticle()) {
            String title = wikiPage.getTitle();
            if (title.length() < 5) {
                try {
                    if (conf.isBig5ToGb()) {
                        writer.write(Big5GB.toGB(title) + "\n");
                    } else {
                        writer.write(title + "\n");
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void close() throws IOException {
        writer.close();
    }
}