package ruc.irm.wikit.cache;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Cache interface:  ESA use redis as cache memory, in order to dump and import easily,
 *    we dump the redis content to gzip compressed file, and then the cache can be rebuild
 *    by dumped gzip file.
 *
 *    Through this way, we can build model on one machine (A), and then dump the cache and copy to another
 *    machine(B), then, we can run the model on machine B easily without slow building from the raw data.
 *
 * User: xiatian
 * Date: 8/12/14
 * Time: 10:22 PM
 */
public interface Cache {
    /**
     * ?? GBK中字符是一个或者两个字节，单字节00–7F这个区间和ASCII是一样的；
     * 双字节字符的第一个字节在81-FE之间，通过这个可以判断是单字节还是双字节，采用这种编码方式，
     * 可以节省中文的存储空间，而对于普通的英文字符，也没有影响
     * 注：因担心特殊符号问题，仍采用utf8编码
     */
    static final Charset ENCODING = Charset.forName("utf-8");

    public default void done(){
        System.out.println("Job " + this.getClass().getSimpleName() + " is " +
                "done!");
    };

    public default boolean hasDone(){
        return false;
    };

    /**
     * Save cache content to gzip compressed file, the gzip file should be specified by Conf
     *
     */
    public default void saveCacheToGZipFile() throws IOException {
        throw new IOException("Cache Method NOT Implemented.");
    };

    /**
     * Re-build cache content from previously saved gzip cache file, , the gzip file should be specified by Conf
     *
     * @throws IOException
     */
    public default void buildCacheFromGZipFile() throws IOException{
        throw new IOException("Cache Method NOT Implemented.");
    };

    /**
     * Clear all cache content
     */
    public void clearAll();
}
