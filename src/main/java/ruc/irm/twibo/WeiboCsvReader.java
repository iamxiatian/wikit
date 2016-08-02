package ruc.irm.twibo;

import au.com.bytecode.opencsv.CSVReader;
import com.google.common.base.Strings;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOExceptionWithCause;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.Arrays;

/**
 * <p>解析CSV文件，把其中的地理坐标提取出来，保存到文本文件中。该文件会包含重复的内容，之后可以用
 * Linux命令过滤掉重复的条目,例如：</p>
 *
 * <code>sort geo.txt| uniq > geo_no_duplicate.txt</code>
 *
 * @author Tian Xia
 * @date Jul 11, 2016 23:08
 */
public class WeiboCsvReader {


    /**
     * 解析存放微博数据的CSV文件，提取出其中的经纬度数据，保存到文本文件输出流中
     */
    private static void outputGeoInfo(File inputCsvFile, PrintWriter out) throws IOException {
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

            //System.out.println(items[7]);
            out.println(longitude + "\t" + latitude);
        }

        in.close();
    }


    public static void outputGeoInfo(File[] inputCsvFiles, File outGeoFile) throws IOException {
        PrintWriter writer = new PrintWriter(new FileWriter(outGeoFile));
        for (File f : inputCsvFiles) {
            outputGeoInfo(f, writer);
            writer.flush();
        }

        writer.close();
    }

    public static void main(String[] args) throws IOException {
//        File homeDir = new File("/media/xiatian/Experiment/corpus/WEIBO");
//        File[] csvFiles = homeDir.listFiles(f -> f.getAbsolutePath().endsWith(".csv"));
        String path = "/media/xiatian/Experiment/corpus/WEIBO";
        File[] csvFiles = new File[52];
        for(int i=0; i<52; i++) {
            File f = new File(path + "/week" + (i + 1) + ".csv");
            csvFiles[i] = f;
        }

        for (File f : csvFiles) {
            System.out.println(f.getAbsolutePath());
        }

        File outFile = new File(path + "/geo.txt");
        outputGeoInfo(csvFiles, outFile);
    }
}
