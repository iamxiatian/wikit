package ruc.irm.wikit.db.je;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import ruc.irm.wikit.data.dump.impl.PageXmlDump;
import ruc.irm.wikit.data.dump.parse.WikiPage;
import ruc.irm.wikit.db.je.struct.DbPage;
import ruc.irm.wikit.util.ProgressCounter;
import ruc.irm.wikit.util.ProgressTracker;

import java.io.*;


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

		return new IntObjectDatabase<DbPage>(
				env,
				WDatabase.DatabaseType.page,
				keyBinding
		) {
			@Override
			protected void loading(Database db, ProgressTracker tracker) throws IOException {
				PageXmlDump dump = new PageXmlDump(env.getConf());
				dump.open();

				ProgressCounter counter = new ProgressCounter();
				while (dump.hasNext()) {
					WikiPage wikiPage = dump.next();
					counter.increment();

					if(wikiPage.isArticle() || wikiPage.isCategory()) {
						int ns = Integer.parseInt(wikiPage.getNs());
						int id = wikiPage.getId();
						DbPage dbPage = new DbPage(id, wikiPage.getTitle(),
								ns, wikiPage.getText());

						DatabaseEntry k = new DatabaseEntry();
						keyBinding.objectToEntry(id, k);

						DatabaseEntry v = new DatabaseEntry();
						valueBinding.objectToEntry(dbPage, v);

						db.put(null, k, v);
					}

					//if(count++>100) break;
				}
				dump.close();
				System.out.println("Total count:" + counter.getCount());
				counter.done();
			}
		};
	}

}



