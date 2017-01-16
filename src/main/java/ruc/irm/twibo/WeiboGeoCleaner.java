package ruc.irm.twibo;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.Jedis;

import java.io.*;
import java.text.DecimalFormat;
import java.util.Set;

/**
 * 微博经纬度坐标的清洗处理： 坐标精度保留6为小数，去除极少数的科学计数法坐标和0,0坐标，
 *
 * 坐标信息保存到Redis中，首先把每一个经度（小数精度保留6位）保存在SortedSet中，key为
 * longitude:all， sorted set的元素名称为经度的字符串，元素对应的数值权重为经度值。
 * 然后把每一个经度下的所有维度，也都保存在Redis的SortedSet中，Key为“lon:经度值”，元素
 * 为维度值对应的字符串，权重为维度值
 *
 * @author Tian Xia
 * @date Aug 04, 2016 00:24
 */
public class WeiboGeoCleaner {
    private Jedis jedis = null;
    private boolean removeDuplicate = false;

    public WeiboGeoCleaner() {
        this.jedis = new Jedis("127.0.0.1", 6379);
    }

    /**
     * 首先按照经度从大到小排序，相同经度则按照维度从大到小排序，在Redis中建立映射关系
     */
    public void sortInRedis(File geoFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(geoFile));
        String line = null;
        DecimalFormat df = new DecimalFormat("0.000000");

        int count = 0;
        while ((line = reader.readLine()) != null) {
            if (Strings.isNullOrEmpty(line)) {
                continue;
            }
            String[] lonlat = StringUtils.split(line, '\t');
            String longitude = lonlat[0];
            String latitude = lonlat[1];
            double d1 = Double.parseDouble(longitude);
            double d2 = Double.parseDouble(latitude);

            String s1 = df.format(d1);
            String s2 = df.format(d2);

            //把经度加到sorted set中, 哈希主键为经度的字符串，哈希值为经度的数值，从而实现按照经度排序
            jedis.zadd("longitude:all", d1, s1);

            //把该经度下的维度加入到sorted set中
            jedis.zadd("lon:" + s1, d2, s2);

            //把该经纬度的出现次数保存到Hash中
            jedis.hincrBy("geo:counter", s1+"," + s2, 1);

            count++;
            if (count % 1000 == 0) {
                System.out.println("Processing " + count + "...");
            }
        }

        reader.close();
        System.out.println("I'm DONE!");
    }

    public void dump(String prefix) throws IOException {
        int count = 0;
        int filePart = 1;
        PrintWriter writerAll = new PrintWriter(new FileWriter(prefix + "all.csv"));
        PrintWriter writerPart = new PrintWriter(new FileWriter(prefix + filePart + ".csv"));

        Set<String> longitudes = jedis.zrange("longitude:all", 0, -1);
        for (String lon : longitudes) {
            String key = "lon:" + lon;
            Set<String> latitudes = jedis.zrange(key, 0, -1);
            for (String lat : latitudes) {
                if(removeDuplicate) {
                    count++;
                    writerAll.println(lon + "," + lat);
                    writerPart.println(lon + "," + lat);
                    if (count == 200000) {
                        count = 0;
                        writerPart.close();
                        filePart++;
                        writerPart = new PrintWriter(new FileWriter(prefix + filePart + ".csv"));
                    }
                } else {
                    int duplicates = Integer.parseInt(jedis.hget("geo:counter", lon + "," + lat));
                    for(int i=0;i<duplicates;i++) {
                        count++;
                        writerAll.println(lon + "," + lat);
                        writerPart.println(lon + "," + lat);
                        if (count == 200000) {
                            count = 0;
                            writerPart.close();
                            filePart++;
                            writerPart = new PrintWriter(new FileWriter(prefix + filePart + ".csv"));
                        }
                    }
                }
            }
        }

        writerAll.close();
        writerPart.close();
    }

    public static void main(String[] args) throws IOException {
        WeiboGeoCleaner cleaner = new WeiboGeoCleaner();
        File f = new File("/home/xiatian/data/weibo/geo.txt");
        cleaner.sortInRedis(f);
        cleaner.dump("/home/xiatian/data/weibo/duplicate_keep/geo-");
    }
}
