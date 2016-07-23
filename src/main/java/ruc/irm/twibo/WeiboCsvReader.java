package ruc.irm.twibo;

import au.com.bytecode.opencsv.CSVReader;
import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author Tian Xia
 * @date Jul 11, 2016 23:08
 */
public class WeiboCsvReader {
    private static void read(File f, int topN) throws IOException {
        CSVReader reader = new CSVReader(new FileReader(f));
        String[] items = null;
        int count = 0;

        reader.readNext(); //skip head

        while ((items = reader.readNext()) != null) {
            String point = items[7];

            if (StringUtils.isEmpty(point)) {
                continue;
            }

            String[] lonlat = point.split(" ");
            String longitude = lonlat[0].substring(6);
            String latitude = lonlat[1].substring(0, lonlat[1].length()-1);

            System.out.println(items[7]);
            System.out.println(longitude + "," + latitude);
            if (++count == topN) {
                break;
            }
        }

        reader.close();
    }

    public static void main(String[] args) throws IOException {
        File f = new File("/home/xiatian/data/weibo/week36.csv");
        WeiboCsvReader.read(f, 10);
    }
}
