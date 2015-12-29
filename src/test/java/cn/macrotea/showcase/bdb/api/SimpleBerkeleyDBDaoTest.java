package cn.macrotea.showcase.bdb.api;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author macrotea@qq.com
 * @since 2014-8-8 下午8:13
 */
public class SimpleBerkeleyDBDaoTest {

    SimpleBerkeleyDBDao dao;

    @Before
    public void setUp() throws Exception {
        dao = new SimpleBerkeleyDBDao();
    }

    @After
    public void tearDown() throws Exception {
        if (dao != null) {
            dao.close();
            dao = null;
        }
    }

    @Test
    public void testAdd() throws Exception {

        //prepare
        dao.add("1", "one");
        String found = dao.find("1");

        //check
        assertEquals(found, "one");

        //clear
        boolean deleteResult = dao.delete("1");
        String result = dao.find("1");

        //check again
        assertTrue(deleteResult);
        assertNull(result);
    }

    @Test
    public void testAdd_override() throws Exception {

        //prepare
        boolean addResult1 = dao.add("1", "one");
        boolean addResult2 = dao.add("1", "oneone");
        String found = dao.find("1");

        //check
        assertTrue(addResult1);
        assertTrue(addResult2);
        assertEquals(found, "oneone");

        //clear
        boolean deleteResult = dao.delete("1");
        String result = dao.find("1");

        //check again
        assertTrue(deleteResult);
        assertNull(result);
    }


    @Test(timeout = 10000)
    public void testAdd_big_data() throws Exception {

        //1万条数据压力测试
        //FIXME macrotea@qq.com/2014-8-8 下午9:05 我这破机插入1w条数据要10秒? 尴尬!! 看来要换电脑了

        Database db = dao.getDatabase();
        for (int i = 0; i < 10000; i++) {
            byte[] index = String.valueOf(i).getBytes("utf-8");

            DatabaseEntry keyEntry = new DatabaseEntry(index);
            DatabaseEntry valEntry = new DatabaseEntry(index);

            db.put(null, keyEntry, valEntry);
        }
    }

    @Test
    public void testUpdate() throws Exception {
        //prepare
        boolean addResult = dao.add("1", "one");
        String newValue = dao.update("1", "oneone");
        String found = dao.find("1");

        //check
        assertTrue(addResult);
        assertEquals(found, "oneone");
        assertEquals(newValue, found);

        //clear
        boolean deleteResult = dao.delete("1");
        String result = dao.find("1");

        //check again
        assertTrue(deleteResult);
        assertNull(result);
    }

    @Test
    public void testFind() throws Exception {
        // same ass testAdd
    }

    @Test
    public void testFind_not_exists_key() throws Exception {
        String found = dao.find("not_exists_key");
        assertNull(found);
    }

    @Test
    public void testFindByKeyOrValue() throws Exception {
        //prepare
        boolean addResult = dao.add("1", "one");
        String found1 = dao.find("1");
        String found2 = dao.findByKeyAndValue("1", "one");

        //check
        assertTrue(addResult);
        assertEquals(found1, "one");
        assertEquals(found2, "one");

        //clear
        boolean deleteResult = dao.delete("1");
        String result = dao.find("1");

        //check again
        assertTrue(deleteResult);
        assertNull(result);
    }

    @Test
    public void testDelete() throws Exception {
        //prepare
        boolean addResult = dao.add("1", "one");

        //check
        assertTrue(addResult);

        boolean deleteResult = dao.delete("1");
        String result = dao.find("1");

        //check
        assertTrue(deleteResult);
        assertNull(result);
    }

    @Test
    public void testDelete_not_exists() throws Exception {
        //do
        boolean deleteResult = dao.delete("not_exists_key");

        //check
        assertFalse(deleteResult);
    }
}
