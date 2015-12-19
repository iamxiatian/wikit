package ruc.irm.wikit.util;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.dictionary.CustomDictionary;
import com.hankcs.hanlp.seg.common.Term;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:xiat@ruc.edu.cn">XiaTian</a>
 * @date Feb 11, 2015 11:47 AM
 */
public class Segment {
    public static List<String> segment(String text) {
        List<String> results = new ArrayList<>();
        for (Term t : HanLP.segment(text)) {
            results.add(t.word);
        }
        return results;
    }

    public static void main(String[] args) {
        String text =
                "微博的用户增长率增长迅速，张晓麟说：标准分词是最常用的分词器，基于HMM-Viterbi" +
                        "实现，开启了中国人名识别和音译人名识别，调用方法如下:";
        text = "习近平指出：南海诸岛自古以来就是中国领土，这是老祖宗留下的。任何人要侵犯中国的主权和相关权益，中国人民都不会答应。中国在南海采取的有关行动，是维护自身领土主权的正当反应。";
        text = "微博和微信的竞争实在是然并卵";
        CustomDictionary.add("然并卵", "n 10");
        //com.hankcs.hanlp.seg.Segment segment = HanLP.newSegment();
        //segment.enablePartOfSpeechTagging(true);
        for (Term t : HanLP.segment(text)) {
            System.out.print(t.word+"/" + t.nature.name() + " ");
        }
    }
}
