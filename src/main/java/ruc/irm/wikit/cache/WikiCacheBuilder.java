package ruc.irm.wikit.cache;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.common.exception.MissedException;
import ruc.irm.wikit.common.exception.WikitException;
import ruc.irm.wikit.cache.impl.ArticleCacheRedisImpl;
import ruc.irm.wikit.cache.impl.CategoryCacheRedisImpl;
import ruc.irm.wikit.data.dump.WikiPageDump;
import ruc.irm.wikit.data.dump.impl.PageSequenceDump;
import ruc.irm.wikit.data.dump.parse.WikiPage;
import ruc.irm.wikit.data.dump.parse.WikiPageFilter;
import ruc.irm.wikit.util.ProgressCounter;

import java.io.IOException;
import java.util.Collection;

/**
 * <p>Building cache which is related with wiki category</p>
 *
 *
 * User: xiatian
 * Date: 7/15/14
 * Time: 11:08 PM
 */
@Deprecated
public class WikiCacheBuilder {
    private static Logger LOG = LoggerFactory.getLogger(WikiCacheBuilder.class);

    private Conf conf = null;

    public WikiCacheBuilder(Conf conf) {
        this.conf = conf;
    }


    /**
     * Build cache for article and/or category
     */
    public void buildCache(final boolean createArticleCache,
                                    final boolean createCategoryCache)
            throws WikitException, IOException {
        WikiPageDump dump = new PageSequenceDump(conf);
        final ArticleCache artCache = new ArticleCacheRedisImpl(conf);
        final CategoryCache catCache = new CategoryCacheRedisImpl(conf);
        if(!artCache.nameIdMapped() || !catCache.nameIdMapped()) {
            throw new WikitException("Name-ID mapping does not created, See " +
                    "WikiCacheMaker.buildNameIdMapping()");
        }

        System.out.println("Start to build cache, article:" +
                createArticleCache + ", category:" + createCategoryCache);
        dump.traverse(new WikiPageFilter() {
            private ProgressCounter counter = new ProgressCounter();

            private void buildArticleCache(WikiPage wikiPage) throws MissedException {
                if (wikiPage.isRedirect() && wikiPage.isArticle()) {
                    int toId = artCache.getIdByName(wikiPage.getRedirect(), -1);
                    if (toId>0) {
                        artCache.saveAlias(toId, wikiPage.getTitle());
                    }
                    return;
                }

                if (!wikiPage.isArticle()) {
                    return;
                }

                int pageId = artCache.getIdByName(wikiPage.getTitle(), -1);
                if(pageId<0) return;

                Collection<String> categories = wikiPage.getCategories();
                artCache.saveCategories(pageId, wikiPage.getCategories());

                //保存wikiPage自身已经识别出的别名
                artCache.saveAlias(pageId, wikiPage.getAliases());
            }

            private void buildCategoryCache(WikiPage wikiPage) throws MissedException {
                if (wikiPage.isRedirect()) {
                    return;
                }

                if (wikiPage.isArticle()) {
                    for (String c : wikiPage.getCategories()) {
                        if (catCache.nameExist(c)) {
                            int catId = catCache.getIdByName(c);
                            catCache.addArticleRelation(catId, wikiPage.getId());
                            catCache.incArticleCount(catId);
                        }
                    }
                } else if (wikiPage.isCategory()) {
                    if (!wikiPage.isCommonCategory()) return;

                    String title = wikiPage.getCategoryTitle();
                     //DO NOT use wikiPage.getId() because only one id is
                     // kept in redis for the same title categories.
                    int catId = catCache.getIdByName(title, -1);
                    if (catId < 0) return;

                    Collection<String> parents = wikiPage.getCategories();
                    for (String p : parents) {
                        int pid = catCache.getIdByName(p, -1);
                        if (pid>0) {
                            catCache.saveChildren(pid, catId);
                        }
                    }
                    catCache.saveParents(catId, parents);
                }
            }

            @Override
            public void process(WikiPage wikiPage, int index) {
                if (!wikiPage.isCategory() && !wikiPage.isArticle()) {
                    return;
                }

                try {
                    wikiPage.drillMoreInfo();

                    if(createArticleCache) {
                        buildArticleCache(wikiPage);
                    }

                    if(createCategoryCache && !wikiPage.isRedirect()) {
                       buildCategoryCache(wikiPage);
                    }
                } catch (Exception e) {
                    System.out.println(wikiPage);
                    e.printStackTrace();
                    LOG.warn(e.toString());
                }
                counter.increment();
            }

            @Override
            public void close() {
                if(createArticleCache) artCache.done();
                if(createCategoryCache) catCache.done();
            }
        });
    }

    public static void main(String[] args) throws ParseException, IOException, WikitException {
        String helpMsg = "usage: WikiCacheBuilder -c config.xml";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("c", true, "config file"));
        options.addOption(new Option("mapNameId", false, "Mapping Name-Id "));
        options.addOption(new Option("bac", false, "Build article cache"));
        options.addOption(new Option("bcc", false, "Build category cache"));
        options.addOption(new Option("ppc", false, "Post process for " +
                "category cache"));
        options.addOption(new Option("ppa", false, "Post process for " +
                "article cache"));

        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("c")) {
            LOG.warn("Please specify parameters.");
            helpFormatter.printHelp(helpMsg, options);
            return;
        }

        Conf conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);
        WikiCacheBuilder maker = new WikiCacheBuilder(conf);

        if(commandLine.hasOption("bac") && commandLine.hasOption("bcc")){
            maker.buildCache(true, true);
            LOG.warn("DONE for build article and category cache!");
        } else if (commandLine.hasOption("bac")) {
            maker.buildCache(true, false);
            LOG.warn("DONE for build article cache!");
        } else if (commandLine.hasOption("bcc")) {
            maker.buildCache(false, true);
            LOG.warn("DONE for build category cache!");
        }

        if (commandLine.hasOption("ppc")) {
            CategoryCache catCache = new CategoryCacheRedisImpl(conf);
            catCache.assignDepth();
        }
    }
}
