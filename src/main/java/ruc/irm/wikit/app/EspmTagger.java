package ruc.irm.wikit.app;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.io.Files;
import org.apache.commons.cli.*;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.common.exception.WikitException;
import ruc.irm.wikit.esa.ESAModel;
import ruc.irm.wikit.esa.ESAModelImpl;
import ruc.irm.wikit.esa.concept.ConceptCache;
import ruc.irm.wikit.esa.concept.ConceptCacheRedisImpl;
import ruc.irm.wikit.esa.concept.vector.ConceptIterator;
import ruc.irm.wikit.esa.concept.vector.ConceptVector;
import ruc.irm.wikit.espm.SemanticPath;
import ruc.irm.wikit.espm.SemanticPathMining;
import ruc.irm.wikit.espm.impl.SemanticPathMiningWikiImpl;
import ruc.irm.wikit.nlp.SentenceSplitter;
import ruc.irm.wikit.util.FileFilters;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Tian Xia
 * @date Feb 28, 2016 10:48 PM
 */
public class EspmTagger {
    private static final Logger LOG = LoggerFactory.getLogger(EspmTagger.class);

    private Conf conf;
    private ESAModel esaModel = null;
    private SemanticPathMining espmModel = null;
    private SentenceSplitter splitter = null;
    private int conceptLimit = 20;
    private ConceptCache conceptCache = null;

    public EspmTagger(Conf conf) {
        this.conf = conf;

    }

    private void init() throws WikitException, IOException {
        this.esaModel = new ESAModelImpl(conf);
        this.espmModel = new SemanticPathMiningWikiImpl(conf);
        this.conceptCache = new ConceptCacheRedisImpl(conf);
        this.splitter = SentenceSplitter.getInstance();
    }


    /**
     * 抽取20news语料中的标题内容，形成短文本数据集
     */
    private static void extract20NGSubject(File ngHomeDir, File outputHomeDir) throws IOException, WikitException {
        File[] ngCategoryDirs = ngHomeDir.listFiles(FileFilters.DIRECTORY);
        for (File catDir : ngCategoryDirs) {
            File outCatDir = new File(outputHomeDir, catDir.getName());
            outCatDir.mkdirs();

            File[] files = catDir.listFiles(FileFilters.FILE);
            for (File f : files) {
                File outFile = new File(outCatDir, f.getName());
                List<String> lines = Files.readLines(f, Charsets.UTF_8);
                for (String line : lines) {
                    if (line.startsWith("Subject:")) {
                        Files.write(line.substring(8).trim(), outFile, Charsets.UTF_8);
                        break;
                    }
                }
            }
        }
    }


    private void generateSemanticInfoBySentence(File textFile, File outputHomeDir) throws IOException, WikitException {
        String category = textFile.getParentFile().getName();
        File espmPath = new File(outputHomeDir, "espm/" + category);
        espmPath.mkdirs();
        File espmFile = new File(espmPath, textFile.getName());

        File esaPath = new File(outputHomeDir, "esa/" + category);
        esaPath.mkdirs();
        File esaFile = new File(esaPath, textFile.getName());

        if (espmFile.exists() && esaFile.exists()) {
            return;
        }

        String[] sentences = splitter.split(textFile);
        StringBuilder esaBuffer = new StringBuilder();
        StringBuilder espmBuffer = new StringBuilder();

        for(String sentence: sentences) {
            ConceptVector cv = esaModel.getCombinedVector(sentence, conceptLimit);
            if (cv == null) {
                esaBuffer.append("\n");
                espmBuffer.append("\n");
                continue;
            }
            ConceptIterator it = cv.orderedIterator();
            while (it.next()) {
                int id = it.getId();
                String name = conceptCache.getNameById(id);
                String value = String.format("%.4f", it.getValue());
                esaBuffer.append(name).append(id + "|" + name + "|" + value).append("\n");
            }
            esaBuffer.append("\n");
            List<SemanticPath> paths = espmModel.getSemanticPaths(cv, conceptLimit, 50);
            for (SemanticPath path : paths) {
                double score = path.getAvgWeight();
                espmBuffer.append(path.getPathString('/') );
                espmBuffer.append("|").append(String.format("%.4f", score)).append('\n');
            }
            espmBuffer.append("\n");
        }


        Files.write(espmBuffer, espmFile, Charsets.UTF_8);

        Files.write(esaBuffer, esaFile, Charsets.UTF_8);
    }

