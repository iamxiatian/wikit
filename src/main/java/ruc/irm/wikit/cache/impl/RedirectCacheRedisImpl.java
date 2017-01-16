package ruc.irm.wikit.cache.impl;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import ruc.irm.wikit.cache.ArticleCache;
import ruc.irm.wikit.cache.RedirectCache;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.util.NumberUtils;
import ruc.irm.wikit.util.ProgressCounter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Tian Xia
 * @date Feb 03, 2016 11:40 AM
 */
public class RedirectCacheRedisImpl implements RedirectCache {
    private static final Logger LOG = LoggerFactory.getLogger(ArticleCacheRedisImpl.class);
    private Conf conf = null;
    private String prefix = "";
    private Jedis jedis = null;

    public RedirectCacheRedisImpl(Conf conf) {
        this.conf = conf;
        this.prefix = conf.getRedisPrefix();
        this.jedis = new Jedis(conf.getRedisHost(), conf.getRedisPort(), conf.getRedisTimeout());
    }

    @Override
    public int getRedirectToId(String fromName, int notExistId) {
        byte[] key = (prefix + "vr:name2id").getBytes(ENCODING);
        byte[] hkey = fromName.getBytes(ENCODING);

        byte[] value = jedis.hget(key, hkey);
        return NumberUtils.bytes2Int(value, notExistId);
    }

    @Override
    public Collection<String> getRedirectNames(int pageId) {
        byte[] key = (prefix + "vr:id2name").getBytes(ENCODING);
        byte[] hkey = NumberUtils.int2Bytes(pageId);

        byte[] value = jedis.hget(key, hkey);
        if (value == null) {
            return new HashSet<>();
        } else {
            String s = new String(value, ENCODING);
            return Lists.newArrayList(Splitter.on('\n')
                    .omitEmptyStrings()
                    .trimResults()
                    .splitToList(s));
        }
    }

    private void saveRedirect(int fromId, String fromName,
                             String toName) {
        if (fromName.equalsIgnoreCase(toName)) {
            return;
        }

        ArticleCache articleCache = new ArticleCacheRedisImpl(conf);
        int targetId = articleCache.getIdByName(toName, 0);
        if (targetId == 0) {
            System.out.println("skip " + fromId + ", " + fromName + ", " + toName);
            return;
        }

        byte[] key = (prefix + "vr:name2id").getBytes(ENCODING);
        byte[] hkey = fromName.getBytes(ENCODING);
        jedis.hset(key, hkey, NumberUtils.int2Bytes(targetId));


        key = (prefix + "vr:id2name").getBytes(ENCODING);
        hkey = NumberUtils.int2Bytes(targetId);

        Collection<String> names = getRedirectNames(targetId);
        if (!names.contains(fromName)) {
            names.add(fromName);
            String s = Joiner.on("\n").join(names);
            jedis.hset(key, hkey, s.getBytes(ENCODING));
        }
    }


    @Override
    public void build(File redirectTextFile) throws IOException {
        clearAll();

        BufferedReader reader = Files.newBufferedReader(redirectTextFile.toPath
                        (), Charsets.UTF_8);
        String line = null;
        ProgressCounter counter = new ProgressCounter();
        while ((line = reader.readLine()) != null) {
            if (line.length() == 0) {
                continue;
            }

            List<String> list = Splitter.on('\t')
                    .omitEmptyStrings()
                    .trimResults()
                    .splitToList(line);
            int fromId = Integer.parseInt(list.get(0));
            String fromName = list.get(1);
            String toName = list.get(2);
            //System.out.println(fromId + ", " + fromName + ", " + toName);
            saveRedirect(fromId, fromName, toName);
            counter.increment();
        }

        counter.done();
    }

    @Override
    public void clearAll() {
        Set<byte[]> keys = jedis.keys((prefix+"vr:*").getBytes());
        for (byte[] key : keys) {
            jedis.del(key);
        }
    }
}
