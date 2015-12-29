package cn.macrotea.showcase.bdb.api;

import cn.macrotea.showcase.bdb.api.exception.BDBDataAccessException;
import com.sleepycat.je.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.UnsupportedEncodingException;

/**
 * @author macrotea@qq.com
 * @since 2014-8-8 下午7:03
 */
public abstract class BerkeleyDBTemplate {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    protected EnvironmentConfig envConfig = null;

    protected Environment environment = null;

    protected DatabaseConfig dbConfig = null;

    protected Database database = null;

    public abstract String getEnvFilePath();

    public abstract String getDatabaseName();

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

    public Database getDatabase() {

        if (database == null) {
            database = getEnvironment().openDatabase(null, getDatabaseName(), getDatabaseConfig());
        }

        return database;
    }

    public void close() {
        if (getDatabase().getConfig().getDeferredWrite()) {
            getDatabase().sync();
        }
        getDatabase().close();
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
    public boolean add(String key, String value) {
        OperationStatus status;
        try {
            DatabaseEntry keyEntry = new DatabaseEntry(asBytes(key));
            DatabaseEntry valEntry = new DatabaseEntry(asBytes(value));

            if (allowDuplicatedKey()) {
                status = getDatabase().putNoOverwrite(null, keyEntry, valEntry);
            } else {
                status = getDatabase().put(null, keyEntry, valEntry);
            }

            return status != null && status == OperationStatus.SUCCESS;

        } catch (Exception e) {
            BDBDataAccessException.throwMe(String.format("根据 key=%s , value = %s 插入失败", key, value), e);
        }

        return false;
    }

    /**
     * 更新且返回新值
     *
     * @param key
     * @param value
     * @return
     */
    public String update(String key, String value) {
        try {
            DatabaseEntry keyEntry = new DatabaseEntry(asBytes(key));
            DatabaseEntry valEntry = new DatabaseEntry(asBytes(value));
            DatabaseEntry valueToFetchEntry = new DatabaseEntry();

            OperationStatus status = getDatabase().put(null, keyEntry, valEntry);
            status = getDatabase().get(null, keyEntry, valueToFetchEntry, LockMode.DEFAULT);

            return status == OperationStatus.SUCCESS ? new String(valueToFetchEntry.getData(), getDataEncoding()) : null;
        } catch (Exception e) {
            BDBDataAccessException.throwMe(String.format("根据 key=%s , value = %s 更新失败", key, value), e);
        }

        return null;
    }

    /**
     * 查找
     *
     * @param key
     * @return
     */
    public String find(String key) {
        try {
            DatabaseEntry keyEntry = new DatabaseEntry(asBytes(key));
            DatabaseEntry valueEntry = new DatabaseEntry();

            OperationStatus status = getDatabase().get(null, keyEntry, valueEntry, LockMode.DEFAULT);

            return status == OperationStatus.SUCCESS ? new String(valueEntry.getData(), getDataEncoding()) : null;
        } catch (Exception e) {
            BDBDataAccessException.throwMe(String.format("根据 key=%s 查找失败", key), e);
        }
        return null;
    }

    /**
     * 根据键和值查找
     *
     * @param key
     * @param value
     * @return
     */
    public String findByKeyAndValue(String key, String value) {
        try {
            DatabaseEntry keyEntry = new DatabaseEntry(asBytes(key));
            DatabaseEntry valueEntry = new DatabaseEntry(asBytes(value));

            OperationStatus status = getDatabase().getSearchBoth(null, keyEntry, valueEntry, LockMode.DEFAULT);

            return status == OperationStatus.SUCCESS ? new String(valueEntry.getData(), getDataEncoding()) : null;
        } catch (Exception e) {
            BDBDataAccessException.throwMe(String.format("根据 key=%s value=%s 查找失败", key, value), e);
        }
        return null;
    }

    /**
     * 删除
     *
     * @param key
     * @return
     */
    public boolean delete(String key) {
        try {
            OperationStatus status = getDatabase().delete(null, new DatabaseEntry(asBytes(key)));
            return status == OperationStatus.SUCCESS;
        } catch (Exception e) {
            BDBDataAccessException.throwMe(String.format("根据 key=%s 删除失败", key), e);
        }
        return false;
    }

    /**
     * String -> byte[]
     *
     * @param text
     * @return
     * @throws UnsupportedEncodingException
     */
    private byte[] asBytes(String text) throws UnsupportedEncodingException {
        if (StringUtils.isBlank(text)) {
            return new byte[0];
        }
        return text.getBytes(getDataEncoding());
    }

    /**
     * 获得数据编码
     *
     * @return
     */
    private String getDataEncoding() {
        return "utf-8";
    }
}
