package ruc.irm.wikit.util;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.dictionary.CustomDictionary;
import com.hankcs.hanlp.seg.common.Term;
import ruc.irm.wikit.common.conf.Conf;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:xiat@ruc.edu.cn">XiaTian</a>
 * @date Feb 11, 2015 11:47 AM
 */
public class Segment {
    private Conf conf = new Conf();

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
        text = "习近平和彭丽媛，微博和微信的竞争实在是然并卵";
        text = "法制晚报讯(记者 李文姬)今日就日本拟派自卫队舰船进入钓鱼岛海域一事，中国国防部首次回应发出警告回应称，中国军队将坚定捍卫国家的主权和安全利益，敦促日方不要颠倒黑白，混淆是非。";
        text = "李天一 李双江 彭丽媛与习近平";
        text = "'''徐才厚'''（{{bd|1943年|6月|2015年|3月15日|catIdx=X徐}}），[[辽宁]][[瓦房店市|瓦房店]]人，[[中国人民解放军]]前主要领导人之一，[[中国共产党]]第十七届[[中国共产党中央政治局|中央政治局]]委员、[[中国共产党中央军事委员会|中央军事委员会]]副主席，[[中国人民解放军上将|上将]]军衔（2014年被开除军籍）。1968年毕业于[[哈尔滨军事 工程学院]]，大学文化。曾任中共第十五届至第十七届中央委员，第十六届[[中国共产党中央书记处|中央书记处]]书记、[[中国人民解放军总政治部|总政治部]]主任，曾经长期在[[沉阳军区]]服役。";
        CustomDictionary.add("然并卵", "n 10");
        //com.hankcs.hanlp.seg.Segment segment = HanLP.newSegment();
        //segment.enablePartOfSpeechTagging(true);
        for (Term t : HanLP.segment(text)) {
            System.out.print(t.word+"/" + t.nature.name() + " ");
        }
    }
}
