package ruc.irm.wikit.esa.index;

import com.google.common.primitives.Chars;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.util.HeapSort;
import ruc.irm.wikit.util.text.analysis.ESAAnalyzer;

import java.io.*;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads TF and IDF from the index and writes cosine-normalized TF.IDF values to files.
 * <p>For IDF, store idf results into termsIdfOutputTextFile, it's a plain text file, each line
 * likes:
 * terms1 \t idf value
 * term2 \t idf value
 * ...
 * </p>
 * <p/>
 * Normalization is performed as in Gabrilovich et al. (2009)
 * <p/>
 * Usage: IndexModifier <Lucene index location>
 *
 * @author Cagatay Calli <ccalli@gmail.com>
 */
public class IndexMining {
    private static final Logger LOG = LoggerFactory.getLogger(IndexMining.class);

    private String termsIdfOutputTextFile = null;
    private String tfidfOutputDataFile = null;

    private File vectorFileName = null;
    private File sortedVectorFileName = null;

    private Analyzer analyzer = null;
    private IndexReader reader = null;
    private IndexSearcher searcher = null;

    private static TIntDoubleHashMap inlinkMap;

    private static int WINDOW_SIZE = 100;
    private static float WINDOW_THRES = 0.005f;
    private static DecimalFormat decimalFormat = new DecimalFormat("#.#######");
    private float tfidfBoost = 1.5f;

    private Conf conf = null;
    public IndexMining(Conf conf) {
        this.conf = conf;

        this.termsIdfOutputTextFile = conf.getWikiTermsIdfFile();
        this.tfidfOutputDataFile = conf.getWikiTfidfFile();

        this.vectorFileName = new File(conf.getEsaModelDir(), "vector.txt");
        this.sortedVectorFileName = new File(conf.getEsaModelDir(), "vector.sorted.txt");
        this.vectorFileName.getParentFile().mkdirs();
        this.tfidfBoost = conf.getFloat("esa.model.title.boost", 1.5f);
    }


