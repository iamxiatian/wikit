package ruc.irm.wikit.db.je.builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.data.dump.parse.WikiPage;
import ruc.irm.wikit.data.dump.parse.WikiPageFilter;
import ruc.irm.wikit.db.je.WDatabase;
import ruc.irm.wikit.db.je.WDatabase.DatabaseType;
import ruc.irm.wikit.db.je.WEnvironment;
import ruc.irm.wikit.db.je.struct.DbPage;

import java.io.IOException;

/**
 * @author Tian Xia
 * @date Jan 01, 2016 3:37 PM
 */
public class TitleDbFilter implements WikiPageFilter {
    private Conf conf = null;
    private static Logger LOG = LoggerFactory.getLogger(TitleDbFilter.class);

    private WDatabase<String, Integer> dbByTitle = null;
    private WEnvironment env = null;
    private boolean skipped = false;
    private DatabaseType type = null;

    public TitleDbFilter(WEnvironment env, DatabaseType type, boolean overwrite) {
        this.env = new WEnvironment(conf);

        if(type== DatabaseType.articlesByTitle){
            dbByTitle = env.getDbArticlesByTitle();
        } else if (type == DatabaseType.categoriesByTitle) {
            dbByTitle = env.getDbCategoriesByTitle();
        } else {
            skipped = true;
            return;
        }

        if (dbByTitle.exists() && !overwrite) {
            LOG.info("This db has already exists, skipped.");
            this.skipped = true;
        } else {
            //open database for writing
            dbByTitle.open(false);
        }
    }

    @Override
    public void process(WikiPage wikiPage, int index) {
        if (skipped) {
            return;
        }

        //save pages
        if (wikiPage.isArticle() && type ==  DatabaseType.articlesByTitle
                || wikiPage.isCategory() && type==DatabaseType.categoriesByTitle) {
            int id = wikiPage.getId();
            dbByTitle.put(wikiPage.getTitle().toLowerCase(), id);
        }
    }

    @Override
    public void close() throws IOException{

    }

}
