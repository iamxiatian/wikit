package ruc.irm.wikit.espm.graph;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import ruc.irm.wikit.cache.Cache;
import ruc.irm.wikit.cache.CategoryCache;
import ruc.irm.wikit.cache.impl.CategoryCacheRedisImpl;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.exception.MissedException;
import ruc.irm.wikit.common.exception.WikitException;
import ruc.irm.wikit.esa.concept.ConceptCache;
import ruc.irm.wikit.esa.concept.ConceptCacheRedisImpl;
import ruc.irm.wikit.model.Category;
import ruc.irm.wikit.util.NumberUtils;
import ruc.irm.wikit.util.ProgressCounter;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tree like grapth for category implemented on Redis memory database
 *
 * @author Tian Xia <a href="mailto:xiat@ruc.edu.cn">xiat@ruc.edu.cn</a>
 * @date Aug 03, 2015 12:41 PM
 */
public class CategoryTreeGraphRedisImpl implements CategoryTreeGraph,
        Cache, Closeable{
    private Logger LOG = LoggerFactory.getLogger(CategoryTreeGraphRedisImpl.class);

    private Conf conf;
    private String prefix = "";
    private Jedis jedis = null;
    private int rootId;

    public CategoryTreeGraphRedisImpl(Conf conf) throws MissedException {
        this.conf = conf;
        this.prefix = conf.get("category.redis.prefix", "");
        this.prefix += "tg:";
        this.jedis = new Jedis(conf.getRedisHost(), conf.getRedisPort(), conf.getRedisTimeout());

        if (hasDone()) {
            this.rootId = getIdByName(conf.getWikiRootCategoryName());
        }
    }

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

    private void saveParents(int catId, Collection<String> parents) {
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

    private void saveParents(int catId, int... parentIds) {
        byte[] key = (prefix + "id2pids").getBytes(ENCODING);
        byte[] hkey = NumberUtils.int2Bytes(catId);

        byte[] value = jedis.hget(key, hkey);
        Set<Integer> ids = NumberUtils.bytes2IntSet(value);

        boolean changed = false;
        for (int p: parentIds) {
            if(idExist(p)) {
                ids.add(p);
                changed = true;
            }
        }
        if(changed)
            jedis.hset(key, hkey, NumberUtils.intSet2Bytes(ids));
    }

    private void saveChildren(int catId, String... children) {
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

    private void saveChildren(int catId, int... childIds) {
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
    public Set<Integer> getParentIds(int catId) {
        byte[] key = (prefix + "id2pids").getBytes(ENCODING);
        byte[] hkey = NumberUtils.int2Bytes(catId);

        byte[] value = jedis.hget(key, hkey);
        Set<Integer> ids = NumberUtils.bytes2IntSet(value);

        //remove skipped categories:
        //35321838 Wikipedia categories named after scientific buildings
        //35321841 Wikipedia categories named after scientific organizations
        //35321847 Wikipedia categories named after scientists
        ids.remove(35321838);
        ids.remove(35321841);
        ids.remove(35321847);
        return ids;
    }

    @Override
    public Set<String> getParentNames(String catName) {
        Set<String> names = new HashSet<>();
        for (int id : getParentIds(getIdByName(catName, 0))) {
            names.add(getNameById(id));
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
        Set<String> names = new HashSet<>();
        try {
            Set<Integer> ids = getChildIds(getIdByName(catName));
            for (int id : ids) {
                if(idExist(id))
                    names.add(getNameById(id));
            }
        } catch (MissedException e) {
            LOG.error("get child names error:", e);
        }

        return names;
    }

    @Override
    public void saveConceptRelation(int catId, int conceptId) {
        byte[] key = (prefix + "id2aids").getBytes(ENCODING);
        byte[] hkey = NumberUtils.int2Bytes(catId);
        byte[] value = jedis.hget(key, hkey);
        Set<Integer> ids = NumberUtils.bytes2IntSet(value);

        if (!ids.contains(conceptId)) {
            ids.add(conceptId);
            jedis.hset(key, hkey, NumberUtils.intSet2Bytes(ids));
        }

        //记录该类别下概念的数量
        incConceptCount(catId, 1);

        //记录概念所隶属的类别
        key = (prefix + "ctp:catids").getBytes(ENCODING);
        hkey = NumberUtils.int2Bytes(conceptId);
        value = jedis.hget(key, hkey);
        ids = NumberUtils.bytes2IntSet(value);
        if(!ids.contains(catId)) {
            ids.add(catId);
            jedis.hset(key, hkey, NumberUtils.intSet2Bytes(ids));
        }
    }

    @Override
    public Set<Integer> getConceptIds(int catId) {
        byte[] key = (prefix + "id2aids").getBytes(ENCODING);
        byte[] hkey = NumberUtils.int2Bytes(catId);

        byte[] value = jedis.hget(key, hkey);
        return NumberUtils.bytes2IntSet(value);
    }

    public void incConceptCount(int catId) {
        incConceptCount(catId, 1);
    }

    public void incConceptCount(int catId, int count) {
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
    public int getConceptCount(int catId) {
        byte[] key = (prefix + "id2ac").getBytes(ENCODING);
        byte[] hkey = NumberUtils.int2Bytes(catId);

        byte[] value = jedis.hget(key, hkey);
        return (value == null) ? 0 : NumberUtils.bytes2Int(value);
    }

    private void incRecursiveConceptCount(int catId, int count) {
        byte[] key = (prefix + "id2acr").getBytes(ENCODING);
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
    public int getRecursiveConceptCount(int catId) {
        byte[] key = (prefix + "id2acr").getBytes(ENCODING);
        byte[] hkey = NumberUtils.int2Bytes(catId);

        byte[] value = jedis.hget(key, hkey);
        return (value == null) ? 0 : NumberUtils.bytes2Int(value);
    }

    @Override
    public Set<Integer> getCategoryIdsByConceptId(int conceptId) {
        byte[] key = (prefix + "ctp:catids").getBytes(ENCODING);
        byte[] hkey = NumberUtils.int2Bytes(conceptId);
        byte[] value = jedis.hget(key, hkey);
        return NumberUtils.bytes2IntSet(value);
    }

    private void saveDepth(int catId, int depth) {
        byte[] key = (prefix + "id2depth").getBytes(ENCODING);
        byte[] hkey = NumberUtils.int2Bytes(catId);

        jedis.hset(key, hkey, NumberUtils.int2Bytes(depth));
    }

    @Override
    public int getDepth(int catId) {
        byte[] key = (prefix + "id2depth").getBytes(ENCODING);
        byte[] hkey = NumberUtils.int2Bytes(catId);
        byte[] value = jedis.hget(key, hkey);
        return (value == null) ? -1 : NumberUtils.bytes2Int(value);
    }

    @Override
    public Category getCategory(String catTitle) throws MissedException {
        int pageId = getIdByName(catTitle);

        Category c = new Category();
        c.setPageId(pageId);
        c.setTitle(catTitle);
        c.setParentIds(getParentIds(pageId));
        c.setArticleIds(getConceptIds(pageId));
        c.setArticleCount(getConceptCount(pageId));
        return c;
    }


    @Override
    public Category getCategory(int pageId) throws MissedException {
        String catTitle = getNameById(pageId);
        Category c = new Category();
        c.setPageId(pageId);
        c.setTitle(catTitle);
        c.setParentIds(getParentIds(pageId));
        c.setArticleIds(getConceptIds(pageId));
        c.setArticleCount(getConceptCount(pageId));

        return c;
    }

    /**
     * find category articles distribution, each output line like the following format:
     * [article count] \t [the category count which contains this specified article count]
     */
    public void displayCategoryConceptDistribution() throws MissedException {
        Collection<String> categories = listNames();
        Bag<Integer> catDistBag = new HashBag<>();

        ProgressCounter counter = new ProgressCounter();
        for (String c : categories) {
            counter.increment();
            int articleCount = getConceptCount(getIdByName(c));
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
    public String getNameById(int pageId) {
        byte[] key = (prefix + "id2name").getBytes(ENCODING);
        byte[] value = jedis.hget(key, NumberUtils.int2Bytes(pageId));
        if (value == null) {
            return "Not Exist:" + pageId;
        }
        return new String(value, ENCODING);
    }

    @Override
    public String getNameById(int id, String defaultValue) {
        byte[] key = (prefix + "id2name").getBytes(ENCODING);
        byte[] value = jedis.hget(key, NumberUtils.int2Bytes(id));
        return (value==null)?defaultValue:new String(value, ENCODING);
    }

    @Override
    public void finishNameIdMapping() {
        String key = (prefix + "cnf");
        jedis.hset(key,"nameIdMapped", "true");
    }

    @Override
    public boolean nameIdMapped() {
        String key = (prefix + "cnf");
        String value = jedis.hget(key, "nameIdMapped");
        return "true".equals(value);
    }

    @Override
    public boolean isChild(String parent, String child) {
        int parentId = getIdByName(parent, -1);
        int childId = getIdByName(child, -1);
        return isChild(parentId, childId);
    }

    @Override
    public boolean isChild(int parentId, int childId) {
        Set<Integer> parentIds = getParentIds(childId);
        return parentIds.contains(parentId);
    }

    @Override
    public Set<String> getLevelOneCategoryNames() {
        return getLevelOneCategoryIds().stream()
                .map(id -> getNameById(id))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Integer> getLevelOneCategoryIds() {
        return getChildIds(rootId);
    }

    @Override
    public LinkedList<Integer> getLeafCategoryIds() {
        LinkedList<Integer> ids = new LinkedList<>();
        for (int id : listIds()) {
            if(getChildCount(id)==0) {
                ids.add(id);
            }
        }
        return ids;
    }

    @Override
    public int getChildCount(String catName) {
        int catId = getIdByName(catName, -1);
        return getChildIds(catId).size();
    }

    @Override
    public int getChildCount(int catId) {
        return getChildIds(catId).size();
    }

    /**
     * childPathList里面的每个元素为一个Stack<Integer>，栈顶存放了路径的起始节点，栈底则
     * 存放了终止节点, 该方法用于getPaths(catId)之中
     *
     * @param childPathList
     */
    private List<Stack<Integer>> walkUp(List<Stack<Integer>> childPathList) {
        List<Stack<Integer>> results = new LinkedList<>();
        for (Stack<Integer> subPath : childPathList) {
            int currentId = subPath.peek();
            int depth = getDepth(currentId);

            if (depth <= 1) {
                results.add(subPath);
            } else {
                Set<Integer> parentIds = getParentIds(currentId);
                for (int parentId : parentIds) {
                    Stack<Integer> path = (Stack<Integer>) subPath.clone();
                    path.push(parentId);
                    results.add(path);
                }
            }
        }

        return results;
    }

    @Override
    public List<String> getPaths(int catId) {
        int depth = getDepth(catId);
        List<Stack<Integer>> idPaths = new LinkedList<>();
        Stack<Integer> stack = new Stack<>();
        stack.add(catId);
        idPaths.add(stack);

        for (int i = depth; i > 1; i--) {
            idPaths = walkUp(idPaths);
        }

        //idPaths里面的每条Path都是存放在栈中的以id表示的路径，下面转换为字符串形式
        List<String> list = new LinkedList<>();
        for (Stack<Integer> path : idPaths) {
            StringBuilder sb = new StringBuilder();
            while (!path.isEmpty()) {
                int id = path.pop();
                String name = getNameById(id);
                int conceptCount = getConceptCount(id);
                sb.append("/").append(name + "(" + conceptCount + ")");
            }
            list.add(sb.toString());
        }
        return list;
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
        jedis.del(prefix + "id2acr");
        jedis.del(prefix + "ctp:catids");
        jedis.del(prefix + "cnf");
    }


    private void edgeRelationCreated() {
        String key = (prefix + "cnf");
        jedis.hset(key, "edgeRelation", "created");
    }

    @Override
    public boolean hasEdgeRelationCreated() {
        String key = (prefix + "cnf");
        String value = jedis.hget(key,"edgeRelation");
        return "created".equals(value);
    }

    private void conceptRelationCreated() {
        String key = (prefix + "cnf");
        jedis.hset(key, "cptRelation", "created");
    }

    @Override
    public boolean hasConceptRelationCreated() {
        String key = (prefix + "cnf");
        String value = jedis.hget(key,"cptRelation");
        return "created".equals(value);
    }

    @Override
    public void done() {
        String key = (prefix + "cnf");
        jedis.hset(key,"status", "done");
    }

    @Override
    public boolean hasDone() {
        String key = (prefix + "cnf");
        String value = jedis.hget(key,"status");
        return "done".equals(value);
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public void buildEdgeRelation(CategoryCache categoryCache) throws WikitException {
        if (!categoryCache.hasDone()) {
            throw new WikitException("Please create category cache first.");
        }
        String[] skippedCategories = new String[]{"跨学科领域", "总类", "词汇列表"};
        Set<Integer> skippedCatIds = new HashSet<>();
        for (String c : skippedCategories) {
            skippedCatIds.add(categoryCache.getIdByName(c));
        }

        ProgressCounter counter = new ProgressCounter();
        try {
            String root = conf.getWikiRootCategoryName();
            int id = categoryCache.getIdByName(root);

            this.saveNameIdMapping(root, id);
            this.rootId = id;
            this.saveDepth(id, 0);

            Queue<Integer> queue = new LinkedList<>();
            queue.add(id);
            while (!queue.isEmpty()) {
                int pid = queue.poll();
                int depth = this.getDepth(pid);
                Set<Integer> childIds = categoryCache.getChildIds(pid);

                for (Integer childId : childIds) {
                    if (skippedCatIds.contains(childId)) {
                        continue;
                    }

                    int childDepth = categoryCache.getDepth(childId, -1);
                    if (childDepth != (depth + 1)) {
                        continue; //skip
                    }

                    if (!idExist(childId)) {
                        String name = categoryCache.getNameById(childId);
                        this.saveNameIdMapping(name, childId);
                        this.saveDepth(childId, depth + 1);

                        queue.add(childId);
                    }
                    this.saveParents(childId, pid);
                    this.saveChildren(pid, childId);
                }

                counter.increment();
            }
            this.finishNameIdMapping();
            this.edgeRelationCreated();
        } catch (MissedException e) {
            LOG.error(e.toString());
            throw new WikitException(e);
        }

        counter.done();
        LOG.info("Edge relation has created.");
    }

    @Override
    public void buildConceptRelation(ConceptCache conceptCache) throws WikitException {
        if (!hasEdgeRelationCreated()) {
            throw new WikitException("please create edge relation first.");
        }
        ProgressCounter counter = new ProgressCounter();

        Set<Integer> conceptIds = conceptCache.listIds();
        for (int cid : conceptIds) {
            Collection<String> catNames = conceptCache.getCategoriesById(cid);
            for (String name : catNames) {
                int catId = getIdByName(name, -1);
                if (catId < 0) continue;

                saveConceptRelation(catId, cid);
            }
            counter.increment();
        }
        conceptRelationCreated();
        counter.done();
        LOG.info("concept-category relation has created.");
    }

    @Override
    public void buildRecursiveCountInfo() throws WikitException {
        if (!hasConceptRelationCreated()) {
            throw new WikitException("Category-concept relation hasn't created.");
        }

        Collection<Integer> leafIds = getLeafCategoryIds();
        ProgressCounter progress = new ProgressCounter();
        progress.setMaxCount(leafIds.size());
        for (int catId : leafIds) {
            int cptCount = getConceptCount(catId);
            incRecursiveConceptCount(catId, cptCount);

            //visit all ancestor node, and inc recursive concept count
            Queue<Integer> queue = new LinkedList<>();
            queue.add(catId);
            while (!queue.isEmpty()) {
                int childId = queue.poll();
                Set<Integer> parentIds = getParentIds(childId);
                for (int pid : parentIds) {
                    incRecursiveConceptCount(pid, cptCount);
                    queue.add(pid);
                }
            }
            progress.increment();
        }

        String key = (prefix + "cnf");
        jedis.hset(key, "assignRecursiveCount", "created");
        progress.done();
    }

    @Override
    public void build() throws WikitException {
        clearAll();
        buildEdgeRelation(new CategoryCacheRedisImpl(conf));
        buildConceptRelation(new ConceptCacheRedisImpl(conf));
        buildRecursiveCountInfo();
        this.done();
        LOG.info("DONE for build tree graph!");
    }
}
