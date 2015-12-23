package ruc.irm.wikit.esa.concept;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.exception.MissedException;
import ruc.irm.wikit.util.NumberUtils;

import java.io.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * User: xiatian
 * Date: 3/31/14
 * Time: 2:15 PM
 */
public class ConceptCacheRedisImpl implements ConceptCache, Closeable {
    private Logger LOG = LoggerFactory.getLogger(ConceptCacheRedisImpl.class);

    private String prefix = "";
    private Jedis jedis = null;
    private Conf conf = null;

    public ConceptCacheRedisImpl(Conf conf) {
        this.conf = conf;
        this.prefix = conf.getRedisPrefix();
        this.jedis = new Jedis(conf.get("esa.redis.host"), conf.getInt("esa.redis.port", 6379), conf.getRedisTimeout());
    }

    public ConceptCacheRedisImpl(String host, int port, String prefix) {
        this.prefix = prefix;
        this.jedis = new Jedis(host, port);
    }


    @Override
    public void saveMaxConceptId(int maxId) {
        jedis.hset(KEY_CONFIG, HKEY_CONFIG_MAXID, NumberUtils.int2Bytes(maxId));
    }

    @Override
    public int getMaxConceptId() {
        byte[] value = jedis.hget(KEY_CONFIG, HKEY_CONFIG_MAXID);
        return NumberUtils.bytes2Int(value, 0);
    }

    @Override
    public void saveSumOfPageViews(long pv) {
        jedis.hset(KEY_CONFIG, HKEY_CONFIG_PV_SUM, NumberUtils.long2Bytes(pv));
    }

    @Override
    public long getSumOfPageViews() {
        byte[] value = jedis.hget(KEY_CONFIG, HKEY_CONFIG_PV_SUM);
        return NumberUtils.bytes2Long(value, 0);
    }

    @Override
    public void saveAlias(int id, Collection<String> aliasNames) {
        if (aliasNames == null || aliasNames.isEmpty()) {
            return;
        }

        byte[] key = (prefix + "cpt:id2alias").getBytes(ENCODING);
        String s = Joiner.on("\n").join(aliasNames);
        jedis.hset(key, NumberUtils.int2Bytes(id), s.getBytes(ENCODING));

        key = (prefix + "cpt:alias2id").getBytes(ENCODING);
        byte[] value = NumberUtils.int2Bytes(id);
        for (String name : aliasNames) {
            byte[] hkey = name.toLowerCase().getBytes(ENCODING);
            jedis.hset(key, hkey, value);
        }
    }

    @Override
    public Collection<String> getAliasNames(int id) {
        byte[] key = (prefix + "cpt:id2alias").getBytes(ENCODING);
        byte[] value = jedis.hget(key, NumberUtils.int2Bytes(id));
        if (value == null) {
            return new HashSet<>();
        } else {
            String s = new String(value, ENCODING);
            return Sets.newHashSet(Splitter.on('\n')
                    .omitEmptyStrings()
                    .trimResults()
                    .splitToList(s));
        }
    }

    @Override
    public int getIdByAlias(String alias, int defaultValue) {
        byte[] key = (prefix + "cpt:alias2id").getBytes(ENCODING);
        byte[] hkey = alias.toLowerCase().getBytes(ENCODING);
        return NumberUtils.bytes2Int(jedis.hget(key, hkey), defaultValue);
    }

    @Override
    public void incPageView(int id, int incCount) {
        byte[] hkey = NumberUtils.int2Bytes(id);
        jedis.hincrBy(KEY_PAGE_VIEW, hkey, 1);
    }

    @Override
    public int getPageViewById(int id) {
        byte[] hkey = NumberUtils.int2Bytes(id);
        byte[] value = jedis.hget(KEY_PAGE_VIEW, hkey);
        return NumberUtils.bytes2Int(value, 0);
    }

