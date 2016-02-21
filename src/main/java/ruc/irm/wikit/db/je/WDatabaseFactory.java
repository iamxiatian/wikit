package ruc.irm.wikit.db.je;

import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.bind.tuple.StringBinding;
import ruc.irm.wikit.db.je.WDatabase.DatabaseType;
import ruc.irm.wikit.db.je.struct.PageRecord;


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
	public WDatabase<Integer, PageRecord> buildPageDatabase() {

		RecordBinding<PageRecord> keyBinding = new RecordBinding<PageRecord>() {
			public PageRecord createRecordInstance() {
				return new PageRecord() ;
			}
		} ;

		return new WDatabase<Integer, PageRecord>(
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



