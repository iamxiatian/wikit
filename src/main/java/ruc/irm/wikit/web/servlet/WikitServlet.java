package ruc.irm.wikit.web.servlet;

import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.esa.ESAModel;
import ruc.irm.wikit.esa.ESAModelImpl;
import ruc.irm.wikit.esa.concept.ConceptCacheRedisImpl;
import ruc.irm.wikit.esa.concept.vector.ConceptIterator;
import ruc.irm.wikit.esa.concept.vector.ConceptVector;
import ruc.irm.wikit.espm.NameCodeMapping;
import ruc.irm.wikit.espm.SemanticPath;
import ruc.irm.wikit.espm.SemanticPathMining;
import ruc.irm.wikit.espm.impl.SemanticPathMiningWikiImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;

/**
 * Servlet3.0支持使用注解配置Servlet。我们只需在Servlet对应的类上使用@WebServlet进行标注，
 * 我们就可以访问到该Servlet了，而不需要再在web.xml文件中进行配置。@WebServlet的urlPatterns
 * 和value属性都可以用来表示Servlet的部署路径，它们都是对应的一个数组。
 *
 * @author Tian Xia
 * @date Sep 18, 2016 15:23
 */
@WebServlet(name = "EspmServlet", urlPatterns = "/espm")
public class WikitServlet extends HttpServlet {
    private static final long serialVersionUID = 232323232323231L;

    public WikitServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        this.doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException {
        String text = request.getParameter("t");
        String lang = request.getParameter("lang");

        response.setContentType("text/xml");
        response.setCharacterEncoding("utf-8");
        Writer writer = response.getWriter();

        StringBuilder sb = new StringBuilder();
        try {
            Conf conf = null;
            if ("en".equalsIgnoreCase(lang)) {
                conf = ConfFactory.createEnConf();
            } else {
                conf = ConfFactory.createZhConf();
            }

            ESAModel esaModel = new ESAModelImpl(conf);
            ConceptCacheRedisImpl conceptCache = new ConceptCacheRedisImpl(conf);
            SemanticPathMining ESPM = new SemanticPathMiningWikiImpl(conf);
            NameCodeMapping nameCodeMapping = NameCodeMapping.getInstance(conf);

            ConceptVector cv = esaModel.getCombinedVector(text, 20); //20
            if (cv == null) {
                System.out.println("No terms occurred in the input " +
                        "text, current limit is " + 20);
                writer.write("<result state=\"ERROR\">");
                writer.write("<![CDATA[");
                writer.write("没有有效的实体词语，原始输入为：" + text);
                writer.write("]]></result>");
                writer.close();
                return;
            }


            sb.append("<result state=\"OK\">\n");

            ConceptIterator it = cv.orderedIterator();
            int count = 0;
            while (it.next() && count++ < 5) {
                sb.append("\t<concept>").append(conceptCache.getNameById(it.getId())).append("</concept>\n");
            }

            int cptLimit = 20;
            SortedMap<Integer, Double> probability = ESPM.getCategoryDistribution(cv, cptLimit);
            //System.out.println(ESPM.printCategoryDistribution(probability));
            List<SemanticPath> paths = ESPM.getSemanticPaths(ESPM.constructCategoryTree(probability), 10);

            Set<String> genereated = new HashSet<>();

            NameCodeMapping.Node root = nameCodeMapping.getRoot();
            for (SemanticPath path : paths) {
                //分类名称（分类代码）/分类名称（分类代码）/分类名称（分类代码）/
                if(path.length()<4) continue;
                String name1 = path.name(1);
                String name2 = path.name(2);
                String name3 = path.name(3);

                String key = name1 + name2 + name3;
                if (genereated.contains(key)) {
                    continue;
                } else {
                    genereated.add(key);
                }

                sb.append("\t<path>").append(name1).append("(");
                NameCodeMapping.Node node1 = root.findChild(name1);
                if (node1 == null) {
                    sb.append("00)/");
                } else {
                    sb.append(node1.code).append(")/");
                }

                NameCodeMapping.Node node2 = null, node3 = null;
                if(node1!=null) {
                    node2 = node1.findChild(name2);
                }

                sb.append(name2).append("(");
                if (node2 == null) {
                    sb.append("000)/");
                } else {
                    sb.append(node2.code).append(")/");
                }

                if (node2 != null) {
                    node3 = node2.findChild(name3);
                }
                sb.append(name3).append("(");
                if (node3 == null) {
                    sb.append("000)/");
                } else {
                    sb.append(node3.code).append(")/");
                }
                sb.append("</path>\n");
            }
            sb.append("</result>");

        } catch (Exception e) {
            sb.setLength(0);
            sb.append("<result state=\"ERROR\">");
            sb.append("<![CDATA[");
            sb.append(e.getMessage());
            sb.append("]]></result>");

            e.printStackTrace();
        }
        writer.write(sb.toString());
        writer.close();
    }


}
