package cn.macrotea.showcase.bdb.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author macrotea@qq.com
 * @since 2014-8-8 下午7:03
 */
public class SimpleBerkeleyDBDao extends BerkeleyDBTemplate {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public String getEnvFilePath() {
        return "showcase-bdb-data";
    }

    @Override
    public String getDatabaseName() {
        return "db_showcase";
    }

    @Override
    public Boolean allowDuplicatedKey() {
        return false;
    }


}
