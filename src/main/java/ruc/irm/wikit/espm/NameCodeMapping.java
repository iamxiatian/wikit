package ruc.irm.wikit.espm;

import com.google.common.io.Resources;
import org.apache.commons.lang3.StringUtils;
import ruc.irm.wikit.common.conf.Conf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * 对于ESPM识别的路径,我们希望对其每一个类别重新赋予一个代号, 该类别维持了深度为三级之内的
 * 所有类别和代号的对应关系, 其生成过程请参考: CategoryTreeGraph#exportLevel3Categories()
 * 其使用的地方请参考:WikitServlet
 *
 * @author Tian Xia
 * @date Sep 22, 2016 11:38
 */
public class NameCodeMapping {

    public static class Node {
        public String name;
        public String code;

        public Node(String code, String name) {
            this.name = name;
            this.code = code;
        }

        Map<String, Node> children = new HashMap<>();

        void addNode(Node child) {
            children.put(child.name, child);
        }

        public Node findChild(String childName) {
            return children.get(childName);
        }
    }

    private static Map<String, NameCodeMapping> instances = new HashMap<>();


    public static NameCodeMapping getInstance(Conf conf) {
        String lang = conf.getEsaLanguage().toLowerCase();

        if (instances.containsKey("lang")) {
            return instances.get("lang");
        } else {
            NameCodeMapping instance = new NameCodeMapping();
            if (lang.equals("chinese")) {
                try {
                    instance.load("categories_cn.txt");
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            } else if (lang.equals("english")) {
                try {
                    instance.load("categories_en.txt");
                } catch (IOException e) {
                    return null;
                }
            } else {
                return null;
            }
            instances.put(lang, instance);
            return instance;
        }
    }

    /**
     * 保存类别名称到类别代码的映射关系
     */
    private Map<String, String> mapping = new HashMap<>();

    private Node ROOT = new Node("0", "ROOT");

    private void load(String path) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Resources.getResource(path).openStream()))) {
            String line = null;

            Node L1Node = null;
            Node L2Node = null;
            Node L3Node = null;

            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;
                String[] items = StringUtils.split(line.trim(), "\t");

                if (!line.startsWith("\t")) {
                    L1Node = new Node(items[0], items[1]);
                    ROOT.addNode(L1Node);
                } else if (!line.startsWith("\t\t")) {
                    L2Node = new Node(items[0], items[1]);
                    L1Node.addNode(L2Node);
                } else {
                    L3Node = new Node(items[0], items[1]);
                    L2Node.addNode(L3Node);
                }
            }
        }
    }

    public String findCode(String name, Node parent) {
        if (parent == null) {
            parent = ROOT;
        }
        Node node = parent.children.get(name);
        if (node == null) {
            return parent == ROOT ? "00" : "000";
        } else {
            return node.code;
        }
    }

    public Node getRoot() {
        return this.ROOT;
    }
}
