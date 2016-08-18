package ruc.irm.twibo;

import com.google.common.base.Splitter;
import org.apache.commons.cli.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.Jedis;
import ruc.irm.wikit.util.ProgressCounter;

import java.io.*;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Set;

/**
 * 寻找微博数据集中曾经在海外发过信息的用户集合
 *
 * @author Tian Xia
 * @date Aug 11, 2016 05:15
 */
public class WeiboSuperUserFinder {
    private DecimalFormat df = new DecimalFormat("0.000000");
    private String redisKey = "oversea:users";
    private GeoCountryLookup lookup = null;
    private Jedis jedis = null;

    public WeiboSuperUserFinder() {
        this.jedis = new Jedis("127.0.0.1", 6379);
        this.lookup = new GeoCountryLookup(jedis);
    }

    public void clear() {
        jedis.del(redisKey);
    }

    /**
     * 扫描当前的微博原始CSV文件，如果用户在发布消息时的地理位置位于大陆之外，则记录到Redis中
     *
     * @param inputCsvFile
     * @throws IOException
     */
    private void scan(File inputCsvFile) throws IOException {
        System.out.println("Process Weibo CSV file " + inputCsvFile.getAbsolutePath() + "...");

        Reader in = new FileReader(inputCsvFile);
        Iterable<CSVRecord> records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
        for (CSVRecord record : records) {
            String point = record.get("geo");
            if (StringUtils.isEmpty(point)) {
                continue;
            }

            String[] lonlat = point.split(" ");
            String longitude = lonlat[0].substring(6);
            String latitude = lonlat[1].substring(0, lonlat[1].length() - 1);
            String userId = record.get("uid");
            String country = lookup.getCountry(longitude, latitude);
            if (country.equalsIgnoreCase("China")) {
                continue;
            }

            if (jedis.hexists(redisKey, userId)) {
                String value = jedis.hget(redisKey, userId);
                List<String> countries = Splitter.on(",").splitToList(value);
                if (!countries.contains(country)) {
                    value += "," + country;
                    jedis.hset(redisKey, userId, value);
                }
            } else {
                jedis.hset(redisKey, userId, country);
            }
        }
        in.close();
    }

    public void scanAll() throws IOException {
        String path = "/media/xiatian/Experiment/corpus/WEIBO";
        File[] csvFiles = new File[52];
        for(int i=0; i<52; i++) {
            File f = new File(path + "/week" + (i + 1) + ".csv");
            csvFiles[i] = f;
        }

        for (File f : csvFiles) {
            scan(f);
        }
    }

    /**
     * 扫描当前的微博原始CSV文件，如果用户在发布消息时的地理位置位于大陆之外，则记录到Redis中
     *
     * @throws IOException
     */
    private void findOverseaUsers(File candidateUserCSV) throws IOException {
        System.out.println("Process candidate oversea CSV file " + candidateUserCSV.getAbsolutePath() + "...");

        Reader in = new FileReader(candidateUserCSV);
        //Iterable<CSVRecord> records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
        Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(in);
        ProgressCounter counter = new ProgressCounter();
        for (CSVRecord record : records) {
            String longitude = record.get(0);
            String latitude = record.get(1);
            String userId = record.get(2);
            String country = lookup.getCountry(longitude, latitude);
            if (country.equalsIgnoreCase("China")) {
                continue;
            }

            if (jedis.hexists(redisKey, userId)) {
                String value = jedis.hget(redisKey, userId);
                List<String> countries = Splitter.on(",").splitToList(value);
                if (!countries.contains(country)) {
                    value += "," + country;
                    jedis.hset(redisKey, userId, value);
                }
            } else {
                jedis.hset(redisKey, userId, country);
            }
            counter.increment();
        }
        counter.done();
        in.close();
    }

    public void dump(File outFile) throws IOException {
        PrintWriter writer = new PrintWriter(new FileWriter(outFile));
        Set<String> hkeys = jedis.hkeys(redisKey);
        ProgressCounter counter = new ProgressCounter(jedis.hlen(redisKey));
        for (String hkey : hkeys) {
            writer.print(hkey);
            writer.print(",");
            writer.println(jedis.hget(redisKey, hkey));
            counter.increment();
        }
        counter.done();
        writer.close();
    }

    public static void main(String[] args) throws IOException, ParseException {
        String helpMsg = "Usage: ";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("df", true, "Dump File: dump all oversea users to this file"));
        options.addOption(new Option("scan", false, "Scan all files to find oversea users."));
        options.addOption(new Option("find", true, "find all oversea users for given candidate user list."));

        CommandLine commandLine = parser.parse(options, args);
        WeiboSuperUserFinder finder = new WeiboSuperUserFinder();
        if (commandLine.hasOption("df")) {
            File outFile = new File(commandLine.getOptionValue("df"));
            finder.dump(outFile);
        } else if (commandLine.hasOption("scan")) {
            finder.scanAll();
        } else if (commandLine.hasOption("find")) {
            finder.findOverseaUsers(new File(commandLine.getOptionValue("find")));
        }

        System.out.println("I'm DONE!");
    }

}