    public void open() throws IOException {
        this.analyzer = new ESAAnalyzer(conf);
        this.reader = DirectoryReader.open(FSDirectory.open(new File(conf.getEsaIndexDir())));
        this.searcher = new IndexSearcher(reader);
    }


    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }

    private static final char[] SPECIAL_CHARS = new char[]{'.', '"', '\'',
            ']', '[', '%', '@', '!', '}', '{', '|'} ;
    public void modify() throws IOException {
        //maintain the term name and index number mapping
        HashMap<String, Integer> termHash = new HashMap<String, Integer>(500000);

        //maintain the term and global idf mapping
        HashMap<String, Float> idfMap = new HashMap<String, Float>(500000);

        //maintain the term and tfidf mapping for one document
        HashMap<String, Float> tfidfMap = new HashMap<String, Float>(5000);

        int maxDocId = reader.maxDoc();
        int numDocs = reader.numDocs();
        LOG.debug("maxId=" + maxDocId + ", numDocs=" + numDocs);


        //Step 1: get global idf hash map and termHash
        LOG.warn("Step 1: generate global terms idf...");
        Terms globalTerms = MultiFields.getTerms(reader, "contents");
        TermsEnum termsEnum = globalTerms.iterator(null);
        BytesRef text = null;
        int hashInt = 0;
        while((text = termsEnum.next()) != null) {
            String term = text.utf8ToString();
            //如果term全为特殊符号，则滤掉
            boolean skipped = true;
            for (int i = 0; i < term.length(); i++) {
                if(!Chars.contains(SPECIAL_CHARS, term.charAt(i))){
                    skipped = false;
                    break;
                }
            }
            if(skipped) continue;

            //get DF for the term
            int docFreq = termsEnum.docFreq();
            //如果docFreq比较小，并且没有出现在标题中，则过滤掉
            if (docFreq <= 5) {    // skip rare terms
                int hits = 0;
                try {
                    QueryParser parser = new QueryParser(Conf.LUCENE_VERSION, "title",
                        analyzer);
                    Query titleQuery = parser.parse(term);
                    hits = searcher.search(titleQuery, 5).totalHits;
                } catch (ParseException e) {
                    //e.printStackTrace();
                }
                if(hits==0) continue;
            }

            float idf = (float) (Math.log(numDocs / (double) (docFreq)));
            idfMap.put(term, idf);
            termHash.put(term, hashInt++);
        }

        //Step 5: save global idf
        LOG.warn("Step 2: save global terms idf...");
        saveIDF(idfMap);

        //Step 2: make tf-idf for each document, and save them to file resultVectorFile
        LOG.warn("Step 3: generate tf-idf for each document...");
        FileOutputStream fos = new FileOutputStream(vectorFileName);
        OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8");
        for(int docId=0; docId<maxDocId; docId++) {
            //@TODO skip deleted documents
            Document document = reader.document(docId);
            if(document==null) continue;

            tfidfMap.clear();
            String conceptId = document.get("id");
            double inlinkBoost = 1.0;//inlinkMap.get(wikiId);
            Terms contentTerms = reader.getTermVector(docId, "contents");
            if (contentTerms == null) {
                System.out.println("skip id==>" + conceptId + ", title==>" + document.get("title"));
                continue;
            }
            termsEnum = contentTerms.iterator(null);
            double sum = 0.0;
            //
            while ((text=termsEnum.next()) != null) {
                String term = text.utf8ToString();
                long termFreq = termsEnum.totalTermFreq();
                if(!idfMap.containsKey(term)) continue;

                float idf = idfMap.get(term);
                float tf = (float) (1.0 + Math.log(termFreq));
                float tfidf = (float) (tf * idf);
                tfidfMap.put(term, tfidf);
                sum += tfidf * tfidf;

                //System.out.println("id==>" + wikiId + ", title==>" + document.get("title") + ", term==>" + text.utf8ToString() + ", termFreq==>" + termFreq);
            }

            //boost title field terms
            if(tfidfBoost>1.1) {
                //boost tf-idf
                Terms titleTerms = reader.getTermVector(docId, "title");
                if (titleTerms != null) {
                    TermsEnum titleTermsEnum = titleTerms.iterator(null);
                    while ((text = titleTermsEnum.next()) != null) {
                        String term = text.utf8ToString();
                        if (!idfMap.containsKey(term)) continue;

                        float tfidf = tfidfMap.get(term);
                        tfidfMap.put(term, tfidf * tfidfBoost);
                        sum = sum + tfidf * (tfidfBoost - 1);
                    }
                }
            }

            sum = Math.sqrt(sum);
            //re-scan document terms to normalize the value, then save to the output writer
            contentTerms = reader.getTermVector(docId, "contents");
            termsEnum = contentTerms.iterator(null);
            while ((text=termsEnum.next()) != null) {
                String term = text.utf8ToString();
                if(!idfMap.containsKey(term)) continue;

                float tfidf = (float)(tfidfMap.get(term)* inlinkBoost / sum );
                writer.write(termHash.get(term) + "\t" + term + "\t" + conceptId + "\t" + decimalFormat.format(tfidf) + "\n");
            }
        }

        writer.close();
        fos.close();

        //Step 3: sort vector file into sorted vector file and remove original vector file
        LOG.warn("Step 3: sorting tf-idf text file...");
        sort();

        //Step 4: save term tf-idf indexes table
        LOG.warn("Step 4: save term tf-idf indexes...");
        saveTermTfIdfIndexes();
    }

    private void sort() throws IOException {
        // sort tfidf entries according to terms
//        String command = String.format("sort -T /home/xiatian/tmp -S 2000M -n -t\\\t  -k1 < %s > %s",
//                vectorFileName.getAbsolutePath(),
//                sortedVectorFileName.getAbsolutePath());
        String command = String.format("sort -T ~/tmp -S 2000M -n -t\\\t  -k1 < %s > %s",
                vectorFileName.getAbsolutePath(),
                sortedVectorFileName.getAbsolutePath());
        new File("~/tmp").mkdirs();

        String[] cmd = {"/bin/sh", "-c", command};
        Process p1 = Runtime.getRuntime().exec(cmd);
        try {
            int exitV = p1.waitFor();
            if (exitV != 0) {
                System.exit(1);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // delete unsorted doc-score file
        command = String.format("rm %s", vectorFileName);
        p1 = Runtime.getRuntime().exec(command);
        try {
            int exitV = p1.waitFor();
            if (exitV != 0) {
                System.exit(1);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void saveTfIdfIndexToFile(DataOutputStream out, String term, byte[] tfidfByteArray) throws IOException {
        byte[] array = term.getBytes("utf-8");
        out.writeInt(array.length);
        out.write(array);
        out.writeInt(tfidfByteArray.length);
        out.write(tfidfByteArray);
    }


    /**
     * 保存所有term的doc-tfidf向量，格式为:
     * 第一个term名称的长度，bytes格式的内容，第一个term的向量转换为二进制的总长度，term向量的集合长度，term第一个向量的
     * @throws IOException
     */
    private void saveTermTfIdfIndexes() throws IOException {
        FileInputStream fis = new FileInputStream(sortedVectorFileName);
        InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
        BufferedReader reader = new BufferedReader(isr);

        FileOutputStream tfidfFOS = new FileOutputStream(tfidfOutputDataFile);
        DataOutputStream tfidfDOS = new DataOutputStream(tfidfFOS);

        String prevTerm = null;
        int doc;
        float score;
        TIntFloatHashMap hmap = new TIntFloatHashMap(100);

        float first = 0, last = 0, highest = 0;
        float[] window = new float[WINDOW_SIZE];

        String line = null;
        while ((line = reader.readLine()) != null) {
            final String[] parts = line.split("\t");
            String term = parts[1];

            // prune and write the vector
            //处理之前的term的<doc, score>向量
            if (prevTerm != null && !prevTerm.equals(term)) {
                int[] arrDocs = hmap.keys();
                float[] arrScores = hmap.values();

                HeapSort.heapSort(arrScores, arrDocs);

                //termBAOS中保存了term的排序后的<doc,score>对。score为tf-idf值
                // 为了降低数据维度，我们对每个term对应的向量v={<doc_1, score_1>, <doc_2,
                // score_2>...}（score_i>=socre_{i+1}）进行简化，首先v至少由m=WINDOW_SIZE
                // 个文档组成，如果超过该数值，则判断窗口内文档得分的差值决定是否继续把后续的文档加入到向量中：
                //如果窗口第一个文档的得分和最后一个文档的的得分差值小于 (score_1×threshold)
                // ，说明后面的文档得分变化不大，从向量中移除。
                int pairCount = 0; //向量中保存的<doc, score>数量
                int pairCountInWindow = 0;
                highest = first = last = 0;

                //此处用ByteArrayOutputStream和DataOutputStream，主要是为了调用不同的方法
                //DataOutputStream中写入的内容，通过ByteArrayOutputStream也可以获得,
                //我们用其toByteArray()方法获得写入的内容
                ByteArrayOutputStream termBAOS = new ByteArrayOutputStream(50000);
                DataOutputStream termDOS = new DataOutputStream(termBAOS);
                for (int j = arrDocs.length - 1; j >= 0; j--) {
                    score = arrScores[j];

                    // sliding window
                    window[pairCountInWindow] = score;

                    if (pairCount == 0) {
                        highest = score;
                        first = score;
                    }

                    if (pairCount < WINDOW_SIZE) {
                        termDOS.writeInt(arrDocs[j]);
                        termDOS.writeFloat(score);
                    } else if (highest * WINDOW_THRES < (first - last)) {
                        termDOS.writeInt(arrDocs[j]);
                        termDOS.writeFloat(score);

                        if (pairCountInWindow < WINDOW_SIZE - 1) {
                            first = window[pairCountInWindow + 1];
                        } else {
                            first = window[0];
                        }
                    } else {
                        // truncate
                        break;
                    }

                    last = score;

                    pairCount++;
                    pairCountInWindow++;

                    pairCountInWindow = pairCountInWindow % WINDOW_SIZE;

                }

                ByteArrayOutputStream vectorBAOS = new ByteArrayOutputStream();
                DataOutputStream vectorDOS = new DataOutputStream(vectorBAOS);
                vectorDOS.writeInt(pairCount);
                vectorDOS.flush();
                vectorBAOS.write(termBAOS.toByteArray());
                vectorBAOS.flush();
                vectorDOS.close();

                //vector保存了term所对应的由文档-得分构成的向量的长度和具体值
                byte[] vector = vectorBAOS.toByteArray();
                //把term和对应的vector写入到文件中
                saveTfIdfIndexToFile(tfidfDOS, prevTerm, vector);
                tfidfDOS.flush();

                termDOS.close();
                termBAOS.close();

                hmap.clear();
            }

            doc = Integer.valueOf(parts[2]);
            score = Float.valueOf(parts[3]);

            hmap.put(doc, score);

            prevTerm = term;
        }

        reader.close();
        isr.close();
        fis.close();

        tfidfDOS.writeInt(-1); //write -1 as end indicator
        tfidfDOS.close();
        tfidfFOS.close();
    }


    private void saveIDF(Map<String, Float> idfMap) throws IOException {
        //record term IDFs
        FileOutputStream tos = new FileOutputStream(termsIdfOutputTextFile);
        OutputStreamWriter writer = new OutputStreamWriter(tos, "UTF-8");
        for (Map.Entry<String, Float> entry : idfMap.entrySet()) {
            writer.write(entry.getKey() + "\t" + decimalFormat.format(entry.getValue()) + "\n");
        }
        writer.close();
        tos.close();
    }

}