    private void generateSemanticInfoByDoc(File textFile, File outputDir) throws IOException, WikitException {
        String category = textFile.getParentFile().getName();
        File espmPath = new File(outputDir, "espm/" + category);
        espmPath.mkdirs();
        File espmFile = new File(espmPath, textFile.getName());

        File esaPath = new File(outputDir, "esa/" + category);
        esaPath.mkdirs();
        File esaFile = new File(esaPath, textFile.getName());

        if (espmFile.exists() && esaFile.exists()) {
            return;
        }

        String text = Joiner.on("\n").join(Files.readLines(textFile, Charsets.UTF_8));

        StringBuilder esaBuffer = new StringBuilder();
        StringBuilder espmBuffer = new StringBuilder();

        ConceptVector cv = esaModel.getCombinedVector(text, conceptLimit);
        if (cv != null) {
            ConceptIterator it = cv.orderedIterator();
            while (it.next()) {
                int id = it.getId();
                String name = conceptCache.getNameById(id);
                esaBuffer.append(name).append(" ");
            }


            List<SemanticPath> paths = espmModel.getSemanticPaths(cv, conceptLimit, 5);
            for (SemanticPath path : paths) {
                double score = path.getAvgWeight();
                espmBuffer.append(path.getPathString('/') );
                espmBuffer.append("/").append(String.format("%.4f", score)).append('\n');
            }
        }

        Files.write(espmBuffer, espmFile, Charsets.UTF_8);
        Files.write(esaBuffer, esaFile, Charsets.UTF_8);
    }

