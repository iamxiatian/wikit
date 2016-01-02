package ruc.irm.wikit.db.je;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.je.*;
import gnu.trove.map.hash.THashMap;

/**
 * @author Tian Xia
 * @date Dec 25, 2015 11:29 AM
 */
public class WDatabase<K,V> {

    /**
     * Database types
     */
    public enum DatabaseType
    {
        /**
         * Associates page ids with the title, type and generality of the page.
         */
        page,

        /**
         * Associates String labels with the statistics about the articles (senses) these labels could refer to
         */
        label,

        /**
         * Associates String titles with the id of the page within the article namespace that this refers to
         */
        articlesByTitle,

        /**
         * Associates String titles with the id of the page within the category namespace that this refers to
         */
        categoriesByTitle,
    }



    /**
     * Options for caching data to memory
     */
    public enum CachePriority {

        /**
         * Focus on speed, by storing values directly
         */
        speed,

        /**
         * Focus on memory, by compressing values before storing them.
         */
        space
    }


    private boolean isCached = false ;
    private CachePriority cachePriority = CachePriority.space ;

    private THashMap<K,byte[]> compactCache = null ;
    private THashMap<K,V> fastCache = null ;

    private String name ;
    private DatabaseType type ;

    private WEnvironment env ;
    private Database database ;
    protected EntryBinding<K> keyBinding ;
    protected EntryBinding<V> valueBinding ;

    public WDatabase(WEnvironment env, DatabaseType type, EntryBinding<K>
            keyBinding, EntryBinding<V> valueBinding) {
        this.type = type ;
        this.name = type.name() ;

        this.keyBinding = keyBinding ;
        this.valueBinding = valueBinding ;

        this.env = env;
        this.database = null ;
    }

    public Database open(boolean readOnly) throws DatabaseException {
        DatabaseConfig conf = new DatabaseConfig() ;

        conf.setReadOnly(readOnly) ;
        conf.setAllowCreate(!readOnly) ;
        conf.setExclusiveCreate(!readOnly) ;

        if (database != null) {
            if (database.getConfig().getReadOnly() == readOnly) {
                //the database is already open as it should be.
                return database ;
            } else {
                //the database needs to be closed and re-opened.
                database.close();
            }
        }

        if (!readOnly) {
            try {
                env.getEnvironment().removeDatabase(null, name) ;
            } catch (DatabaseNotFoundException e) {} ;
        }

        database = env.getEnvironment().openDatabase(null, name, conf);
        return database ;
    }

    /**
     * true if there is a persistent database underlying this, otherwise false
     *
     * @return true if there is a persistent database underlying this, otherwise false
     */
    public boolean exists() {
        try {
            open(true) ;
        } catch(DatabaseNotFoundException e) {
            return false ;
        }
        return true ;
    }

    /**
     * Closes the underlying database
     */
    public void close() {

        if (database != null) {
            database.close() ;
            database = null ;
        }

        fastCache = null ;
        compactCache = null ;
    }

    /**
     * Returns the type of this database
     *
     * @return the type of this database
     */
    public DatabaseType getType() {
        return type ;
    }

    /**
     * Returns the name of this database
     *
     * @return the name of this database
     */
    public String getName() {
        return name ;
    }


    /**
     * Returns the number of entries that have been cached to memory
     *
     * @return the number of entries that have been cached to memory
     */
    public long getCacheSize() {
        if (!isCached)
            return 0 ;

        if (cachePriority == CachePriority.space)
            return fastCache.size();
        else
            return compactCache.size();
    }

    /**
     * Returns true if this has been cached to memory, otherwise false
     *
     * @return true if this has been cached to memory, otherwise false
     */
    public boolean isCached() {
        return isCached ;
    }

    /**
     * Returns whether this has been cached for speed or memory efficiency
     *
     * @return whether this has been cached for speed or memory efficiency
     */
    public CachePriority getCachePriority() {
        return cachePriority ;
    }

    public WDatabase<K, V> put(K key, V value) {
        DatabaseEntry k = new DatabaseEntry();
        keyBinding.objectToEntry(key, k);

        DatabaseEntry v = new DatabaseEntry();
        valueBinding.objectToEntry(value, v);

        this.database.put(null, k, v);
        return this;
    }


    /**
     * Retrieves the value associated with the given key, either from the persistent database, or from memory if
     * the database has been cached. This will return null if the key is not found, or has been excluded from the cache.
     *
     * @param key the key to search for
     * @return the value associated with the given key, or null if none exists.
     */
    public V retrieve(K key) {

        if (isCached) {
            return retrieveFromCache(key) ;
        } else {
            Database db = open(true) ;

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



    protected V retrieveFromCache(K key) {

        if (cachePriority == CachePriority.speed) {
            return fastCache.get(key) ;
        } else {
            byte[] cachedData = compactCache.get(key) ;

            if (cachedData == null)
                return null ;

            DatabaseEntry dbValue = new DatabaseEntry(cachedData) ;
            return valueBinding.entryToObject(dbValue) ;
        }
    }

    /**
     * @return an iterator for the entries in this database, in ascending key order.
     */
    public WIterator<K,V> getIterator() {
        return new WIterator<K,V>(this) ;
    }

}
