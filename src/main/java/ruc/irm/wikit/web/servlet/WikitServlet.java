package ruc.irm.wikit.web.servlet;

import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.esa.ESAModel;
import ruc.irm.wikit.esa.ESAModelImpl;
import ruc.irm.wikit.esa.concept.ConceptCacheRedisImpl;
import ruc.irm.wikit.esa.concept.vector.ConceptIterator;
import ruc.irm.wikit.esa.concept.vector.ConceptVector;
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
import java.util.List;
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
            Conf conf = new Conf();
            if ("en".equalsIgnoreCase(lang)) {
                conf.set("esa.language", "English");
            } else {
                conf.set("esa.language", "Chinese");
            }

            ESAModel esaModel = new ESAModelImpl(conf);
            ConceptCacheRedisImpl conceptCache = new ConceptCacheRedisImpl(conf);
            SemanticPathMining ESPM = new SemanticPathMiningWikiImpl(conf);

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

            ConceptIterator it = cv.orderedIterator();
            int cptLimit = 20;
            SortedMap<Integer, Double> probability = ESPM.getCategoryDistribution(cv, cptLimit);
            //System.out.println(ESPM.printCategoryDistribution(probability));
            List<SemanticPath> paths = ESPM.getSemanticPaths(ESPM.constructCategoryTree(probability), 10);

            sb.append("<result state=\"OK\">\n");
            for (SemanticPath path : paths) {
                sb.append("\t<path>").append(path.getPathString()).append("</path>\n");
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