    /**
     * 读取分类目录下（该目录下拥有类别子目录，子目录下含有文件）的文件内容，生成ESA和ESPM结果，
     * 分别保存到不同目录下的同名文件中
     * @param classifyCorpusFolder
     * @param outDir
     */
    public void generateSemanticInfo(File classifyCorpusFolder, File outDir, int type) throws IOException,
            WikitException {
        init();
        File[] categoryFolders = classifyCorpusFolder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory();
            }
        });

        for (File folder : categoryFolders) {
            LOG.info("process {}...", folder.getAbsolutePath());
            File[] instances = folder.listFiles(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isFile();
                }
            }) ;
            for (File instance : instances) {
                if(type==1) {
                    generateSemanticInfoByDoc(instance, outDir);
                } else if(type==2){
                    generateSemanticInfoBySentence(instance, outDir);
                }
            }
        }
    }


    private enum DataType {
        ESA,
        ESPM
    }

    /**
     * 把原始的20newsgroup中的文件与计算得到的ESA/ESPM结果文件合并，输出到outDir目录的相对位置中
     *
     *
     */
    void merge(File rawHomeDir, File mergingHomeDir, File mergedOutDir, DataType type) throws IOException {
        File[] categoryFolders = rawHomeDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory();
            }
        });

        for (File folder : categoryFolders) {
            LOG.info("process {}...", folder.getAbsolutePath());
            File[] instances = folder.listFiles(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isFile();
                }
            }) ;

            String category = folder.getName();
            for (File f : instances) {
                List<String> list = Files.readLines(f, Charsets.UTF_8);

                File mergingCategoryFolder = new File(mergingHomeDir, category);
                File mergingFile = new File(mergingCategoryFolder, f.getName());

                if(type==DataType.ESA) {
                    List<String> lines = Files.readLines(mergingFile, Charsets.UTF_8);
                    for(String line: lines) {
                        list.add(processLine("ESA", line));
                    }
                } else {
                    list.addAll(readLinesFromEspmFile2(mergingFile));
                }

                File outputCategoryFolder = new File(mergedOutDir, category);
                outputCategoryFolder.mkdirs();
                File outFile = new File(outputCategoryFolder, f.getName());
                Files.write(Joiner.on("\n").join(list), outFile, Charsets.UTF_8);
            }
        }
    }

    /**
     * 利用空行进行分割，每一个空行之间为一句话的ESPM输出结果，仅保留每个句子ESPM结果的
     * 前2个结果
     */
    private List<String> readLinesFromEspmFile(File f) throws IOException {
        List<String> results = new ArrayList<>();

        List<String> lines = Files.readLines(f, Charsets.UTF_8);
        List<String> part = new ArrayList<>();
        for (String line : lines) {
            if (line.isEmpty()) {
                if (!part.isEmpty()) {
                    for(int i=0; i<part.size() && i<2; i++) {
                        results.add(part.get(i));
                    }
                    part.clear();
                }
            } else {
                part.add(line);
            }
        }

        return results;
    }

    private String processLine(String prefix, String line) {
        String[] items = StringUtils.split(line, "\t ");
        List<String> merged = new ArrayList<>();
        for (String item : items) {
            merged.add(prefix + item);
        }
        return Joiner.on(" ").join(merged).toString();
    }

    private List<String> readLinesFromEspmFile2(File f) throws IOException {
        List<String> results = new ArrayList<>();

        List<String> lines = Files.readLines(f, Charsets.UTF_8);
        List<String> part = new ArrayList<>();
        int count = 0;
        for (String line : lines) {
            if (line.isEmpty())
                continue;

            List<String> names = Splitter.on("/").trimResults().splitToList(line);
            StringBuilder buffer = new StringBuilder();
            for (int i = 1; i < names.size() - 1; i++) {
                //skip  and last element
                if(buffer.length()>0) buffer.append(" ");

                buffer.append(names.get(i));
            }
            String processedLine = processLine("ESPM", buffer.toString());
            results.add(processedLine);
            count++;
            if(count==10)  break;
        }

        return results;
    }

    private static boolean hasOptions(CommandLine cline, String... commands) {
        for (String c : commands) {
            if(!cline.hasOption(c)) return false;
        }
        return true;
    }

    public static void main(String[] args) throws ParseException, WikitException, IOException {
        //extract20NGSubject(new File("/home/xiatian/data/20news-18828"), new File("/home/xiatian/data/20news-subject"));
        // System.out.println(new EspmTagger(new Conf()).processLine("ESPM", "OK HELLO\tGOGO"));
        String helpMsg = "usage: SmanticPathMining -c config.xml";
        System.out.println("specify 'in' and 'out' parameter to generate esa/espm results.");
        System.out.println("specify 'in1' 'in2' and 'out' parameter to merge results.");

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("c", true, "config file"));
        options.addOption(new Option("in", true, "input classification folder."));
        options.addOption(new Option("out", true, "output dir"));
        options.addOption(new Option("type", true, "for generate, 1 for full text, 2 for sentence; for merge: 1 for " +
                "ESA, 2 for ESPM"));


        options.addOption(new Option("in1", true, "input classification folder."));
        options.addOption(new Option("in2", true, "input espm/esa folder."));


        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("c")
                || (!hasOptions(commandLine, "in", "out")
                && !hasOptions(commandLine, "in1", "in2", "out"))) {
            helpFormatter.printHelp(helpMsg, options);
            return;
        }
        Conf conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);
        EspmTagger tagger = new EspmTagger(conf);
        File outDir = new File(commandLine.getOptionValue("out"));

        if (hasOptions(commandLine, "in", "out")) {
            File inDir = new File(commandLine.getOptionValue("in"));
            int type = Integer.parseInt(commandLine.getOptionValue("type", "1"));
            tagger.generateSemanticInfo(inDir, outDir, type);
        } else if (hasOptions(commandLine, "in1", "in2", "out")) {
            File inDir1 = new File(commandLine.getOptionValue("in1"));
            File inDir2 = new File(commandLine.getOptionValue("in2"));
            int type = Integer.parseInt(commandLine.getOptionValue("type"));
            if (type == 1) {
                tagger.merge(inDir1, inDir2, outDir, DataType.ESA);
            } else if (type == 2) {
                tagger.merge(inDir1, inDir2, outDir, DataType.ESPM);
            }
        }
        System.out.println("I'm DONE!");
    }
}

