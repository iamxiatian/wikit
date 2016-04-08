package ruc.irm.wikit.mining.keyword;

import java.util.HashMap;
import java.util.Map;

/**
 * 一个词语，包含文本在句子中
 * User: xiatian
 * Date: 3/10/13 3:03 PM
 */
public class WordNode {

    /**
     * 词语的名称
     */
    private String name;

    /**
     * 词性
     */
    private String pos;

    /**
     * 词语在文本中出现的数量
     */
    private int count;

    /**
     * 词语的重要性，如果在标题中出现，为λ(λ>1, 默认为5）, ,否则为1
     */
    private double importance = 1;

    /**
     * 当前节点所指向的节点名称及其出现次数
     */
    private Map<String, Integer> adjacentWords = new HashMap<String, Integer>();

    public WordNode() {
    }

    public WordNode(String name, String pos, int count, double importance) {
        this.name = name;
        this.pos = pos;
        this.count = count;
        this.importance = importance;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPos() {
        return pos;
    }

    public void setPos(String pos) {
        this.pos = pos;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public double getImportance() {
        return importance;
    }

    public void setImportance(double importance) {
        this.importance = importance;
    }

    public Map<String, Integer> getAdjacentWords() {
        return adjacentWords;
    }

    public void setAdjacentWords(Map<String, Integer> adjacentWords) {
        this.adjacentWords = adjacentWords;
    }

    public void addAdjacentWord(String word) {
        if (adjacentWords.containsKey(word)) {
            adjacentWords.put(word, adjacentWords.get(word) + 1);
        } else {
            adjacentWords.put(word, 1);
        }
    }

    @Override
    public String toString() {
        return "WordNode{" +
                "name='" + name + '\'' +
                ", pos='" + pos + '\'' +
                ", count=" + count +
                ", importance=" + importance +
                '}';
    }
}
