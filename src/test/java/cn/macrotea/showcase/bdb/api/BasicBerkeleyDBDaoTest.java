package cn.macrotea.showcase.bdb.api;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author macrotea@qq.com
 * @since 2014-8-8 下午9:18
 */
public class BasicBerkeleyDBDaoTest {

    BasicBerkeleyDBDao dao;

    @Before
    public void setUp() throws Exception {
        dao = new BasicBerkeleyDBDao();
    }

    @After
    public void tearDown() throws Exception {
        if (dao != null) {
            dao.close();
            dao = null;
        }
    }

    @Test
    @Ignore
    public void testAdd_override_allowDuplicatedKey_as_true() throws Exception {

        //FIXME macrotea@qq.com/2014-8-8 下午9:35 测试有问题

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

}
