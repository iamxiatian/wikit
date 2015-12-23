package ruc.irm.wikit.data.dump.filter;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.db.MongoClient;
import ruc.irm.wikit.data.dump.parse.WikiPage;
import ruc.irm.wikit.data.dump.parse.WikiPageFilter;

import java.util.Collection;

/**
 * Traverse articles, and save category info into mongodb.
 * for category tree:
 * pid, title, parent
 */
public class DumpCategoryIntoMongoFilter implements WikiPageFilter {
    private int count = 0;
    private MongoCollection<Document> catCollection = null;
    private MongoCollection<Document> pageCollection = null;

    public DumpCategoryIntoMongoFilter(Conf conf, String catCollectionName, String pageCollectionName) {
        this.catCollection = MongoClient.getCollection(conf, catCollectionName);
        this.pageCollection = MongoClient.getCollection(conf, pageCollectionName);
    }

    @Override
    public void process(WikiPage wikiPage, int index) {
        if (wikiPage.isRedirect()) return;

        if (wikiPage.isArticle()) {
            Collection<String> categories = wikiPage.getCategories();
            for (String c : categories) {
                Document record = new Document();
                record.append("pid", wikiPage.getId())
                        .append("title", wikiPage.getTitle())
                        .append("cat", c);
                pageCollection.insertOne(record);
            }
        } else if (wikiPage.isCategory()) {
            if (!wikiPage.isCommonCategory()) return;

            String title = wikiPage.getTitle().substring("Category:".length());
            Collection<String> categories = wikiPage.getCategories();
            if (categories == null || categories.size() == 0) {
                Document record = new Document();
                record.append("pid", wikiPage.getId())
                        .append("title", title)
                        .append("parent", "ROOT");
                catCollection.insertOne(record);
            } else {
                for (String c : categories) {
                    Document record = new Document();
                    record.append("pid", wikiPage.getId())
                            .append("title", title)
                            .append("parent", c);
                    catCollection.insertOne(record);
                }
            }
        }
        if (++count % 500 == 0) {
            System.out.println(count);
        }
    }

    public void close() {
        catCollection.createIndex(new Document("pid", 1),
                new IndexOptions().unique(false));
        catCollection.createIndex(new Document("title", 1),
                new IndexOptions().unique(false));
        catCollection.createIndex(new Document("parent", 1),
                new IndexOptions().unique(false));
        pageCollection.createIndex(new Document("cat", 1),
                new IndexOptions().unique(false));
    }
}
