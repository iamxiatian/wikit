package ruc.irm.wikit.espm.impl;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.NeighborIndex;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ruc.irm.wikit.espm.SemanticPath;
import ruc.irm.wikit.espm.graph.CategoryTreeGraph;

import java.util.List;
import java.util.Set;

/**
 * Maximum Weighted Independent Set
 *
 * @see <a href="http://www.cs.cmu.edu/~deepay/mywww/papers/www10-wrappers.pdf">www 10-The Paths More Taken: Matching DOM Trees to Search Logs for Accurate Webpage Clustering</a>
 *
 * @author   xiatian
 * @date 5/19/14
 * @time: 6:09 PM
 */
public class GreedyMWIS {
    private Logger LOG = LoggerFactory.getLogger(GreedyMWIS.class);

    private UndirectedGraph<SemanticPath, DefaultEdge> graph = null;
    private NeighborIndex index = null;
    private CategoryTreeGraph tree = null;

    public GreedyMWIS(CategoryTreeGraph treeGraph) {
        this.graph = new SimpleGraph<SemanticPath, DefaultEdge>(DefaultEdge.class);
        this.index = new NeighborIndex(graph);
        this.tree = treeGraph;
    }


    public void buildMWISGraph(List<SemanticPath> paths) {
        for (SemanticPath path : paths) {
                addPath(path);
        }
    }

    public void addPath(SemanticPath addingPath) {
        graph.addVertex(addingPath);

        Set<SemanticPath> vertices = graph.vertexSet();
        for (SemanticPath vertex: vertices) {
            //skip self check for edge connection
            if(vertex==addingPath) {
                continue;
            }

            if (getDependentPossibility(vertex, addingPath)>0.7) {
                graph.addEdge(vertex, addingPath);
            }
        }
    }

    private double getDependentPossibility(SemanticPath path1, SemanticPath path2) {
        int minDepth = path1.length();
        int maxDepth = path2.length();
        if(minDepth>maxDepth) {
            minDepth = maxDepth;
            maxDepth = path1.length();
        }

        if(path1.getLastNode(0)==path2.getLastNode(0)) {
        //@IMPORTANT 到达叶子节点的路径，只保留一条
            return 1.0;
        }
        // sum = (first＋last)×count÷2
        //int totalWeight = (1+minDepth)*minDepth/2;
        double totalWeight = 0;

        double currentWeight = 0;
        for (int i = 0; i < minDepth; i++) {
            double weight =(minDepth-i);
            //double weight = Math.log(minDepth-i + 1)/Math.log(2);
            totalWeight += weight;
            int node1 = path1.id(i);
            int node2 = path2.id(i);

            currentWeight += (minDepth-i)* getSimilarity(node1, node2, i);
            //currentWeight += weight * getSimilarity(node1, node2, i);
        }

        //return 1.0;
        totalWeight += (maxDepth - minDepth);
        //totalWeight += Math.log(maxDepth - minDepth+1)/Math.log(2);

        return currentWeight / totalWeight;
    }

    private double getSimilarity(int c1, int c2, int depth) {
        if (c1 == c2) {
            return 1.0;
        } else {
            return 0;
//            Set<Integer> p1 = tree.getParentIds(c1);
//            Set<Integer> p2 = tree.getParentIds(c2);
//            int differentItems = p1.size() + p2.size();
//            int both = 0;
//            for(int id: p1) {
//                if(p2.contains(p1)) {
//                    both++;
//                    differentItems--;
//                }
//            }
//
//            return (1 - 1.0 / (depth+1)) * both/differentItems;
        }
    }

