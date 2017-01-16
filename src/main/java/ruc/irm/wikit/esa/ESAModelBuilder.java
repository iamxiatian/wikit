package ruc.irm.wikit.esa;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.data.dump.impl.PageSequenceDump;
import ruc.irm.wikit.esa.concept.ConceptCacheRedisImpl;
import ruc.irm.wikit.esa.concept.domain.FullConcept;
import ruc.irm.wikit.esa.dataset.DatasetVisitor;
import ruc.irm.wikit.esa.dataset.ESADataset;
import ruc.irm.wikit.esa.dataset.WikiDumpDataset;
import ruc.irm.wikit.esa.dataset.visitor.CacheConceptVisitor;
import ruc.irm.wikit.esa.dataset.visitor.IndexConceptVisitor;
import ruc.irm.wikit.esa.index.IndexMining;
import ruc.irm.wikit.util.ProgressCounter;

import java.io.IOException;

/**
 * Build ESA Model
 *
 * @author Tian Xia <a href="mailto:xiat@ruc.edu.cn">xiat@ruc.edu.cn</a>
 * @date Jul 31, 2015 7:46 PM
 */
public class ESAModelBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(ESAModelBuilder.class);
    private ProgressCounter progress = new ProgressCounter();

    public void buildModel(Conf conf, ESADataset dataset) throws IOException {
        final ConceptCacheRedisImpl conceptCache = new ConceptCacheRedisImpl(conf);

        conceptCache.clearAll();

        LOG.warn("Build concept name--id mapping and index all concepts...");
        DatasetVisitor nameIdMappingVisitor = new DatasetVisitor() {
            int maxId = 0;
            @Override
            public boolean filter(FullConcept concept) {
                conceptCache.saveNameIdMapping(concept.getTitle(), concept.getId());
                this.maxId = concept.getId();

                return true;
            }

            @Override
            public void close() throws IOException {
                conceptCache.saveMaxConceptId(maxId);
                conceptCache.finishNameIdMapping();
            }
        };
        dataset.traverse(nameIdMappingVisitor,
                        new IndexConceptVisitor(conf, true));

        if(!conceptCache.nameIdMapped()){
            throw new IOException("Make Name-Id mapping first before cache full " +
                    "concept.");
        }

        LOG.warn("Build full concept cache...");
        dataset.traverse(new CacheConceptVisitor(conceptCache));


        LOG.warn("Make global idf and tf-idf...");
        IndexMining mining = new IndexMining(conf);
        mining.open();
        mining.modify();
        mining.close();

        LOG.warn("Import IDF into redis...");
        String termsIdfOutputFile = conf.getWikiTermsIdfFile();
        conceptCache.importIdf(termsIdfOutputFile);

        LOG.warn("Import TF-IDF into redis...");
        String tfidfOutputDataFile = conf.getWikiTfidfFile();
        conceptCache.importTfIdf(tfidfOutputDataFile);

        conceptCache.close();
        LOG.warn("ALL DONE!");
    }

    public static void main(String[] args) throws IOException, ParseException {
        String helpMsg = "usage: ESAModelBuilder -c config.xml";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("c", true, "config file"));
        options.addOption(new Option("build", false, "Build ESA model."));
        options.addOption(new Option("tfidf", true, "import TF-IDF,specify tfidf filename."));
        options.addOption(new Option("idf", true, "import IDF,specify idf filename."));

        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("c")) {
            LOG.warn("Please specify parameters.");
            helpFormatter.printHelp(helpMsg, options);
            return;
        }

        Conf conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);
        ESAModelBuilder builder = new ESAModelBuilder();

        if (commandLine.hasOption("build")) {
            LOG.info("Start build ESA model...");
            ESADataset dataset = new WikiDumpDataset(new PageSequenceDump(conf,
                    conf.get("wiki.dump.seq.file.article")));
            System.out.println("dataset==>" + dataset.name());
            builder.buildModel(conf, dataset);
            LOG.info("DONE for build ESA model!");
        } else if(commandLine.hasOption("idf")) {
            String idfFile = commandLine.getOptionValue("idf");
            LOG.info("import idf from file " + idfFile);
            ConceptCacheRedisImpl conceptCache = new ConceptCacheRedisImpl(conf);
            conceptCache.importIdf(idfFile);
            LOG.info("DONE for importing idf.");
        } else if(commandLine.hasOption("tfidf")) {
            String tfidfFile = commandLine.getOptionValue("tfidf");
            LOG.info("import tf-idf from file "+tfidfFile);
            ConceptCacheRedisImpl conceptCache = new ConceptCacheRedisImpl(conf);
            conceptCache.importTfIdf(tfidfFile);
            LOG.info("DONE for importing tf-idf.");
        }
    }
}
