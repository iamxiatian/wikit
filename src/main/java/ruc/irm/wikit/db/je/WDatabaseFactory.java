package ruc.irm.wikit.db.je;

import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.bind.tuple.StringBinding;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import ruc.irm.wikit.data.dump.impl.PageXmlDump;
import ruc.irm.wikit.data.dump.parse.WikiPage;
import ruc.irm.wikit.db.je.WDatabase.DatabaseType;
import ruc.irm.wikit.db.je.struct.DbPage;
import ruc.irm.wikit.util.ProgressCounter;
import ruc.irm.wikit.util.ProgressTracker;

import java.io.*;
import java.util.List;


/**
 * A factory for creating WDatabases of various types
 */
public class WDatabaseFactory {
	private WEnvironment env;

	public WDatabaseFactory(WEnvironment env) {
		this.env = env;
	}

	/**
	 * Returns a database associating page ids with the title, type and generality of the page. 
	 * 
	 * @return a database associating page ids with the title, type and generality of the page. 
	 */
	public WDatabase<Integer, DbPage> buildPageDatabase() {

		RecordBinding<DbPage> keyBinding = new RecordBinding<DbPage>() {
			public DbPage createRecordInstance() {
				return new DbPage() ;
			}
		} ;

		return new WDatabase<Integer, DbPage>(
				env,
				DatabaseType.page,
				new IntegerBinding(),
				keyBinding
		);
	}

	public WDatabase<String, Integer> buildTitleDatabase(DatabaseType type) {
		if (type != DatabaseType.articlesByTitle && type != DatabaseType.categoriesByTitle)
			throw new IllegalArgumentException("type must be either DatabaseType.articlesByTitle or DatabaseType.categoriesByTitle") ;

		return new WDatabase<String, Integer>(
				env,
				type,
				new StringBinding(),
				new IntegerBinding()
		);
	}
}



