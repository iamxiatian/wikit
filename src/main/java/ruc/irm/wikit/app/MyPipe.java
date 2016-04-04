package ruc.irm.wikit.app;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.io.Files;
import ruc.irm.wikit.util.PorterStemmer;

import java.io.File;
import java.util.List;

/**
 * @author Tian Xia
 * @date Mar 28, 2016 11:30 PM
 */
public class MyPipe extends Pipe {
    private static final long serialVersionUID = 154100873830L;

    public static enum Type {
        ESA, ESPM, ESPMESA, SKIP
    }

    private Type type;
    private File homeDir;

    public MyPipe(File homeDir, Type type){
        this.homeDir = homeDir;
        this.type = type;
    }

    public Instance pipe(Instance carrier) {

        if (type == Type.SKIP) {
            return carrier;
        }

        //filename的形式：file:/home/xiatian/data/20news-subject/sci.electronics/54268
        String filename = carrier.getName().toString();
        int start = filename.lastIndexOf("/", filename.lastIndexOf("/") - 1);
        //截取后，仅保留sci.electronics/54268
        String name = filename.substring(start + 1);


        try {
            TokenSequence ts = (TokenSequence) carrier.getData();

            if (type == Type.ESA || type == Type.ESPMESA) {
                File f = new File(homeDir, "esa/" + name);
                List<String> lines = Files.readLines(f, Charsets.UTF_8);
                for (String line : lines) {
                    List<String> items = Splitter.on("|").splitToList(line);
                    if (items.size() > 2) {
                        Token token = new Token("ESA:" + items.get(1));
                        ts.add(token);
                    }
                }
            }

            if (type == Type.ESPM || type == Type.ESPMESA) {
                File f = new File(homeDir, "espm/" + name);
                List<String> lines = Files.readLines(f, Charsets.UTF_8);
                int count = 0;
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    count++;
                    if(count>20) break;
                    String path = line.substring(0, line.lastIndexOf("|"));
                    for (String item : Splitter.on("/").splitToList(path)) {
                        ts.add(new Token("ESPM:" + item));
                    }
                }
            }

            carrier.setData(ts);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return carrier;
    }
}
