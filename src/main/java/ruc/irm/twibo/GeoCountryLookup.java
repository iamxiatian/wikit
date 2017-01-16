package ruc.irm.twibo;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.Jedis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

/**
 * <p>经纬度和国家的对应关系， 主键为经度和维度分别保留6位小数后的字符串以逗号拼接的结果，对应的值为国家名称。</p>
 *  <br/>
 * 本类包括两个功能：<br/>
 * （1） 从文本中加载对应的经纬度坐标和国家信息到Redis中<br/>
 * （2） 获取一个经纬度所隶属的国家<br/>
 *
 * @author Tian Xia
 */
public class GeoCountryLookup {
    private Jedis jedis = null;
    private DecimalFormat df = new DecimalFormat("0.000000");

    public GeoCountryLookup() {
        this.jedis = new Jedis("127.0.0.1", 6379);
    }

    public GeoCountryLookup(Jedis jedis) {
        this.jedis = jedis;
    }

    /**
     * 从文件中读取经纬度和国家对应关系，保存到Redis中
     */
    public void loadFromFile(File geoFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(geoFile));
        reader.readLine(); //skip head

        String line = null;
        int count = 0;
        while ((line = reader.readLine()) != null) {
            if (Strings.isNullOrEmpty(line)) {
                continue;
            }
            List<String> items = Splitter.on(",").splitToList(line);
            String longitude = items.get(0);
            String latitude = items.get(1);
            String country = items.get(2);

            double d1 = Double.parseDouble(longitude);
            double d2 = Double.parseDouble(latitude);
            String key = df.format(d1) + "," + df.format(d2);
            jedis.set(key, country);

            count++;
            if (count % 1000 == 0) {
                System.out.println("Processing " + count + "...");
            }
        }

        reader.close();
        System.out.println("I'm DONE!");
    }

    public String getCountry(String longitude, String latitude) throws IOException {
        double d1 = Double.parseDouble(longitude);
        double d2 = Double.parseDouble(latitude);

        String key = df.format(d1) + "," + df.format(d2);
        String value = jedis.get(key);

        return StringUtils.isEmpty(value)?"Ocean": value;
    }

    public static void main(String[] args) throws IOException, ParseException {
        String helpMsg = "usage: GeoCountry";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        Option option = new Option("fs", true, "geo files for storing to the redis");
        option.setArgs(Option.UNLIMITED_VALUES);
        options.addOption(option);

        options.addOption(new Option("lon", true, "longitude"));
        options.addOption(new Option("lat", true, "latitude"));

        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("fs") && !(commandLine.hasOption("lon")
                && commandLine.hasOption("lat"))) {
            helpFormatter.printHelp(helpMsg, options);
            return;
        }
        GeoCountryLookup lookup = new GeoCountryLookup();
        if (commandLine.hasOption("fs")) {
            String[] fs = commandLine.getOptionValues("fs");
            System.out.println("Save geo - country info to Redis");
            for (String f : fs) {
                System.out.println("Process file " + f + "...");
                lookup.loadFromFile(new File(f));
            }
        } else if (commandLine.hasOption("lon") && commandLine.hasOption("lat")) {
            String longitude = commandLine.getOptionValue("lon");
            String latitude = commandLine.getOptionValue("lat");
            String country = lookup.getCountry(longitude, latitude);
            System.out.println(country);
        }

    }
}
