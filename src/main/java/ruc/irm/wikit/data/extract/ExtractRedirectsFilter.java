package ruc.irm.wikit.data.extract;

import com.alibaba.fastjson.JSON;
import org.apache.commons.cli.*;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.data.dump.WikiPageDump;
import ruc.irm.wikit.data.dump.impl.PageXmlDump;
import ruc.irm.wikit.data.dump.parse.WikiPage;
import ruc.irm.wikit.data.dump.parse.WikiPageFilter;
import ruc.irm.wikit.util.Big5GB;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Extract Redirect information
 */
public class ExtractRedirectsFilter implements WikiPageFilter {
    private BufferedWriter redirectWriter = null;
    private Conf conf;

    public ExtractRedirectsFilter(Conf conf, File outFile) throws
            IOException {
        this.conf = conf;
        redirectWriter = new BufferedWriter(new FileWriter(outFile));
    }

    @Override
    public void process(WikiPage wikiPage, int index) {
        if (wikiPage.isArticle() && wikiPage.isRedirect()) {
            int id = wikiPage.getId();
            String title = wikiPage.getTitle();
            if (conf.isBig5ToGb()) {
                title = Big5GB.toGB(title);
            }
            try {
                String rt = wikiPage.getRedirect();
                if (conf.isBig5ToGb()) {
                    rt = Big5GB.toGB(rt);
                }

                if(!title.equalsIgnoreCase(rt))
                    redirectWriter.append(id + "\t" + title + "\t" + rt + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void close() throws IOException {
        redirectWriter.close();
    }

}