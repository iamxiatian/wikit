package ruc.irm.wikit.db.je;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.ClassCatalog;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.collections.StoredSortedMap;
import com.sleepycat.je.*;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * @author Tian Xia
 * @date Dec 25, 2015 12:08 PM
 */
public class PageDatabase {
    private Environment env;
    private StoredClassCatalog pageCatalog;

    public PageDatabase(String homeDirectory)
            throws DatabaseException, FileNotFoundException {
        System.out.println("Opening environment in: " + homeDirectory);
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setTransactional(true);
        envConfig.setAllowCreate(true);
        env = new Environment(new File(homeDirectory), envConfig);

        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setTransactional(true);
        dbConfig.setAllowCreate(true);
        Database pageDB = env.openDatabase(null, "page",
                dbConfig);
        pageCatalog = new StoredClassCatalog(pageDB);

        EntryBinding keyBinding =
                new SerialBinding(pageCatalog, Integer.class);
        EntryBinding valueBinding =
                new SerialBinding(pageCatalog, String.class);
        StoredSortedMap map = new StoredSortedMap(pageDB, keyBinding,
                valueBinding, true);
        map.put(1, "hello");
        map.put(2, "world");

    }

    public final Environment getEnvironment() {
        return env;
    }

    public void close()
            throws DatabaseException {
        env.close();
    }
}
