package ruc.irm.wikit.db.je;

import com.sleepycat.je.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.db.je.WDatabase.DatabaseType;
import ruc.irm.wikit.db.je.struct.PageRecord;

import java.io.File;
import java.util.HashMap;

/**
 * A wrapper for {@link Environment}, that keeps track of all of the databases required for a single dump of Wikipedia.
 * 
 *  It is unlikely that you will want to work with this class directly: use {@link Wikipedia} instead.
 */
public class WEnvironment {
    private static final Logger LOG = LoggerFactory.getLogger(WEnvironment.class);

    private Conf conf ;
	private Environment env ;
	private WDatabase<Integer, PageRecord> dbPage ;
	private WDatabase<String,Integer> dbArticlesByTitle;
    private WDatabase<String,Integer> dbCategoriesByTitle ;
    ;
	private WDatabase<Integer, Long> dbStatistics ;


	private HashMap<DatabaseType, WDatabase> databasesByType;

    /**
     * Open Berkeley DB environment with readonly mode.
     *
     * @param conf
     * @throws EnvironmentLockedException
     */
    public WEnvironment(Conf conf) throws
            EnvironmentLockedException {
        this(conf, false, true);
    }

    /**
     * When update database, use this constructor with allowCreate=true and
     * readOnly=false, otherwise please use WEnvironment(Conf conf)
     *
     */
	public WEnvironment(Conf conf, boolean allowCreate, boolean readOnly) throws
			EnvironmentLockedException {
		this.conf = conf;
		EnvironmentConfig envConf = new EnvironmentConfig() ;
		envConf.setCachePercent(10);
		envConf.setAllowCreate(allowCreate) ;
		envConf.setReadOnly(readOnly) ;

        System.out.println("berkeley db path:" + conf.get("berkeley.db.dir"));
        File envDir = new File(conf.get("berkeley.db.dir", "bdb"));
		envDir.mkdirs();

		env = new Environment(envDir, envConf) ;
		
		initDatabases();
	}

    public Conf getConf() {
        return this.conf;
    }

    private void initDatabases() {
        WDatabaseFactory dbFactory = new WDatabaseFactory(this) ;

        databasesByType = new HashMap<DatabaseType, WDatabase>() ;

        dbPage = dbFactory.buildPageDatabase();
        databasesByType.put(DatabaseType.page, dbPage);

        dbArticlesByTitle = dbFactory.buildTitleDatabase(DatabaseType.articlesByTitle);
        databasesByType.put(DatabaseType.articlesByTitle, dbArticlesByTitle);

        dbCategoriesByTitle = dbFactory.buildTitleDatabase(DatabaseType.categoriesByTitle);
        databasesByType.put(DatabaseType.categoriesByTitle, dbCategoriesByTitle);
    }
	

	private WDatabase getDatabase(DatabaseType dbType) {
		return databasesByType.get(dbType) ;
	}

    /**
     * Returns the {@link DatabaseType#page} database
     *
     * @return see {@link DatabaseType#page}
     */
    public WDatabase<Integer, PageRecord> getDbPage() {
        return dbPage;
    }

    public WDatabase<String, Integer> getDbArticlesByTitle() {
        return dbArticlesByTitle;
    }

    public WDatabase<String, Integer> getDbCategoriesByTitle() {
        return dbCategoriesByTitle;
    }

	/**
	 * Tidily closes the environment, and all databases within it. This should always be called once you are finished with the environment.
	 */
	public void close() {
		for (WDatabase db:this.databasesByType.values()) {
			db.close() ;
		}

        env.close();
	}

    public boolean isReady() {
        return true;
    }

    @Override
	public void finalize() {
		if (env != null) {
			LOG.warn("Unclosed enviroment. You may be causing a memory leak.");
		}
	}

    public void cleanAndCheckpoint() throws DatabaseException {
        LOG.info("Starting cleaning") ;
        boolean anyCleaned = false;
        while (env.cleanLog() > 0) {
            System.out.println("cleaning") ;
            anyCleaned = true;
        }
        LOG.info("Finished cleaning") ;

        if (anyCleaned) {
            LOG.info("Starting checkpoint") ;

            CheckpointConfig force = new CheckpointConfig();
            force.setForce(true);
            env.checkpoint(force);

            LOG.info("Finished checkpoint") ;
        }
    }

    public Environment getEnvironment() {
		return env ;
	}

}
