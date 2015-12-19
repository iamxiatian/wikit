package ruc.irm.wikit.common.conf;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration factory, use this method to createWeiboConf application configuration.
 * <p/>
 * User: xiatian
 * Date: 4/16/14
 * Time: 12:25 PM
 */
public class ConfFactory {
    private static Map<String, Conf> confs = new HashMap<>();

    public static Conf createCommonConf() {
        return createConf("conf-common.xml", false);
    }

    public static Conf createZhConf() {
        return createConf("conf-chinese.xml", false);
    }

    public static Conf createEnConf() {
        return createConf("conf-english.xml", false);
    }

    public static Conf createConf(String resource, boolean isFileResource) {
        Conf conf = confs.get(resource);
        if (conf == null) {
            conf = new Conf();
            if (isFileResource) {
                File f = new File(resource);
                if(!f.exists()) {
                    System.err.println("File does not exist when loading " +
                            "resource from " + f.getAbsolutePath());
                    System.exit(1);
                } else {
                    conf.addResource(f);
                }
            } else {
                conf.addResource(resource);
            }

            if (conf.getBoolean("common.debug", false)) {
                System.out.println(StringUtils.center("Configuration", 30));

                try {
                    conf.writeXml(System.out);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            confs.put(resource, conf);
        }

        return conf;
    }


    /**
     * Create Commandline options for configuration, this method can be used
     *
     * @return
     */
    public static Options createConfCmdOptions() {
        Options options = new Options();
        options.addOption("h", false, "Print help");

        options.addOption("c", true, "specify config file, or en for default English wiki config, " +
                "or zh for default Chinese wiki config");//参数可用

        return options;
    }

    public static Conf createConf(String[] args) throws ParseException {
        return createConf(args, createConfCmdOptions());
    }

    public static Conf createConf(String[] args, Options options) throws ParseException {
        HelpFormatter hf = new HelpFormatter();

        CommandLineParser parser = new PosixParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("h")) {
            hf.printHelp("Options", options);
        } else if (cmd.hasOption("c")) {
            String value = cmd.getOptionValue("c");
            if (value.equals("en")) {
                return createEnConf();
            } else if (value.equals("zh")) {
                return createZhConf();
            } else if (!new File(value).exists()) {
                System.out.println("the config file does not exist ==> " + value);
            } else {
                return ConfFactory.createConf(value, true);
            }
        } else {
            hf.printHelp("Options", options);
        }
        return null;
    }

}
