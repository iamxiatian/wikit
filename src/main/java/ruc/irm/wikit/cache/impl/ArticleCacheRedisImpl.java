package ruc.irm.wikit.cache.impl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import ruc.irm.wikit.cache.ArticleCache;
import ruc.irm.wikit.cache.NameIdMapping;
import ruc.irm.wikit.cache.RedirectCache;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.exception.MissedException;
import ruc.irm.wikit.util.NumberUtils;
import ruc.irm.wikit.util.ProgressCounter;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * We use "vka" abbreviation for "wiki article page"
 *
 * User: xiatian
 * Date: 4/21/14
 * Time: 12:02 AM
 */
public class ArticleCacheRedisImpl implements ArticleCache,NameIdMapping,
        Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(ArticleCacheRedisImpl.class);
    private Conf conf;
    private String prefix = "";
    private Jedis jedis = null;
    private NameIdMapping categoryNameIdMapping = null;

    public ArticleCacheRedisImpl(Conf conf) {
        this.conf = conf;
        this.prefix = conf.getRedisPrefix();
        this.jedis = new Jedis(conf.getRedisHost(), conf.getRedisPort(), conf.getRedisTimeout());
        this.categoryNameIdMapping = new CategoryCacheRedisImpl(conf);
    }

    private byte[] makePageKey(int pageId) {
        byte[] first = (prefix + "va:p:").getBytes(ENCODING);
        byte[] second = NumberUtils.int2Bytes(pageId);
        byte[] full = new byte[first.length + second.length];
        System.arraycopy(first, 0, full, 0, first.length);
        System.arraycopy(second, 0, full, first.length, second.length);
        return full;
    }

    @Override
    public void saveNameIdMapping(String name, int pageId) {
        if (StringUtils.isEmpty(name)) {
            return;
        }

        if(idExist(pageId)){
            LOG.warn(pageId + "->" + name + " has already existed.");
            return;
        }

        //save name->id mapping
        byte[] key = (prefix + "va:name2id").getBytes(ENCODING);
        List<Integer> ids = getAllIdsByName(name);
        if (!ids.contains(pageId)) {
            ids.add(pageId);
            jedis.hset(key, name.toLowerCase().getBytes(ENCODING),
                    NumberUtils.intList2Bytes(ids));
        }

        //save id->name mapping
        key = (prefix + "va:id2name").getBytes(ENCODING);
        jedis.hset(key, NumberUtils.int2Bytes(pageId), name.getBytes(ENCODING));
    }

    @Override
    public boolean nameExist(String name) {
        byte[] key = (prefix + "va:name2id").getBytes(ENCODING);
        return jedis.hexists(key, name.toLowerCase().getBytes(ENCODING));
    }

    @Override
    public boolean idExist(int pageId) {
        byte[] key = (prefix + "va:id2name").getBytes(ENCODING);
        return jedis.hexists(key, NumberUtils.int2Bytes(pageId));
    }

    @Override
    public int getIdByName(String name, int valueForNotExisted) {
        List<Integer> ids = getAllIdsByName(name);
        return ids.isEmpty()?valueForNotExisted : ids.get(0);
    }


    @Override
    public int getIdByName(String name) throws MissedException {
        List<Integer> ids = getAllIdsByName(name);
        if (ids.isEmpty()) {
            throw new MissedException("wiki article " + name + " does" +
                    " not exist");
        } else {
            return ids.get(0);
        }
    }

    @Override
    public Set<Integer> listIds() {
        Set<byte[]> keys = jedis.hkeys((prefix + "va:id2name").getBytes(ENCODING));
        return keys.stream().mapToInt(NumberUtils::bytes2Int).boxed()
                .collect(Collectors.toSet());
    }

    @Override
    public List<Integer> getAllIdsByName(String name) {
        byte[] key = (prefix + "va:name2id").getBytes(ENCODING);
        byte[] value = jedis.hget(key, name.toLowerCase().getBytes(ENCODING));
        return NumberUtils.bytes2IntList(value);
    }

    @Override
    public String getNameById(int pageId) throws MissedException {
        byte[] key = (prefix + "va:id2name").getBytes(ENCODING);
        byte[] value = jedis.hget(key, NumberUtils.int2Bytes(pageId));
        if (value == null) {
            throw new MissedException("wiki article page does not " +
                    "exist for id" + pageId);
        }
        return new String(value, ENCODING);
    }

    @Override
    public String getNameById(int id, String defaultValue) {
        byte[] key = (prefix + "va:id2name").getBytes(ENCODING);
        byte[] value = jedis.hget(key, NumberUtils.int2Bytes(id));
        return  (value == null) ? defaultValue:new String(value, ENCODING);
    }

    @Override
    public void finishNameIdMapping() {
        String key = (prefix + "va:cnf");
        jedis.hset(key, "nameIdMapped", "true");
    }

    @Override
    public boolean nameIdMapped() {
        String key = (prefix + "va:cnf");
        String value = jedis.hget(key, "nameIdMapped");
        return "true".equals(value);
    }

    @Override
    public int getIdByNameOrAlias(String name) {
        int id = getIdByName(name, -1);
        if (id < 0) {
            RedirectCache redirectCache = new RedirectCacheRedisImpl(conf);
            id = redirectCache.getRedirectToId(name, 0);
        }
        return id;
    }

    @Override
    public void saveCategories(int pageId, Set<String> categories) {
        byte[] key = makePageKey(pageId);
        Set<Integer> catIds = new HashSet<>();

        for (String c : categories) {
            int catId = categoryNameIdMapping.getIdByName(c, -1);
            if (catId <= 0) {
                LOG.warn("category " + c + " does not exist.");
            } else {
                catIds.add(catId);
            }
        }
        if (!catIds.isEmpty()) {
            jedis.hset(key, HKEY_CATEGORIES, NumberUtils.intSet2Bytes(catIds));
        }
    }

    @Override
    public Set<Integer> getCategories(int pageId) {
        byte[] key = makePageKey(pageId);
        byte[] value = jedis.hget(key, HKEY_CATEGORIES);
        return NumberUtils.bytes2IntSet(value);
    }

    @Override
    public void close() throws IOException {
        jedis.close();
    }

    @Override
    public void done() {
        String key = (prefix + "va:cnf");
        jedis.hset(key,"status", "done");
    }

    @Override
    public boolean hasDone() {
        String key = (prefix + "va:cnf");
        String value = jedis.hget(key,"status");
        return "done".equals(value);
    }


    ////////////////////////////////////////
    // Cache interface implementation
    /////////////////////////////////////////
    @Override
    public void saveCacheToGZipFile() throws IOException {

    }

    @Override
    public void buildCacheFromGZipFile() throws IOException {

    }

    @Override
    public void clearAll() {
        Set<byte[]> keys = jedis.keys((prefix+"va:*").getBytes());
        ProgressCounter counter = new ProgressCounter(keys.size());
        for (byte[] key : keys) {
            jedis.del(key);
            counter.increment();
        }
        counter.done();
    }

    /**
     * 一个网页对象可以包含的哈希对象主键
     */
    private static final byte[] HKEY_CATEGORIES = "c".getBytes(ENCODING);
}
