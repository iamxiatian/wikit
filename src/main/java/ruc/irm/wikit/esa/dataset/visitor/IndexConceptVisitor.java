package ruc.irm.wikit.esa.dataset.visitor;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.esa.concept.domain.FullConcept;
import ruc.irm.wikit.esa.dataset.DatasetVisitor;
import ruc.irm.wikit.util.text.analysis.ESAAnalyzer;
import ruc.irm.wikit.util.text.analysis.ESASimilarity;

import java.io.File;
import java.io.IOException;

/**
 * Index all the concept through lucene full text search engine.
 *
 * User: xiatian
 * Date: 4/13/14
 * Time: 12:08 PM
 */
public class IndexConceptVisitor implements DatasetVisitor {
    private IndexWriter writer = null;

    public IndexConceptVisitor(Conf conf, boolean create) throws IOException {
        writer = openWriter(conf, create);
    }

    private IndexWriter openWriter(Conf conf, boolean create) throws IOException {
        File indexDir = new File(conf.getEsaIndexDir());
        indexDir.mkdirs();
        System.out.println("Create index on " + indexDir.getAbsolutePath());
        Directory fsDirectory = FSDirectory.open(indexDir);

        IndexWriterConfig config = new IndexWriterConfig(Conf.LUCENE_VERSION, new ESAAnalyzer(conf));
        config.setSimilarity(new ESASimilarity());
        if (create) {
            // Create a new index in the directory, removing any
            // previously indexed documents:
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        } else {
            // Add new documents to an existing index:
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        }
        return new IndexWriter(fsDirectory, config);
    }

    @Override
    public boolean filter(FullConcept concept) {
        //index
        Document doc = new Document();

        /** The customized field type for contents field */
        FieldType contentFieldType = new FieldType();
        contentFieldType.setIndexed(true);
        contentFieldType.setStored(true);
        contentFieldType.setStoreTermVectors(true);
        contentFieldType.setTokenized(true);

        doc.add(new Field("contents", concept.getTitle() + "\n" + concept.getPlainContent(), contentFieldType));
        doc.add(new StringField("id", Integer.toString(concept.getId()), Field.Store.YES));
        doc.add(new StringField("outId", concept.getOutId(), Field.Store.YES));
        doc.add(new Field("title", concept.getTitle(), contentFieldType));

        try {
            writer.addDocument(doc);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
