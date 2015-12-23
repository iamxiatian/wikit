package ruc.irm.wikit.cache.impl;

import com.google.common.collect.Sets;
import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.exception.MissedException;
import ruc.irm.wikit.cache.Cache;
import ruc.irm.wikit.model.Category;
import ruc.irm.wikit.cache.CategoryCache;
import ruc.irm.wikit.cache.NameIdMapping;
import ruc.irm.wikit.util.NumberUtils;
import ruc.irm.wikit.util.ProgressCounter;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * User: xiatian
 * Date: 4/15/14
 * Time: 2:58 PM
 */
public class CategoryCacheRedisImpl implements CategoryCache,
        NameIdMapping, Cache {
    private static final Logger LOG = LoggerFactory.getLogger(CategoryCacheRedisImpl.class);

    private Conf conf;
    private String prefix = "";
    private Jedis jedis = null;

    public CategoryCacheRedisImpl(Conf conf) {
        this.conf = conf;
        this.prefix = conf.get("category.redis.prefix", "");
        this.prefix += "vc:";
        this.jedis = new Jedis(conf.getRedisHost(), conf.getRedisPort(), conf.getRedisTimeout());
    }

//    private byte[] makePageKey(int pageId) {
//        byte[] first = (prefix + "p:").getBytes(ENCODING);
//        byte[] second = NumberUtils.int2Bytes(pageId);
//        byte[] full = new byte[first.length + second.length];
//        System.arraycopy(first, 0, full, 0, first.length);
//        System.arraycopy(second, 0, full, first.length, second.length);
//        return full;
//    }

    @Override
    public Set<Integer> listIds() {
        Set<byte[]> keys = jedis.hkeys((prefix + "id2name").getBytes(ENCODING));
        return keys.stream().mapToInt(NumberUtils::bytes2Int).boxed()
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> listNames() {
        List<byte[]> values = jedis.hvals((prefix + "id2name").getBytes(ENCODING));
        return values.stream().map(old -> new String(old, ENCODING)).collect(Collectors.toSet());
    }

    @Override
    public void saveParents(int catId, String... parents) {
        saveParents(catId, Sets.newHashSet(parents));
    }

    @Override
    public void saveParents(int catId, Collection<String> parents) {
        byte[] key = (prefix + "id2pids").getBytes(ENCODING);
        byte[] hkey = NumberUtils.int2Bytes(catId);

        byte[] value = jedis.hget(key, hkey);
        Set<Integer> ids = NumberUtils.bytes2IntSet(value);

        boolean changed = false;
        for (String p : parents) {
            if(nameExist(p)) {
                try {
                    ids.add(getIdByName(p));
                    changed = true;
                } catch (MissedException e) {
                    LOG.warn(e.toString());
                }
            }
        }
        if(changed)
            jedis.hset(key, hkey, NumberUtils.intSet2Bytes(ids));
    }

    @Override
    public void saveChildren(int catId, String... children) {
        byte[] key = (prefix + "id2cids").getBytes(ENCODING);
        byte[] hkey = NumberUtils.int2Bytes(catId);

        byte[] value = jedis.hget(key, hkey);
        Set<Integer> ids = NumberUtils.bytes2IntSet(value);

        boolean changed = false;
        for (String c : children) {
            if(nameExist(c)) {
                try {
                    ids.add(getIdByName(c));
                    changed = true;
                } catch (MissedException e) {
                    e.printStackTrace();
                }
            }
        }
        if(changed)
            jedis.hset(key, hkey, NumberUtils.intSet2Bytes(ids));
    }

    @Override
    public void saveChildren(int catId, int... childIds) {
        byte[] key = (prefix + "id2cids").getBytes(ENCODING);
        byte[] hkey = NumberUtils.int2Bytes(catId);
        byte[] value = jedis.hget(key, hkey);
        Set<Integer> ids = NumberUtils.bytes2IntSet(value);

        boolean changed = false;
        for (int id: childIds) {
            if(idExist(id) && !ids.contains(id)) {
                ids.add(id);
                changed = true;
            }
        }
        if(changed)
            jedis.hset(key, hkey, NumberUtils.intSet2Bytes(ids));
    }

    @Override
    public void addArticleRelation(int catId, int articleId) {
        byte[] key = (prefix + "id2aids").getBytes(ENCODING);
        byte[] hkey = NumberUtils.int2Bytes(catId);
        byte[] value = jedis.hget(key, hkey);
        Set<Integer> ids = NumberUtils.bytes2IntSet(value);

        if (!ids.contains(articleId)) {
            ids.add(articleId);
            jedis.hset(key, hkey, NumberUtils.intSet2Bytes(ids));
        }
    }

    @Override
    public Set<Integer> getParentIds(int catId) {
        byte[] key = (prefix + "id2pids").getBytes(ENCODING);
        byte[] hkey = NumberUtils.int2Bytes(catId);

        byte[] value = jedis.hget(key, hkey);
        Set<Integer> ids = NumberUtils.bytes2IntSet(value);
        return ids;
    }

    @Override
    public Set<String> getParentNames(String catName) {
        Set<Integer> ids = getParentIds(getIdByName(catName, 0));
        Set<String> names = new HashSet<>();
        for (int id : ids) {
            try {
                names.add(getNameById(id));
            } catch (MissedException e) {
                LOG.warn("missed child id:" + id, e);
            }
        }
        return names;
    }

    @Override
    public Set<Integer> getChildIds(int catId) {
        byte[] key = (prefix + "id2cids").getBytes(ENCODING);
        byte[] hkey = NumberUtils.int2Bytes(catId);

        byte[] value = jedis.hget(key, hkey);
        Set<Integer> ids = NumberUtils.bytes2IntSet(value);
        return ids;
    }

    @Override
    public Set<String> getChildNames(String catName) {
        Set<Integer> ids = getChildIds(getIdByName(catName, 0));
        Set<String> names = new HashSet<>();
        for (int id : ids) {
            try {
                names.add(getNameById(id));
            } catch (MissedException e) {
                LOG.warn("missed child id:" + id, e);
            }
        }
        return names;
    }

    @Override
    public Set<Integer> getArticleIds(int catId) {
        byte[] key = (prefix + "id2aids").getBytes(ENCODING);
        byte[] hkey = NumberUtils.int2Bytes(catId);

        byte[] value = jedis.hget(key, hkey);
        return NumberUtils.bytes2IntSet(value);
    }

    @Override
    public void incArticleCount(int catId) {
        incArticleCount(catId, 1);
    }

    @Override
    public void incArticleCount(int catId, int count) {
        byte[] key = (prefix + "id2ac").getBytes(ENCODING);
        byte[] hkey = NumberUtils.int2Bytes(catId);

        byte[] value = jedis.hget(key, hkey);
        if (value == null) {
            jedis.hset(key, hkey, NumberUtils.int2Bytes(count));
        } else {
            int old = NumberUtils.bytes2Int(value);
            jedis.hset(key, hkey, NumberUtils.int2Bytes(count+old));
        }
    }

    @Override
    public int getArticleCount(int catId) {
        byte[] key = (prefix + "id2ac").getBytes(ENCODING);
        byte[] hkey = NumberUtils.int2Bytes(catId);

        byte[] value = jedis.hget(key, hkey);
        return (value == null) ? 0 : NumberUtils.bytes2Int(value);
    }

    private void saveDepth(int catId, int depth) {
        byte[] key = (prefix + "id2depth").getBytes(ENCODING);
        byte[] hkey = NumberUtils.int2Bytes(catId);

        jedis.hset(key, hkey, NumberUtils.int2Bytes(depth));
    }

    private int getDepth(int catId) throws MissedException {
        byte[] key = (prefix + "id2depth").getBytes(ENCODING);
        byte[] hkey = NumberUtils.int2Bytes(catId);
        byte[] value = jedis.hget(key, hkey);
        if (value == null) {
            throw new MissedException(catId);
        } else {
            return NumberUtils.bytes2Int(value);
        }
    }

    public int getDepth(int catId, int defaultDepth) {
        byte[] key = (prefix + "id2depth").getBytes(ENCODING);
        byte[] hkey = NumberUtils.int2Bytes(catId);
        byte[] value = jedis.hget(key, hkey);
        return (value == null) ? defaultDepth : NumberUtils.bytes2Int(value);
    }

    @Override
    public Category getCategory(String catTitle) throws MissedException {
        int pageId = getIdByName(catTitle);

        Category c = new Category();
        c.setPageId(pageId);
        c.setTitle(catTitle);
        c.setParentIds(getParentIds(pageId));
        c.setArticleIds(getArticleIds(pageId));
        c.setArticleCount(getArticleCount(pageId));
        return c;
    }


    @Override
    public Category getCategory(int pageId) throws MissedException {
        String catTitle = getNameById(pageId);
        Category c = new Category();
        c.setPageId(pageId);
        c.setTitle(catTitle);
        c.setParentIds(getParentIds(pageId));
        c.setArticleIds(getArticleIds(pageId));
        c.setArticleCount(getArticleCount(pageId));

        return c;
    }

    /**
     * find category articles distribution, each output line like the following format:
     * [article count] \t [the category count which contains this specified article count]
     */
    public void displayCategoryArticlesDistribution() throws MissedException {
        Collection<String> categories = listNames();
        Bag<Integer> catDistBag = new HashBag<>();

        ProgressCounter counter = new ProgressCounter();
        for (String c : categories) {
            counter.increment();
            int articleCount = getArticleCount(getIdByName(c));
            catDistBag.add(articleCount);
        }

        for (int key: catDistBag.uniqueSet()) {
            System.out.println(key + "\t" + catDistBag.getCount(key));
        }
    }

    /////////////////////////////////////////////
    // NameIdMapping interface implementation  //
    /////////////////////////////////////////////

    @Override
    public void saveNameIdMapping(String name, int pageId) {
        if (StringUtils.isEmpty(name)) {
            return;
        }
        if(nameExist(name) || idExist(pageId)){
            LOG.warn(pageId + "->" + name + " has already existed.");
            return;
        }

        //save name->id mapping
        byte[] key = (prefix + "name2id").getBytes(ENCODING);
        jedis.hset(key, name.toLowerCase().getBytes(ENCODING), NumberUtils
                .int2Bytes(pageId));

        //save id->name mapping
        key = (prefix + "id2name").getBytes(ENCODING);
        jedis.hset(key, NumberUtils.int2Bytes(pageId), name.getBytes(ENCODING));
    }

    @Override
    public boolean nameExist(String name) {
        byte[] key = (prefix + "name2id").getBytes(ENCODING);
        return jedis.hexists(key, name.toLowerCase().getBytes(ENCODING));
    }

    @Override
    public boolean idExist(int pageId) {
        byte[] key = (prefix + "id2name").getBytes(ENCODING);
        return jedis.hexists(key, NumberUtils.int2Bytes(pageId));
    }

    @Override
    public int getIdByName(String name, int valueForNotExisted) {
        byte[] key = (prefix + "name2id").getBytes(ENCODING);
        byte[] value = jedis.hget(key, name.toLowerCase().getBytes(ENCODING));

        return value==null?valueForNotExisted:NumberUtils.bytes2Int(value);
    }

    @Override
    public int getIdByName(String name) throws MissedException {
        byte[] key = (prefix + "name2id").getBytes(ENCODING);
        byte[] value = jedis.hget(key, name.toLowerCase().getBytes(ENCODING));

        if (value == null) {
            throw new MissedException("wiki category " + name + " does not exist");
        }
        return NumberUtils.bytes2Int(value);
    }

    @Override
    public String getNameById(int pageId) throws MissedException {
        byte[] key = (prefix + "id2name").getBytes(ENCODING);
        byte[] value = jedis.hget(key, NumberUtils.int2Bytes(pageId));
        if (value == null) {
            throw new MissedException("wiki category page does not " +
                    "exist for id" + pageId);
        }
        return new String(value, ENCODING);
    }

    @Override
    public String getNameById(int id, String defaultValue) {
        byte[] key = (prefix + "id2name").getBytes(ENCODING);
        byte[] value = jedis.hget(key, NumberUtils.int2Bytes(id));
        return (value == null)?defaultValue:new String(value, ENCODING);
    }

    @Override
    public void finishNameIdMapping() {
        String key = (prefix + "cnf");
        jedis.hset(key,"nameIdMapped", "true");
    }

    @Override
    public boolean nameIdMapped() {
        String key = (prefix + "cnf");
        String value = jedis.hget(key,"nameIdMapped");
        return "true".equals(value);
    }

    /**
     * 为类别赋予深度depth, 如果一个节点没有depth值，则说明该节点不在类别树中，属于孤立节点
     */
    public void assignDepth() {
        //为类别赋予深度属性
        try {
            jedis.del(prefix + "id2depth");

            Set<String> skippedNames = new HashSet<>();
            String root = conf.getWikiRootCategoryName();
            int id = getIdByName(root);
            saveDepth(id, 0);

            Queue<Integer> queue = new LinkedList<>();
            queue.add(id);
            while (!queue.isEmpty()) {
                int pageId = queue.poll();
                int depth = getDepth(pageId);
                Set<Integer> childIds = getChildIds(pageId);
                for (Integer childId : childIds) {
                    if (getDepth(childId, -1) < 0) {
//                        String name = getNameById(childId);
//                        if (name.contains(" by ")) {
//                            //skip category like: Education by subject, People by company
//                            skippedNames.add(name);
//                            continue;
//                        }
                        saveDepth(childId, depth + 1);
                        queue.add(childId);
                    }
                }
            }

//            try {
//                FileWriter writer = new FileWriter("./skip_by.txt");
//                for (String n : skippedNames) {
//                    writer.write(n);
//                    writer.write("\n");
//                }
//                writer.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }

        } catch (MissedException e) {
            LOG.error(e.toString());
        }
    }


    @Override
    public void findCycles(String startCatName) throws MissedException {
        int startId = getIdByName(startCatName);
        Stack<Integer> visited = new Stack<>();
        visited.add(startId);
        dfs(startId, visited);
    }

    private void dfs(int catId, Stack<Integer> visitedIds) throws MissedException {
        Set<Integer> children = getChildIds(catId);
        for (int child : children) {
            if (visitedIds.contains(child)) {
                for (int i = 0; i < visitedIds.size(); i++) {
                    int id = visitedIds.get(i);
                    System.out.print(getNameById(id) + "/");
                }
                System.out.println(getNameById(child));
            } else {
                visitedIds.push(child);
                dfs(child, visitedIds);
            }
        }
        visitedIds.pop();
    }

    public void buildTreeGraph(File graphOutputFile) {
        String root = conf.getWikiRootCategoryName();
        //int id = getIdByName(root);

    }

    @Override
    public void saveCacheToGZipFile() throws IOException {

    }

    @Override
    public void buildCacheFromGZipFile() throws IOException {

    }

    @Override
    public void clearAll() {
        jedis.del(prefix + "name2id");
        jedis.del(prefix + "id2name");
        jedis.del(prefix + "id2depth");
        jedis.del(prefix + "id2cids");
        jedis.del(prefix + "id2pids");
        jedis.del(prefix + "id2aids");
        jedis.del(prefix + "id2ac");
        jedis.del(prefix + "cnf");
    }

    @Override
    public void done() {
        String key = (prefix + "cnf");
        jedis.hset(key, "status", "done");
    }

    @Override
    public boolean hasDone() {
        String key = (prefix + "cnf");
        String value = jedis.hget(key,"status");
        return "done".equals(value);
    }

}
