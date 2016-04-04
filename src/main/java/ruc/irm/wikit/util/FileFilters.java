package ruc.irm.wikit.util;


import java.io.File;
import java.io.FileFilter;

/**
 * @author Tian Xia
 * @date Mar 18, 2016 10:08 PM
 */
public class FileFilters {
    public static final FileFilter DIRECTORY = new DirectoryFilter();
    public static final FileFilter FILE = new CommonFileFilter();

    private static class DirectoryFilter implements FileFilter {
        @Override
        public boolean accept(File f) {
            return f.isDirectory();
        }
    }

    private static class CommonFileFilter implements FileFilter {
        @Override
        public boolean accept(File f) {
            return f.isFile();
        }
    }
}