    @Override
    public void saveCategories(int id, Collection<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return;
        }
        byte[] key = (prefix + "cpt:id2category").getBytes(ENCODING);
        String s = Joiner.on("\n").join(categories);
        jedis.hset(key, NumberUtils.int2Bytes(id), s.getBytes(ENCODING));

    }

    @Override
    public Collection<String> getCategoriesById(int id) {
        byte[] key = (prefix + "cpt:id2category").getBytes(ENCODING);
        byte[] hkey = NumberUtils.int2Bytes(id);
        byte[] value = jedis.hget(key, hkey);
        if (value == null) {
            return new HashSet<>();
        } else {
            String s = new String(value, ENCODING);
            return Sets.newHashSet(Splitter.on('\n')
                    .omitEmptyStrings()
                    .trimResults()
                    .splitToList(s));
        }
    }

    @Override
    public void saveOutId(int id, String outId) {
        byte[] key = (prefix + "cpt:id2outid").getBytes(ENCODING);
        byte[] hkey = NumberUtils.int2Bytes(id);
        jedis.hset(key, hkey, outId.getBytes(ENCODING));
    }

    @Override
    public String getOutIdById(int id) {
        byte[] key = (prefix + "cpt:id2outid").getBytes(ENCODING);
        byte[] hkey = NumberUtils.int2Bytes(id);
        byte[] value = jedis.hget(key, hkey);

        return (value == null) ? null : new String(value, ENCODING);
    }

    @Override
    public void saveLinkRelation(int fromId, int toId) {
        byte[] key = (prefix + "cpt:id2inlink:set").getBytes(ENCODING);
        byte[] fromBytes =  NumberUtils.int2Bytes(toId);
        byte[] toBytes = NumberUtils.int2Bytes(toId);

        byte[] value = jedis.hget(key, toBytes);
        Set<Integer> ids = NumberUtils.bytes2IntSet(value);
        ids.add(fromId);
        jedis.hset(key, toBytes, NumberUtils.intSet2Bytes(ids));

        //save inlink count
        key = (prefix + "cpt:id2inlink:count").getBytes(ENCODING);
        jedis.hset(key, toBytes, NumberUtils.int2Bytes(ids.size()));

        //save outlink set
        key = (prefix + "cpt:id2outlink:set").getBytes(ENCODING);
        value = jedis.hget(key, fromBytes);
        ids = NumberUtils.bytes2IntSet(value);
        ids.add(toId);
        jedis.hset(key, fromBytes, NumberUtils.intSet2Bytes(ids));

        //save outlink count
        key = (prefix + "cpt:id2outlink:count").getBytes(ENCODING);
        jedis.hset(key, fromBytes, NumberUtils.int2Bytes(ids.size()));

    }

    @Override
    public int getInlinkCount(int id) {
        byte[] key = (prefix + "cpt:id2inlink:count").getBytes(ENCODING);
        byte[] value = jedis.hget(key, NumberUtils.int2Bytes(id));
        return NumberUtils.bytes2Int(value, 0);
    }

    @Override
    public Set<Integer> getInlinkIds(int id) {
        byte[] key = (prefix + "cpt:id2inlink:set").getBytes(ENCODING);
        byte[] value = jedis.hget(key, NumberUtils.int2Bytes(id));
        return NumberUtils.bytes2IntSet(value);
    }

    @Override
    public int getOutlinkCount(int id) {
        byte[] key = (prefix + "cpt:id2outlink:count").getBytes(ENCODING);
        byte[] value = jedis.hget(key, NumberUtils.int2Bytes(id));
        return NumberUtils.bytes2Int(value, 0);
    }

    @Override
    public Set<Integer> getOutlinkIds(int id) {
        byte[] key = (prefix + "cpt:id2outlink:set").getBytes(ENCODING);
        byte[] value = jedis.hget(key, NumberUtils.int2Bytes(id));
        return NumberUtils.bytes2IntSet(value);
    }

    @Override
    public void clearAll() {
        LOG.info("Remove id2name");
        byte[] key = (prefix + "cpt:id2name").getBytes(ENCODING);
        jedis.del(key);

        LOG.info("Remove name2id");
        key = (prefix + "cpt:name2id").getBytes(ENCODING);
        jedis.del(key);
    }


    //////////////////////////////////////////////////////
    //
    // 以下为NameIdMapping接口实现
    //
    ///////////////////////////////////////////////////////
    @Override
    public void saveNameIdMapping(String name, int id) {
        if (StringUtils.isEmpty(name)) {
            return;
        }

        if (idExist(id)) {
            LOG.warn("id has already existed for <" + id + ", " + name + ">");
            return;
        }

        //save name->id mapping
        byte[] key = (prefix + "cpt:name2id").getBytes(ENCODING);
        List<Integer> ids = getAllIdsByName(name);
        if (!ids.contains(id)) {
            ids.add(id);
            jedis.hset(key, name.toLowerCase().getBytes(ENCODING),
                    NumberUtils.intList2Bytes(ids));
        }

        //save id->name mapping
        key = (prefix + "cpt:id2name").getBytes(ENCODING);
        jedis.hset(key, NumberUtils.int2Bytes(id), name.getBytes(ENCODING));
    }

    @Override
    public boolean nameExist(String name) {
        byte[] key = (prefix + "cpt:name2id").getBytes(ENCODING);
        return jedis.hexists(key, name.toLowerCase().getBytes(ENCODING));
    }

    @Override
    public boolean idExist(int id) {
        byte[] key = (prefix + "cpt:id2name").getBytes(ENCODING);
        return jedis.hexists(key, NumberUtils.int2Bytes(id));
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
            throw new MissedException("concept " + name + " does" +
                    " not exist");
        } else {
            return ids.get(0);
        }
    }

    @Override
    public List<Integer> getAllIdsByName(String name) {
        byte[] key = (prefix + "cpt:name2id").getBytes(ENCODING);
        byte[] value = jedis.hget(key, name.toLowerCase().getBytes(ENCODING));
        return NumberUtils.bytes2IntList(value);
    }

    @Override
    public String getNameById(int conceptId) throws MissedException {
        byte[] key = (prefix + "cpt:id2name").getBytes(ENCODING);
        byte[] value = jedis.hget(key, NumberUtils.int2Bytes(conceptId));
        if (value == null) {
            throw new MissedException("Concept does not exist for id"
                    + conceptId);
        }
        return new String(value, ENCODING);
    }

    @Override
    public String getNameById(int conceptId, String defaultValue) {
        byte[] key = (prefix + "cpt:id2name").getBytes(ENCODING);
        byte[] value = jedis.hget(key, NumberUtils.int2Bytes(conceptId));

        return (value==null)?defaultValue:new String(value, ENCODING);
    }

    @Override
    public void finishNameIdMapping() {
        jedis.hset(KEY_CONFIG, HKEY_CONFIG_NAMEID_MAPPED, "true".getBytes(ENCODING));
    }

    @Override
    public boolean nameIdMapped() {
        byte[] value = jedis.hget(KEY_CONFIG, HKEY_CONFIG_NAMEID_MAPPED);
        return value!=null && new String(value, ENCODING).equals("true");
    }


    @Override
    public Set<Integer> listIds() {
        Set<byte[]> keys = jedis.hkeys((prefix + "cpt:id2name").getBytes(ENCODING));
        return keys.stream().mapToInt(NumberUtils::bytes2Int).boxed().collect(Collectors.toSet());
    }

    ///////////////////////////////////////
    //
    // ESA TF-IDF information
    //
    ///////////////////////////////////////

    public void saveIdf(String term, float idf) {
        byte[] key = (prefix + "cpt:term2idf").getBytes(ENCODING);
        jedis.hset(key, term.toLowerCase().getBytes(ENCODING),
                NumberUtils.float2Bytes(idf));
    }


    public float getIdf(String term, float defaultValue) {
        byte[] key = (prefix + "cpt:term2idf").getBytes(ENCODING);
        byte[] value = jedis.hget(key, term.toLowerCase().getBytes(ENCODING));
        return NumberUtils.bytes2Float(value, defaultValue);
    }

    public void saveTfIdf(String term, byte[] vector) {
        byte[] key = (prefix + "cpt:term2tfidf").getBytes(ENCODING);
        jedis.hset(key, term.getBytes(ENCODING), vector);
    }

    public DocScore[] getTfIdf(String term) throws IOException {
        byte[] key = (prefix + "cpt:term2tfidf").getBytes(ENCODING);
        byte[] vector = jedis.hget(key, term.getBytes(ENCODING));
        if (vector == null) {
            return null;
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(vector);
        DataInputStream dis = new DataInputStream(bais);
        /**
         * 4 bytes: int - length of array
         * 4 byte (doc) - 8 byte (tfidf) pairs
         */
        int vectorLength = dis.readInt();
        DocScore[] docScores = new DocScore[vectorLength];

        // System.out.println("vector len: " + vectorLength);
        for (int k = 0; k < vectorLength; k++) {
            docScores[k] = new DocScore(dis.readInt(), dis.readFloat());
        }

        return docScores;
    }

    public void importIdf(String idfFile) throws IOException {
        byte[] key = (prefix + "cpt:term2idf").getBytes(ENCODING);
        jedis.del(key);
        BufferedReader reader = new BufferedReader(new FileReader(idfFile));
        String line = null;
        int count = 0;
        while ((line = reader.readLine()) != null) {
            count++;
            if (line.length() > 0) {
                String[] items = StringUtils.split(line, "\t");
                if (count % 1000 == 0) {
                    System.out.println("process " + count + "\t" + items[0] + "," + items[1] + "...");
                }
                saveIdf(items[0], Float.parseFloat(items[1]));
            }
        }

        reader.close();
    }

    public void importTfIdf(String tfidfFile) throws IOException {
        byte[] key = (prefix + "cpt:term2tfidf").getBytes(ENCODING);
        jedis.del(key);

        DataInputStream in = new DataInputStream(new FileInputStream(tfidfFile));

        int count = 0;
        int len = 0;
        while ((len = in.readInt()) >= 0) {
            count++;
            if (count % 500 == 0) {
                System.out.print(count + "\t");
            }
            byte[] termArray = new byte[len];
            in.readFully(termArray);

            len = in.readInt();
            if (len > 0) {
                byte[] vector = new byte[len];
                in.readFully(vector);
                String term = new String(termArray, "utf-8");
                saveTfIdf(term, vector);
            }
        }
        in.close();
    }

    @Override
    public void close() {
        jedis.close();
    }

    private final byte[] KEY_PAGE_VIEW =  (prefix + "cpt:pv").getBytes(ENCODING);
    private final byte[] KEY_CONFIG =  (prefix + "cpt:cnf").getBytes(ENCODING);
    private final byte[] HKEY_CONFIG_MAXID =  "maxId".getBytes(ENCODING);
    private final byte[] HKEY_CONFIG_PV_SUM =  "pv_sum".getBytes(ENCODING);
    private final byte[] HKEY_CONFIG_NAMEID_MAPPED =  "nameIdMapped".getBytes(ENCODING);
}
