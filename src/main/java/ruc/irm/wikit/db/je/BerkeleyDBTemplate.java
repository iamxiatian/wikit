package ruc.irm.wikit.db.je;

import cn.macrotea.showcase.bdb.api.exception.BDBDataAccessException;
import com.sleepycat.bind.EntryBinding;
import com.sleepycat.je.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 *
 * The first edition of this class comes from:
 * https://github.com/macrotea/berkeleydb-je-showcase
 *
 * @author macrotea@qq.com
 * @since 2014-8-8 下午7:03
 */
public abstract class BerkeleyDBTemplate<K, V> {
    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    protected EntryBinding<K> keyBinding ;
    protected EntryBinding<V> valueBinding ;

    protected EnvironmentConfig envConfig = null;

    protected Environment environment = null;

    protected DatabaseConfig dbConfig = null;

    private String name;
    private Database database = null;
    private boolean readOnly = true;

    public BerkeleyDBTemplate(String name, EntryBinding<K> keyBinding,
                              EntryBinding<V> valueBinding) {
        this.name = name;

        this.keyBinding = keyBinding ;
        this.valueBinding = valueBinding ;

        this.database = null ;
    }

    public abstract String getEnvFilePath();

    protected EnvironmentConfig getEnvironmentConfig() {
        if (envConfig == null) {
            envConfig = new EnvironmentConfig();
            envConfig.setTransactional(true);
            envConfig.setAllowCreate(true);
        }

        return envConfig;
    }

    protected Environment getEnvironment() {

        if (environment == null) {
            environment = new Environment(getEnvHomeFile(), getEnvironmentConfig());
        }

        return environment;
    }

    protected File getEnvHomeFile() {
        File envHome = new File(getEnvFilePath());
        if (!envHome.exists()) {
            envHome.mkdirs();
        }
        return envHome;
    }

    protected DatabaseConfig getDatabaseConfig() {
        if (dbConfig == null) {
            dbConfig = new DatabaseConfig();
            dbConfig.setAllowCreate(true);
            dbConfig.setTransactional(true);
            // 设置一个key是否允许存储多个值
            dbConfig.setSortedDuplicates(allowDuplicatedKey());
        }

        return dbConfig;
    }

    protected Boolean allowDuplicatedKey() {
        return false;
    }

    protected Database getDatabase() throws DatabaseException {
        DatabaseConfig conf = new DatabaseConfig() ;

        conf.setReadOnly(readOnly) ;
        conf.setAllowCreate(!readOnly) ;
        conf.setExclusiveCreate(!readOnly) ;

        database = getEnvironment().openDatabase(null, name, conf);
        return database ;
    }


    public void close() {
        if (database != null) {
            database.close() ;
            database = null ;
        }

        getEnvironment().sync();
        getEnvironment().cleanLog();
        getEnvironment().close();
    }

    /**
     * 添加
     * 说明:
     * 1.若支持重复键则不覆盖现有数据
     * 1.若不支持重复键则覆盖更新已有数据
     *
     * @param key
     * @param value
     * @return
     */
    public boolean add(K key, V value) {
        try {
            if(readOnly) {
                throw new IOException("Can not add record for read only" +
                        " database");
            }
            DatabaseEntry keyEntry = new DatabaseEntry();
            keyBinding.objectToEntry(key, keyEntry);

            DatabaseEntry valEntry = new DatabaseEntry();
            valueBinding.objectToEntry(value, valEntry);

            OperationStatus status = getDatabase().put(null, keyEntry, valEntry);

            return status != null && status == OperationStatus.SUCCESS;
        } catch (Exception e) {
            BDBDataAccessException.throwMe(String.format("根据 key=%s , value = %s 插入失败", key, value), e);
        }

        return false;
    }

    /**
     * true if there is a persistent database underlying this, otherwise false
     *
     * @return true if there is a persistent database underlying this, otherwise false
     */
    public boolean exists() {
        try {
            getDatabase();
        } catch(DatabaseNotFoundException e) {
            return false ;
        }
        return true ;
    }

    /**
     * Retrieves the value associated with the given key, either from the persistent database, or from memory if
     * the database has been cached. This will return null if the key is not found, or has been excluded from the cache.
     *
     * @param key the key to search for
     * @return the value associated with the given key, or null if none exists.
     */
    public V retrieve(K key) {

        //System.out.println("d") ;
        Database db = getDatabase();

        DatabaseEntry dbKey = new DatabaseEntry() ;
        keyBinding.objectToEntry(key, dbKey) ;

        DatabaseEntry dbValue = new DatabaseEntry() ;

        OperationStatus os = db.get(null, dbKey, dbValue, LockMode.READ_COMMITTED) ;

        if (!os.equals(OperationStatus.SUCCESS))
            return null ;
        else
            return valueBinding.entryToObject(dbValue) ;

    }

}
