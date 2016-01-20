package ruc.irm.wikit.data.dump.filter;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.data.dump.parse.WikiPage;
import ruc.irm.wikit.data.dump.parse.WikiPageFilter;

import java.io.*;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

/**
 * In order to remove unimportant pages and speed up further process, we use
 * two phase process. This is the second phase, all pages which match one of the
 * following conditions will be removed:
 * <p>
 * <ul>
 * <li>inlink and outlink count is less than 20</li>
 * </ul>
 * <p>
 * In order to collect in-link numbers for second phase usage, we use Redis
 * as a temporary cache, and store all &lt;page title, in-link count&gt; pairs
 *
 * <br/>
 * <strong>Remember:</strong> category pages are also saved to the output file.
 */
public class SecondStopFilter implements WikiPageFilter {
    private Logger LOG = LoggerFactory.getLogger(SecondStopFilter.class);

    private Conf conf = null;
    private Jedis jedis = null;

    private int totalPages = 0;
    private int totalArticles = 0;
    private int totalCategories = 0;

    private int normalArticles = 0;
    private int redirectArticles = 0;
    private int normalCategories = 0;
    private int removedByLinks = 0;

    private DataOutputStream out = null;

    /**
     * minimum links for valid wiki page
     */
    private int minLinks = 10;

    public SecondStopFilter(Conf conf) throws IOException {
        this.conf = conf;

        this.minLinks = conf.getInt("wiki.stop.filter.min.links", 10);
        File gzSeqFile = new File(conf.get("wiki.dump.file.seq2", "seq2.gz"));
        if (!gzSeqFile.getParentFile().exists()) {
            gzSeqFile.getParentFile().mkdirs();
        }

        this.out = new DataOutputStream(new GZIPOutputStream(
                new BufferedOutputStream(new FileOutputStream(gzSeqFile))));

        this.jedis = new Jedis(conf.getRedisHost(), conf.getRedisPort(),
                conf.getRedisTimeout());
    }

    @Override
    public void process(WikiPage wikiPage, int index) {
        totalPages++;

        try {
            if (wikiPage.isArticle()) {
                totalArticles++;

                if (StringUtils.isEmpty(wikiPage.getTitle())) {
                    return;
                }

                if (wikiPage.isRedirect()) {
                    //由于FirstStopFilter中已经过滤了跳转类词条，所以应该不会执行到下面的两条语句
                    redirectArticles++;
                    wikiPage.writeIn(out);
                } else {
                    //所指向词条的入链数量
                    String key = "tmp:article:inlinks";
                    String value = jedis.hget(key, wikiPage.getTitle().toLowerCase());
                    int inlinks = NumberUtils.toInt(value, 0);
                    int outlinks = wikiPage.getInternalLinks().size();

                    if ((inlinks + outlinks) >= minLinks) {
//                    if(inlinks>=minLinks && outlinks>=minLinks) {
                        normalArticles++;
                        wikiPage.setInlinkCount(inlinks);

                        //写入alias信息
                        String target = wikiPage.getTitle().toLowerCase();

                        if (jedis.hexists("tmp:article:alias", target)) {
                            String aliasesString = jedis.hget("tmp:article:alias",
                                    target);
                            Set<String> aliasSet = Sets.newHashSet(Splitter.on('\n')
                                    .omitEmptyStrings()
                                    .trimResults()
                                    .splitToList(aliasesString));
                            wikiPage.setAliases(aliasSet);
                        }

                        wikiPage.writeIn(out);
                    } else {
                        removedByLinks++;
                    }
                }
            } else if (wikiPage.isCategory()) {
                //记录所隶属分类的词条数量
                String key = "tmp:cat:articles";
                String value = jedis.hget(key, wikiPage.getCategoryTitle().toLowerCase());
                wikiPage.setInlinkCount(NumberUtils.toInt(value, 0));
                wikiPage.writeIn(out);
                totalCategories++;
                normalCategories++;
            }
        } catch (IOException e) {
            e.printStackTrace();
            LOG.error(e.toString());
        }

        if (totalPages % 10000 == 0) {
            System.out.println("Parsed " + totalPages + " pages.");
        }
    }

    public void close() throws IOException {
        String key = "tmp:summary:stop2";
        jedis.hset(key, "totalPages", Integer.toString(totalPages));
        jedis.hset(key, "totalArticles", Integer.toString(totalArticles));
        jedis.hset(key, "normalArticles", Integer.toString(normalArticles));
        jedis.hset(key, "redirectArticles", Integer.toString(redirectArticles));
        jedis.hset(key, "totalCategories", Integer.toString(totalCategories));
        jedis.hset(key, "normalCategories", Integer.toString(normalCategories));
        jedis.hset(key, "removedByLinks", Integer.toString(removedByLinks));

        jedis.hset(key, "minLinkThreshold", Integer.toString(minLinks));
        out.close();
    }
}