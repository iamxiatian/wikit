 package ruc.irm.wikit.data.db;

 import com.mongodb.client.MongoCollection;
 import com.mongodb.client.MongoDatabase;
 import org.apache.log4j.LogManager;
 import org.bson.Document;
 import ruc.irm.wikit.common.conf.Conf;

 import java.util.HashMap;
 import java.util.Map;

 /**
 * User: xiatian
 * Date: 4/7/14
 * Time: 5:30 PM
 */
public class MongoClient {

    private static Map<String, MongoDatabase> dbs = new HashMap<>();

    private static MongoDatabase connect(Conf conf) {
        String key = conf.getMongoHost() + ":" + conf.getMongoPort() + ":" +
                conf.getMongoDbName();

        MongoDatabase db = dbs.get(key);
        if(db==null){
            //close logger of mongodb
            LogManager.getLogger("org.mongodb.driver.connection").setLevel(org.apache.log4j.Level.OFF);
            LogManager.getLogger("org.mongodb.driver.management").setLevel(org.apache.log4j.Level.OFF);
            LogManager.getLogger("org.mongodb.driver.cluster").setLevel(org.apache.log4j.Level.OFF);
            LogManager.getLogger("org.mongodb.driver.protocol.insert").setLevel(org.apache.log4j.Level.OFF);
            LogManager.getLogger("org.mongodb.driver.protocol.query").setLevel(org.apache.log4j.Level.OFF);
            LogManager.getLogger("org.mongodb.driver.protocol.update").setLevel(org.apache.log4j.Level.OFF);
            LogManager.getLogger("org.mongodb").setLevel(org.apache.log4j.Level.OFF);

            com.mongodb.MongoClient client = new com.mongodb.MongoClient(
                    conf.getMongoHost(), conf.getMongoPort());

            db = client.getDatabase(conf.getMongoDbName());
            dbs.put(key, db);
        }
        return db;
    }

    public static MongoCollection<Document> getCollection(Conf conf,
                                                          String relativeName) {
        return connect(conf).getCollection(conf.getMongoCollectionPrefix() + relativeName);
    }

     public static MongoCollection<Document> getCollection(Conf conf, String prefix, String relativeName) {
         return connect(conf).getCollection(prefix+ relativeName);
     }

     public static MongoCollection<Document> getOriginCollection(Conf conf, String fullName) {
         return connect(conf).getCollection(fullName);
     }

}
