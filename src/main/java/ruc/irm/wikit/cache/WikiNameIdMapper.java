package ruc.irm.wikit.cache;

import org.apache.commons.cli.*;
import ruc.irm.wikit.cache.impl.ArticleCacheRedisImpl;
import ruc.irm.wikit.cache.impl.CategoryCacheRedisImpl;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.common.exception.WikitException;
import ruc.irm.wikit.data.dump.WikiPageDump;
import ruc.irm.wikit.data.dump.impl.PageSequenceDump;
import ruc.irm.wikit.data.dump.impl.PageXmlDump;
import ruc.irm.wikit.data.dump.parse.WikiPage;
import ruc.irm.wikit.data.dump.parse.WikiPageFilter;

import java.io.File;
import java.io.IOException;

/**
 * Construct Name--Id mapping of Wikipedia articles and categories.
 *
 * @author Tian Xia
 * @date Jan 20, 2016 5:06 PM
 */
public class WikiNameIdMapper {
    private Conf conf = null;

    public WikiNameIdMapper(Conf conf) {
        this.conf = conf;
    }

    /**
     * Build all name-id maping for wiki cache, include category name-id
     * mapping and wiki page(article) name-id mapping
     */
    public void build(WikiPageDump dump) throws IOException {
        NameIdMapping categoryMapper = new CategoryCacheRedisImpl(conf);
        NameIdMapping articleMapper = new ArticleCacheRedisImpl(conf);

        dump.traverse(new WikiPageFilter() {
            @Override
            public void process(WikiPage wikiPage, int index) {
                if (wikiPage.isRedirect()) {
                    return; //skip redirect page
                }

                if (wikiPage.isArticle()) {
                    articleMapper.saveNameIdMapping(wikiPage.getTitle(),
                            wikiPage.getId());
                } else if (wikiPage.isCategory()) {
                    categoryMapper.saveNameIdMapping(wikiPage.getCategoryTitle(),
                            wikiPage.getId());
                }
            }


            @Override
            public void close() {
                categoryMapper.finishNameIdMapping();
                articleMapper.finishNameIdMapping();
            }

        });
    }



    public static void main(String[] args) throws ParseException, IOException, WikitException {
        String helpMsg = "usage: WikiNameIdMapper -c config.xml";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("c", true, "config file"));
        options.addOption(new Option("build", false, "Mapping Name-Id "));

        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("c")) {
            helpFormatter.printHelp(helpMsg, options);
            return;
        }

        Conf conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);
        WikiNameIdMapper mapper = new WikiNameIdMapper(conf);

        if (commandLine.hasOption("build")) {
            System.out.println("Start build name-id mapping relation...");
            WikiPageDump dump = new PageXmlDump(conf);
            mapper.build(dump);

            System.out.println("Done!");
        }
    }
}
