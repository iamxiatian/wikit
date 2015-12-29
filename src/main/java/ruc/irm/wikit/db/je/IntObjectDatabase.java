package ruc.irm.wikit.db.je;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.tuple.IntegerBinding;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * A {@link WDatabase} for associating Integer keys with some generic value type.
 *
 * @param <V> the type of object to store as values
 */
public abstract class IntObjectDatabase<V> extends WDatabase<Integer,V> {
	
	private TIntObjectHashMap<V> fastCache = null ;
	private TIntObjectHashMap<byte[]> compactCache = null ;
	
	/**
	 * Creates or connects to a database, whose name will match the given {@link WDatabase.DatabaseType}
	 * 
	 * @param env the WEnvironment surrounding this database
	 * @param type the type of database
	 * @param valueBinding a binding for serialising and de-serialising values
	 */
	public IntObjectDatabase(WEnvironment env, DatabaseType type,
							 EntryBinding<V> valueBinding) {
		super(env, type, new IntegerBinding(), valueBinding) ;
	}

}
