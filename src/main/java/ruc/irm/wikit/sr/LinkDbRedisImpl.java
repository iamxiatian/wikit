package ruc.irm.wikit.sr;

import com.google.common.primitives.Ints;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import ruc.irm.wikit.cache.Cache;
import ruc.irm.wikit.cache.NameIdMapping;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.exception.WikitException;
import ruc.irm.wikit.data.dump.WikiPageDump;
import ruc.irm.wikit.data.dump.impl.PageSequenceDump;
import ruc.irm.wikit.data.dump.parse.WikiPage;
import ruc.irm.wikit.data.dump.parse.WikiPageFilter;
import ruc.irm.wikit.esa.concept.ConceptCacheRedisImpl;
import ruc.irm.wikit.util.NumberUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
public class LinkDbRedisImpl implements LinkDb, Cache {
    private Logger LOG = LoggerFactory.getLogger(LinkDbRedisImpl.class);
    private Conf conf = null;

    private String prefix = "";
    private Jedis jedis = null;


    public LinkDbRedisImpl(Conf conf) {
        this.prefix = conf.getRedisPrefix() + "link:";
        this.jedis = new Jedis(conf.getRedisHost(), conf.getRedisPort(), conf.getRedisTimeout());
    }

    public void build(PageSequenceDump dump) throws IOException {
        NameIdMapping nameIdMapping = new ConceptCacheRedisImpl(conf);
        if (!nameIdMapping.nameIdMapped()) {
            System.out.println("Please build concept cache first!");
            return;
        }

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

                    //add out links
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
                    LOG.error("Meet page that is not a normal article", wikiPage);
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

    @Override
    public TIntSet getOutLinks(int pageId) {
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

    @Override
    public void clearAll() {

    }
}
