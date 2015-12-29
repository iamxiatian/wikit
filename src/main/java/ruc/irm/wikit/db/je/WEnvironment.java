package ruc.irm.wikit.db.je;

import com.sleepycat.je.*;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.db.je.WDatabase.DatabaseType;
import ruc.irm.wikit.db.je.struct.DbPage;

import java.io.File;
import java.io.IOException;
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
	private WDatabase<Integer, DbPage> pageDatabase ;
	private WDatabase<String,Integer> dbArticlesByTitle ;
	private WDatabase<Integer, Long> dbStatistics ;


	private HashMap<DatabaseType, WDatabase> databasesByType;

	public WEnvironment(Conf conf) throws
			EnvironmentLockedException {
		this.conf = conf;
		EnvironmentConfig envConf = new EnvironmentConfig() ;
		envConf.setCachePercent(10);
		envConf.setAllowCreate(true) ;
		envConf.setReadOnly(false) ;

		File envDir = new File(conf.get("berkeley.db.dir"), "bdb");
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

        pageDatabase = dbFactory.buildPageDatabase();
        databasesByType.put(DatabaseType.page, pageDatabase);
    }
	

	private WDatabase getDatabase(DatabaseType dbType) {
		return databasesByType.get(dbType) ;
	}

    /**
     * Returns the {@link DatabaseType#page} database
     *
     * @return see {@link DatabaseType#page}
     */
    public WDatabase<Integer, DbPage> getPageDatabase() {
        return pageDatabase;
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

    protected void cleanAndCheckpoint() throws DatabaseException {
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

    public static void buildEnvironment(Conf conf, boolean overwrite) throws IOException {
        WEnvironment env = new WEnvironment(conf);

        env.pageDatabase.loadData(overwrite, null);

        env.close();
    }

    public static void main(String[] args) throws ParseException, IOException {
        String helpMsg = "usage: WEnvironment -c config.xml -overwrite true";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("c", true, "config file"));
        options.addOption(new Option("overwrite", true, "true or false"));

        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("c") || !commandLine.hasOption("overwrite")) {
            helpFormatter.printHelp(helpMsg, options);
            return;
        }

        Conf conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);
        boolean overwrite = BooleanUtils.toBoolean(commandLine.getOptionValue("overwrite"));
        WEnvironment.buildEnvironment(conf, overwrite);
        System.out.println("I'm DONE for create WEnvironment!");
    }
}
