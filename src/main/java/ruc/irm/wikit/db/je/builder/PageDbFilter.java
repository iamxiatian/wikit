package ruc.irm.wikit.db.je.builder;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.data.dump.WikiPageDump;
import ruc.irm.wikit.data.dump.parse.WikiPage;
import ruc.irm.wikit.data.dump.parse.WikiPageFilter;
import ruc.irm.wikit.db.je.WDatabase;
import ruc.irm.wikit.db.je.WEnvironment;
import ruc.irm.wikit.db.je.struct.DbPage;

import java.io.IOException;

/**
 * @author Tian Xia
 * @date Jan 01, 2016 3:37 PM
 */
public class PageDbFilter implements WikiPageFilter {
    private Conf conf = null;
    private static Logger LOG = LoggerFactory.getLogger(PageDbFilter.class);

    private WDatabase<Integer, DbPage> dbPage = null;
    private boolean overwrite = false;
    private WEnvironment env = null;
    private boolean skipped = false;

    public PageDbFilter(WEnvironment env, boolean overwrite) {
        this.env = new WEnvironment(conf);
        this.overwrite = overwrite;

        dbPage = env.getDbPage();

        if (dbPage.exists() && !overwrite) {
            LOG.info("page db has already exists, skipped.");
            this.skipped = true;
        } else {
            //open database for writing
            dbPage.open(false);
        }
    }

    @Override
    public void process(WikiPage wikiPage, int index) {
        if (skipped) {
            return;
        }

        //save pages
        if (wikiPage.isArticle() || wikiPage.isCategory()) {
            int ns = Integer.parseInt(wikiPage.getNs());
            int id = wikiPage.getId();
            DbPage pageObject = new DbPage(id, wikiPage.getTitle(),
                    ns, wikiPage.getText());
            dbPage.put(id, pageObject);
        }
    }

    @Override
    public void close() throws IOException{

    }

}
