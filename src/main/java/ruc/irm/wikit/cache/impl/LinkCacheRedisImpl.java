package ruc.irm.wikit.cache.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.primitives.Ints;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import ruc.irm.wikit.cache.ArticleCache;
import ruc.irm.wikit.cache.Cache;
import ruc.irm.wikit.cache.LinkCache;
import ruc.irm.wikit.cache.NameIdMapping;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.exception.MissedException;
import ruc.irm.wikit.data.dump.impl.PageSequenceDump;
import ruc.irm.wikit.data.dump.parse.WikiPage;
import ruc.irm.wikit.data.dump.parse.WikiPageFilter;
import ruc.irm.wikit.util.NumberUtils;
import ruc.irm.wikit.util.ProgressCounter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * <p>LinkDb implemented using Redis database.</p>
 *
 * <p>For every page, we save all its output links with frequency, and save all
 * its in link page id. Therefore, we use Redis Hash data structure to save
 * out-link page id with its frequency, and use Set data structure to save
 * all in-links</p>
 *
 * @author Tian Xia
 * @date Jan 19, 2016 11:17 AM
 */
public class LinkCacheRedisImpl implements LinkCache, Cache {
    private Logger LOG = LoggerFactory.getLogger(LinkCacheRedisImpl.class);
    private Conf conf = null;

    private String prefix = "";
    private Jedis jedis = null;


    public LinkCacheRedisImpl(Conf conf) {
        this.conf = conf;
        this.prefix = conf.getRedisPrefix() + "link:";
        this.jedis = new Jedis(conf.getRedisHost(), conf.getRedisPort(), conf.getRedisTimeout());
    }

    public void build(PageSequenceDump dump) throws IOException {
        NameIdMapping nameIdMapping = new ArticleCacheRedisImpl(conf);
        if (!nameIdMapping.nameIdMapped()) {
            System.out.println("Please build article cache first!");
            return;
        }

        this.clearAll();
        dump.traverse(new WikiPageFilter() {
            private int pages = 0;
            private int inLinks = 0;
            private int outLinks = 0;
            private int skipLinks = 0;

            @Override
            public void process(WikiPage wikiPage, int index) {
                if (wikiPage.isArticle() && !wikiPage.isRedirect()) {
                    List<String> internalLinks = wikiPage.getInternalLinks();
                    Map<String, Integer> linkFreqMap = new HashMap<>();
                    for (String target : internalLinks) {
                        linkFreqMap.put(target,
                                linkFreqMap.getOrDefault(target, 0) + 1);
                    }

                    //add out links, for inlink, we use Set structure because
                    // some page may have a lot of in links, but for
                    // outlinks, it's limited, so, we use Hash to save outlink.
                    byte[] keyOut = makeKey(prefix + "out:", wikiPage.getId());
                    for (Map.Entry<String, Integer> entry : linkFreqMap.entrySet()) {
                        int targetId = nameIdMapping.getIdByName(entry.getKey(), 0);
                        if (targetId == 0) {
                            skipLinks++;
                            continue;
                        }
                        jedis.hset(keyOut, Ints.toByteArray(targetId),
                                Ints.toByteArray(entry.getValue()));
                        outLinks += entry.getValue();

                        //update in link info
                        byte[] keyIn = makeKey(prefix + "in:", targetId);
                        jedis.sadd(keyIn, Ints.toByteArray(wikiPage.getId()));
                        inLinks++;
                    }

                    pages++;
                } else {
                    LOG.error("Meet page that is not a normal article");
                    System.out.println(wikiPage);
                }
            }

            @Override
            public void close (){
                //save summary info
                byte[] key = (prefix + "summary").getBytes(ENCODING);
                byte[] hkey = "totalPages".getBytes(ENCODING);
                jedis.hset(key, hkey, Ints.toByteArray(pages));

                hkey = "inLinks".getBytes(ENCODING);
                jedis.hset(key, hkey, Ints.toByteArray(inLinks));

                hkey = "outLinks".getBytes(ENCODING);
                jedis.hset(key, hkey, Ints.toByteArray(outLinks));

                hkey = "skipLinks".getBytes(ENCODING);
                jedis.hset(key, hkey, Ints.toByteArray(skipLinks));

                LOG.info(pages + " articles are processed.");

            }
        });
    }


    private byte[] makeKey(String head, int pageId) {
        byte[] first = head.getBytes(ENCODING);
        byte[] second = NumberUtils.int2Bytes(pageId);
        byte[] full = new byte[first.length + second.length];
        System.arraycopy(first, 0, full, 0, first.length);
        System.arraycopy(second, 0, full, first.length, second.length);
        return full;
    }

