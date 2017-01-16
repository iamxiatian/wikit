package ruc.irm.wikit.app;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.io.Files;

import java.io.File;
import java.util.List;

/**
 * 根据管道类型，决定如何合并ESPM、ESA分析结果数据，使得每个Instance最终能够根据设置生成对应的特征集合
 *
 * @author Tian Xia
 * @date Mar 28, 2016 11:30 PM
 */
public class MyPipe extends Pipe {
    private static final long serialVersionUID = 154100873830L;

    public static enum Type {
        BOW_ESA, BOW_ESPM, BOW_ESPM_ESA, PURE_ESA, PURE_ESPM, PURE_ESPM_ESA, PURE_BOW
    }

    private Type type;
    private File homeDir;

    public MyPipe(File homeDir, Type type){
        this.homeDir = homeDir;
        this.type = type;
    }

    public Instance pipe(Instance carrier) {

        if (type == Type.PURE_BOW) {
            return carrier;
        }

        //filename的形式：file:/home/xiatian/data/20news-subject/sci.electronics/54268
        String filename = carrier.getName().toString();
        int start = filename.lastIndexOf("/", filename.lastIndexOf("/") - 1);
        //截取后，仅保留sci.electronics/54268
        String name = filename.substring(start + 1);


        try {
            TokenSequence ts = (TokenSequence) carrier.getData();
            if(type==Type.PURE_ESA || type==Type.PURE_ESPM || type==Type.PURE_ESPM_ESA) {
                ts = new TokenSequence();
            }

            if (type == Type.BOW_ESA || type == Type.BOW_ESPM_ESA
                    || type==Type.PURE_ESA || type==Type.PURE_ESPM_ESA) {
                File f = new File(homeDir, "esa/" + name);
                List<String> lines = Files.readLines(f, Charsets.UTF_8);
                int count = 0;
                for (String line : lines) {
                    List<String> items = Splitter.on("|").splitToList(line);
                    if (items.size() > 2) {
                        Token token = new Token("ESA:" + items.get(1));
                        ts.add(token);
                    }

                    count++;
                    if(count>=50) break;
                }
            }

            if (type == Type.BOW_ESPM || type == Type.BOW_ESPM_ESA
                    || type==Type.PURE_ESPM || type==Type.PURE_ESPM_ESA) {
                File f = new File(homeDir, "espm/" + name);
                List<String> lines = Files.readLines(f, Charsets.UTF_8);
                int count = 0;
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;

                    String path = line.substring(0, line.lastIndexOf("|"));
                    for (String item : Splitter.on("/").splitToList(path)) {
                        ts.add(new Token("ESPM:" + item));
                    }

                    count++;
                    if(count>=20) break;
                }
            }

            carrier.setData(ts);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return carrier;
    }
}