    public SemanticPath findTopNode() {
        int maxId = -1;
        double maxWeight = -1;
        SemanticPath maxPath = null;

        Set<SemanticPath> vertices = graph.vertexSet();

        for (SemanticPath vertex: vertices) {
            double weight = vertex.getAvgWeight();
            if (maxPath==null || weight> maxPath.getAvgWeight()) {
                maxPath = vertex;
            }
        }

        if (maxPath != null) {
            Set<SemanticPath> neighbors = index.neighborsOf(maxPath);

            for (SemanticPath neighbor : neighbors) {
                graph.removeVertex(neighbor);
            }

            graph.removeVertex(maxPath);
        }

        return maxPath;
    }
//
//    public void export() {
//        Gexf gexf = new GexfImpl();
//        Calendar date = Calendar.getInstance();
//
//        gexf.getMetadata()
//                .setLastModified(date.getTime())
//                .setCreator("Gephi.org")
//                .setDescription("Category Dependency Network");
//
//
//        Graph graph = gexf.getGraph();
//        graph.setDefaultEdgeType(EdgeType.UNDIRECTED)
//                .setMode(Mode.DYNAMIC)
//                .setTimeType(TimeFormat.XSDDATETIME);
//
//        AttributeList attrList = new AttributeListImpl(AttributeClass.NODE);
//        graph.getAttributeLists().add(attrList);
//
//        Attribute attUrl = attrList.createAttribute("0", AttributeType.STRING, "url");
//        Attribute attIndegree = attrList.createAttribute("1", AttributeType.FLOAT, "indegree");
//        Attribute attFrog = attrList.createAttribute("2", AttributeType.BOOLEAN, "frog")
//                .setDefaultValue("true");
//
//
//        /** Node Gephi */
//        Node gephi = graph.createNode("0");
//        gephi .setLabel("Gephi")
//                .getAttributeValues()
//                .addValue(attUrl, "http://gephi.org")
//                .addValue(attIndegree, "1");
//
//        Spell spellGephi = new SpellImpl();
//        date.set(2012, 3, 28, 16, 10, 0);
//        date.set(Calendar.MILLISECOND, 0);
//        spellGephi.setStartValue(date.getTime());
//        gephi.getSpells().add(spellGephi);
//
//
//        /** Node Webatlas */
//        Node webatlas = graph.createNode("1");
//        webatlas
//                .setLabel("Webatlas")
//                .getAttributeValues()
//                .addValue(attUrl, "http://webatlas.fr")
//                .addValue(attIndegree, "2");
//
//        Spell spellWebatlas1 = new SpellImpl();
//        date.set(Calendar.MINUTE, 15);
//        spellWebatlas1.setStartValue(date.getTime());
//        date.set(2012, 3, 28, 18, 57, 2);
//        spellWebatlas1.setEndValue(date.getTime());
//        webatlas.getSpells().add(spellWebatlas1);
//
//        Spell spellWebatlas2 = new SpellImpl();
//        date.set(2012, 3, 28, 20, 31, 10);
//        spellWebatlas2.setStartValue(date.getTime()).setStartIntervalType(IntervalType.OPEN);
//        date.set(Calendar.MINUTE, 45);
//        date.set(Calendar.SECOND, 21);
//        spellWebatlas2.setEndValue(date.getTime());
//        webatlas.getSpells().add(spellWebatlas2);
//
//        Spell spellWebatlas3 = new SpellImpl();
//        date.set(2012, 3, 28, 21, 0, 0);
//        spellWebatlas3.setStartValue(date.getTime());
//        date.set(2012, 4, 11, 10, 49, 27);
//        spellWebatlas3.setEndValue(date.getTime()).setEndIntervalType(IntervalType.OPEN);
//        webatlas.getSpells().add(spellWebatlas3);
//
//
//        /** Node RTGI */
//        Node rtgi = graph.createNode("2");
//        rtgi
//                .setLabel("RTGI")
//                .getAttributeValues()
//                .addValue(attUrl, "http://rtgi.fr")
//                .addValue(attIndegree, "1");
//
//        Spell spellRtgi = new SpellImpl();
//        date.set(2012, 3, 27, 6, 0, 0);
//        spellRtgi.setStartValue(date.getTime());
//        date.set(2012, 4, 19);
//        spellRtgi.setEndValue(date.getTime());
//        rtgi.getSpells().add(spellRtgi);
//
//
//        /** Node BarabasiLab */
//        Node blab = graph.createNode("3");
//        blab
//                .setLabel("BarabasiLab")
//                .getAttributeValues()
//                .addValue(attUrl, "http://barabasilab.com")
//                .addValue(attIndegree, "3")
//                .addValue(attFrog, "false");
//
//
//        /** Node foobar */
//        Node foobar = graph.createNode("4");
//        foobar
//                .setLabel("FooBar")
//                .getAttributeValues()
//                .addValue(attUrl, "http://foo.bar")
//                .addValue(attIndegree, "1")
//                .addValue(attFrog, "false");
//
//
//        /** Edge 0 [gephi, webatlas] */
//        Edge edge0 = gephi.connectTo("0", webatlas);
//
//        Spell spellEdge0 = new SpellImpl();
//        date.set(2012, 3, 28, 16, 15, 36);
//        spellEdge0.setStartValue(date.getTime());
//        date.set(2012, 3, 28, 17, 41, 5);
//        spellEdge0.setEndValue(date.getTime());
//        edge0.getSpells().add(spellEdge0);
//
//
//        /** Edge 1 [gephi, rtgi] */
//        Edge edge1 = gephi.connectTo("1", rtgi);
//
//        Spell spellEdge1 = new SpellImpl();
//        date.set(2012, 3, 30, 11, 16, 6);
//        spellEdge1.setStartValue(date.getTime());
//        date.set(2012, 4, 3, 11, 52, 6);
//        spellEdge1.setEndValue(date.getTime());
//        edge1.getSpells().add(spellEdge1);
//
//
//        /** Edge 2 [rtgi, webatlas] */
//        Edge edge2 = rtgi.connectTo("2", webatlas);
//        Spell spellEdge2 = new SpellImpl();
//        date.set(2012, 4, 1, 11, 0, 0);
//        spellEdge2.setStartValue(date.getTime()).setStartIntervalType(IntervalType.OPEN);
//        date.set(2012, 4, 5, 11, 9, 44);
//        spellEdge2.setEndValue(date.getTime());
//        edge2.getSpells().add(spellEdge2);
//
//
//        /** Edge 3 [gephi, blab] */
//        Edge edge3 = gephi.connectTo("3", blab);
//        Spell spellEdge3 = new SpellImpl();
//        date.set(2012, 3, 30, 12, 13, 22);
//        spellEdge3.setStartValue(date.getTime());
//        date.set(Calendar.MINUTE, 58);
//        date.set(Calendar.SECOND, 24);
//        spellEdge3.setEndValue(date.getTime());
//        edge3.getSpells().add(spellEdge3);
//
//
//        /** Edge 4 [webatlas, blab] */
//        Edge edge4 = webatlas.connectTo("4", blab);
//        Spell spellEdge4 = new SpellImpl();
//        date.set(2012, 3, 30, 21, 2, 37);
//        spellEdge4.setStartValue(date.getTime());
//        date.set(Calendar.MINUTE, 13);
//        spellEdge4.setEndValue(date.getTime());
//        edge4.getSpells().add(spellEdge4);
//
//
//        /** Edge 5 [foobar, blab] */
//        foobar.connectTo("5", blab);
//
//
//        StaxGraphWriter graphWriter = new StaxGraphWriter();
//        File f = new File("dynamic_graph_sample.gexf");
//        Writer out;
//        try {
//            out =  new FileWriter(f, false);
//            graphWriter.writeToStream(gexf, out, "UTF-8");
//            System.out.println(f.getAbsolutePath());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public static void main(String[] args) {



    }
}
