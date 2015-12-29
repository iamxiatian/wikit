package ruc.irm.wikit.data.dump.filter;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.db.MongoClient;
import ruc.irm.wikit.data.dump.parse.WikiPage;
import ruc.irm.wikit.data.dump.parse.WikiPageFilter;
import ruc.irm.wikit.util.GZipUtils;

/**
 * Dump Wiki Page to MongoDB, it's more convenient for human manually analysis when store the page content into mongodb.
 */
public class PageToMongoFilter implements WikiPageFilter {
    private MongoCollection<Document> collection = null;
    private Conf conf = null;

    public PageToMongoFilter(Conf conf, String collectionName) {
        this.conf = conf;
        this.collection = MongoClient.getCollection(conf, collectionName);
        this.collection.drop();
    }

    @Override
    public void process(WikiPage wikiPage, int index) {
        //only category and common articles are saved into mongodb
        if(wikiPage.isCategory() || wikiPage.isArticle()) {
            Document record = new Document();
            record.put("id", wikiPage.getId());
            record.put("ns", wikiPage.getNs());
            record.put("title", wikiPage.getTitle());
            record.put("text", GZipUtils.gzip(wikiPage.getText(), "utf-8"));
            record.put("format", wikiPage.getFormat());
            record.put("redirect", wikiPage.getRedirect());
            record.put("links", wikiPage.getInternalLinks());
            collection.insertOne(record);
        }
    }
    
    public void close() {
        collection.createIndex(new Document("id", 1));
    }
}
