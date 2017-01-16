package ruc.irm.wikit.data.dump.filter;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.Jedis;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.data.dump.parse.WikiPage;
import ruc.irm.wikit.data.dump.parse.WikiPageFilter;
import ruc.irm.wikit.util.text.analysis.ESAAnalyzer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

/**
 * In order to remove unimportant pages and speed up further process, we use
 * two phase process. This is the first phase, all pages which match one of the
 * following conditions will be removed:
 * <p>
 * <ul>
 * <li>fewer than 100 non-stop words</li>
 * <li>concept which contains date string, such as "2010 China Open"</li>
 * </ul>
 * <p>
 * In order to collect in-link numbers for second phase usage, we use Redis
 * as a temporary cache, and store all &lt;page title, in-link count&gt; pairs
 */
public class FirstStopFilter implements WikiPageFilter {
    private Conf conf = null;
    private Jedis jedis = null;

    private int totalPages = 0;
    private int totalArticles = 0;
    private int totalCategories = 0;

    private int normalArticles = 0;
    private int redirectArticles = 0;
    private int normalCategories = 0;

    private ESAAnalyzer analyzer = null;

    private DataOutputStream out = null;

    /**
     * minimum non-stop words for valid wiki page
     */
    private int minWords = 50;

    public FirstStopFilter(Conf conf) throws IOException {
        this.conf = conf;
        analyzer = new ESAAnalyzer(conf);
        this.minWords = conf.getInt("wiki.stop.filter.min.words", 50);

        File gzSeqFile = new File(conf.get("wiki.dump.file.seq1", "seq1.gz"));
        if (!gzSeqFile.getParentFile().exists()) {
            gzSeqFile.getParentFile().mkdirs();
        }

        this.out = new DataOutputStream(new GZIPOutputStream(
                new BufferedOutputStream(new FileOutputStream(gzSeqFile))));

        this.jedis = new Jedis(conf.getRedisHost(), conf.getRedisPort(),
                conf.getRedisTimeout());
    }

    private boolean accept(WikiPage wikiPage) throws IOException {
        //step 1: if title starts with 4 digits, then skip
        String title = wikiPage.getTitle().toLowerCase();
        if (wikiPage.isCategory()) {
            title = wikiPage.getCategoryTitle().toLowerCase();
        }
        if (title.length() > 7) {
            //保留1980s此类词条
            String startString = title.substring(0, 4);
            if (StringUtils.isNumeric(startString)) {
                return false;
            }
        }

        //step 2: remove "list of xxxx" and "index of xxx"
        if (title.indexOf("index of ") >= 0 || title.indexOf("list of") >= 0
                || title.indexOf("(disambiguation)")>=0) {
            return false;
        }

        //以年份结尾的词条，符合年份时代结尾的形式文章，如``China national football team results (2000–09)''，因为这类文章的作用更类似于类别，起到信息组织的作用。
        Pattern pattern = Pattern.compile("\\(\\d{4}(–|\\-)\\d{2,4}\\)$");
        if (pattern.matcher(title).find()) {
            return false;
        }

        //去除类别为Disambiguation pages的词条
        Set<String> categories = wikiPage.getCategories();
        if (categories.size() < 3) {
            for (String c : categories) {
                if(c.equalsIgnoreCase("Disambiguation pages")){
                    return false;
                }
            }
        }

        return wikiPage.getPlainText().length()>minWords;

//        //step 2: check token numbers
//        int tokens = 0;
//        TokenStream tokenStream = analyzer.tokenStream("contents", new
//                StringReader(wikiPage.getPlainText()));
//        //tokenStream.reset();
//        while (tokenStream.incrementToken()) {
//            tokens++;
//            if (tokens > minWords) break;
//        }
//
//        tokenStream.end();
//        tokenStream.close();
//
//        return tokens>=minWords;
    }

    @Override
    public void process(WikiPage wikiPage, final int index) {
        totalPages++;

        wikiPage.drillMoreInfo();

        //only common category and articles are saved
        if (wikiPage.isArticle()) {
            totalArticles++;
            try {
                if (wikiPage.isRedirect()) { //keep all redirect article pages
                    redirectArticles++;
                    //wikiPage.writeIn(out);

                    //store redirect info to redis
                    String target = wikiPage.getRedirect().toLowerCase();
                    String source = wikiPage.getTitle();

                    String key = "tmp:article:alias";
                    String value = jedis.hget(key, target);

                    List<String> list = null;
                    if (value == null) {
                        list = new ArrayList<>();
                    } else {
                        list = Lists.newArrayList(Splitter.on('\n')
                                .omitEmptyStrings()
                                .trimResults()
                                .splitToList(value));
                    }
                    if (!list.contains(source)) {
                        list.add(source);
                        String s = Joiner.on("\n").join(list);
                        jedis.hset(key, target, s);
                    }
                } else if (accept(wikiPage)) {
                    normalArticles++;
                    wikiPage.writeIn(out);

                    //记录所指向词条的入链数量
                    String key = "tmp:article:inlinks";
                    for (String link : wikiPage.getInternalLinks()) {
                        jedis.hincrBy(key, link.toLowerCase(), 1);
                    }

                    //记录所隶属分类的词条数量
                    key = "tmp:cat:articles";
                    for (String c : wikiPage.getCategories()) {
                        jedis.hincrBy(key, c.toLowerCase(), 1);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (wikiPage.isCategory()) {
            totalCategories++;
            if (wikiPage.isCommonCategory()) {
                try {
                    normalCategories++;
                    wikiPage.writeIn(out);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if(totalPages%10000==0) {
            System.out.println("Parsed " + totalPages + " pages.");
        }
    }

    public void close() throws IOException {
        String key = "tmp:summary:stop1";
        jedis.hset(key, "totalPages", Integer.toString(totalPages));
        jedis.hset(key, "totalArticles", Integer.toString(totalArticles));
        jedis.hset(key, "normalArticles", Integer.toString(normalArticles));
        jedis.hset(key, "redirectArticles", Integer.toString(redirectArticles));
        jedis.hset(key, "totalCategories", Integer.toString(totalCategories));
        jedis.hset(key, "normalCategories", Integer.toString(normalCategories));
        out.close();
    }
}
