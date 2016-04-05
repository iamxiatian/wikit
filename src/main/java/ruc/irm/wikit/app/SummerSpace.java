package ruc.irm.wikit.app;

import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.FileIterator;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;
import edu.ucla.sspace.text.IteratorFactory;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vsm.VectorSpaceModel;
import ruc.irm.wikit.util.mallet.PorterStemmerPipe;

import java.io.*;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Properties;

/**
 * Created by xiatian on 4/4/16.
 */
public class SummerSpace {

    Pipe makePipe(File homeDir, MyPipe.Type type) {
        return new SerialPipes(new Pipe[] {
                new Target2Label(),
                new Input2CharSequence(),
                //new CharSequenceLowercase(),
                new CharSequence2TokenSequence(),
                new TokenSequenceRemoveStopwords(),
                new PorterStemmerPipe(),
                new MyPipe(homeDir, type),
                new TokenSequenceLowercase(),});
    }

    public InstanceList load20NGInstances(File ngCorpusPath, File miningDir, MyPipe.Type type) {
        InstanceList instances = new InstanceList(makePipe(miningDir, type));


        File[] dirs = ngCorpusPath.listFiles();
        instances.addThruPipe(new FileIterator(dirs, FileIterator.LAST_DIRECTORY));

        return instances;
    }

    public void build2(File ngCorpusPath) throws IOException {
        VectorSpaceModel vsm = new VectorSpaceModel();

        File miningDir = new File("/home/xiatian/data/20news-mining-subject");
        InstanceList instances = load20NGInstances(ngCorpusPath, miningDir, MyPipe.Type.PURE_BOW);

        for (Instance instance : instances) {
            TokenSequence ts = (TokenSequence)instance.getData();
            ListIterator<Token> it = ts.listIterator();
            Iterator<String> it2 = new Iterator<String>() {
                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public String next() {
                    String text = it.next().getText();
                    //System.out.println("text");
                    return text;
                }
            };

            vsm.processDocument(it2);
        }

        vsm.processSpace(new Properties());
        System.out.printf("Vsm has %d docs and %d words%n",
                vsm.documentSpaceSize(), vsm.getWords().size());

        int idx = 0;
        for(String w:vsm.getWords()){
            System.out.println((idx++) + "\t ==> " + w);
        }

        for (int i = 0; i < 3; ++i) {
            DoubleVector vector = vsm.getDocumentVector(i);
            System.out.println("magnitude: " + vector.magnitude());
            for(int j=0; j<vector.length(); j++) {
                if(vector.get(j)==0 && j<vector.length()-1) continue;
                System.out.print(j + ":" + vector.get(j) + " ");
            }
            System.out.println("\n-------------------------\n");
        }

    }


    public void build(File dir) throws IOException {
        IteratorFactory.setProperties(new Properties());
        VectorSpaceModel vsm = new VectorSpaceModel();
        try {
            File[] childDirs = dir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isDirectory();
                }
            });

            for (File childDir : childDirs) {
                File[] textFiles = childDir.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        return f.isFile();
                    }
                });
                for (File f : textFiles) {
                    vsm.processDocument(new BufferedReader(new FileReader(f)));
                }
            }
            vsm.processSpace(new Properties());
            System.out.printf("Vsm has %d docs and %d words%n",
                    vsm.documentSpaceSize(), vsm.getWords().size());

            int idx = 0;
            for(String w:vsm.getWords()){
                System.out.println(idx + "\t ==> " + w);
            }

            for (int i = 0; i < 3; ++i) {
                DoubleVector vector = vsm.getDocumentVector(i);
                System.out.println("magnitude: " + vector.magnitude());
                for(int j=0; j<vector.length(); j++) {
                    if(vector.get(j)==0 && j<vector.length()-1) continue;
                    System.out.print(j + ":" + vector.get(j) + " ");
                }
                System.out.println("\n-------------------------\n");
            }
        }
        catch (Throwable t) {
            throw new Error(t);
        }
    }

    public static void main(String[] args) throws IOException {
        SummerSpace space = new SummerSpace();
        File dir = new File("/home/xiatian/data/20news-subject");
        space.build2(dir);

    }
}