    @Override
    public TIntSet getInlinks(int pageId) {
        byte[] key = makeKey(prefix + "in:", pageId);
        Set<byte[]> values = jedis.smembers(key);

        TIntSet ids = new TIntHashSet();
        for (byte[] id : values) {
            ids.add(NumberUtils.bytes2Int(id));
        }
        return ids;
    }

    public long getInlinkCount(int pageId) {
        byte[] key = makeKey(prefix + "in:", pageId);
        return jedis.scard(key);
    }

    public long getOutlinkCount(int pageId) {
        return getOutlinks(pageId).size();
    }

    @Override
    public TIntSet getOutlinks(int pageId) {
        byte[] key = makeKey(prefix + "out:", pageId);
        Set<byte[]> keys = jedis.hkeys(key);

        TIntSet ids = new TIntHashSet();
        for (byte[] id : keys) {
            ids.add(NumberUtils.bytes2Int(id));
        }
        return ids;
    }

    @Override
    public int getTotalPages() {
        byte[] key = (prefix + "summary").getBytes(ENCODING);
        byte[] bytes = jedis.hget(key, "totalPages".getBytes(ENCODING));
        if (bytes == null) {
            return 0;
        } else {
            return Ints.fromByteArray(bytes);
        }
    }

    private void saveGraphToCSV(Writer writer,
                                String rootName,
                                TIntSet links,
                                boolean out) {
        ArticleCache articleCache = new ArticleCacheRedisImpl(conf);

        links.forEach(new TIntProcedure() {
            @Override
            public boolean execute(int id) {
                String name = articleCache.getNameById(id, "ERROR");
                try {
                    if(out) {
                        writer.append(rootName).append(",").append(name).append("\n");
                    } else {
                        writer.append(name).append(",").append(rootName).append("\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return true;
            }
        });
    }

    private long makeNodeSize(int pageId) {
        return (getInlinkCount(pageId) + getOutlinkCount(pageId))*10;
    }

    private List makeOutlinkChildren(int pageId, ArticleCache articleCache) {
        List children = new ArrayList<>();
        TIntSet links = getOutlinks(pageId);
        for(int id: links.toArray()){
            String title = articleCache.getNameById(id, "ERROR");
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", id);
            item.put("name", title);
            item.put("size", makeNodeSize(id));
            children.add(item);
        }
        return children;
    }

    @Override
    public void writeNeighborsToJson(int pageId, File f) throws
            IOException {
        final Writer writer = Files.newWriter(f, Charsets.UTF_8);

        ArticleCache articleCache = new ArticleCacheRedisImpl(conf);
        String name = null;
        try {
            name = articleCache.getNameById(pageId);
        } catch (MissedException e) {
            throw new IOException(e);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", pageId);
        data.put("name", name);

        TIntSet links = getOutlinks(pageId);
        List children = new ArrayList<>();

        links.forEach(new TIntProcedure() {
            @Override
            public boolean execute(int id) {
                String title = articleCache.getNameById(id, "ERROR");
                Map<String, Object> item = new HashMap<String, Object>();
                item.put("id", id);
                item.put("name", title);
                List list = makeOutlinkChildren(id, articleCache);
                item.put("size", makeNodeSize(id));
                item.put("children", list);

                children.add(item);
                return true;
            }
        });

        data.put("children", children);
        JSON.writeJSONStringTo(data, writer, SerializerFeature.PrettyFormat);
        writer.close();
    }

    @Override
    public void saveGraph(int id1, int id2, File outFile) throws IOException,
            MissedException {
        final Writer writer = Files.newWriter(outFile, Charsets.UTF_8);
        ArticleCache articleCache = new ArticleCacheRedisImpl(conf);
        final String name1 = articleCache.getNameById(id1);
        final String name2 = articleCache.getNameById(id2);

        saveGraphToCSV(writer, name1, getOutlinks(id1), true);
        saveGraphToCSV(writer, name2, getOutlinks(id2), true);
        saveGraphToCSV(writer, name1, getInlinks(id1), false);
        saveGraphToCSV(writer, name2, getInlinks(id2), false);
        writer.close();
    }

    @Override
    public void clearAll() {
        Set<byte[]> keys = jedis.keys((prefix+"*").getBytes());
        ProgressCounter counter = new ProgressCounter(keys.size());
        for (byte[] key : keys) {
            jedis.del(key);
            counter.increment();
        }
        counter.done();
    }
}
