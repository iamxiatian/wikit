package ruc.irm.wikit.data.dump.parse;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikiTextParser {
    private static final Pattern CATEGORY_PATTERN = Pattern.compile("\\[\\[Category\\:([^\\]|^\\|]+)(\\|[^\\]]+)?\\]\\]", Pattern.CASE_INSENSITIVE);

    public static Set<String> parseCategories(String source) {
        Set<String> categories = new HashSet<>();
        Matcher matcher = CATEGORY_PATTERN.matcher(source);
        while (matcher.find()) {
            categories.add(matcher.group(1));
        }

        //skip numbers
        return categories;
    }

    private static final Pattern CATEGORY_REDIRECT_PATTERN = Pattern.compile("\\{\\{(分类重定向|cr)\\|([^\\}]+)\\}\\}", Pattern.CASE_INSENSITIVE);

    /**
     * 重定向语法：
     * {{cr|基督教新教}}
     * {{分类重定向|历史学书籍}}
     * @param source
     * @return
     */
    public static String parseCategoryRedirect(String source) {
        Matcher matcher = CATEGORY_REDIRECT_PATTERN.matcher(source);
        while (matcher.find()) {
            return matcher.group(2);
        }

        return null;
    }

    private static final Pattern COMMONS_CATEGORY_PATTERN = Pattern.compile("\\{\\{Commonscat\\|([^\\}]+)\\}\\}", Pattern.CASE_INSENSITIVE);

    /**
     * 在维基百科条目中引用其姊妹计划——维基共享资源的分类:
     * {{Catnav|页面分类|人物|出生|20世纪出生|1930年代出生}}\n\n{{出生年|193|3}}\n{{Commonscat|1933 births}}
     */
    public static String parseCommonsCat(String source) {
        Matcher matcher = COMMONS_CATEGORY_PATTERN.matcher(source);
        while (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    public static void main(String[] args) {
        String[] array = new String[]{
                "{{filmyr|200|4}}\n{{Commonscat|2004 in film}}",
                "sfdsfajsd[[category:中国]]hsdlf[[Category:中国2|sd]]jsdl", "{{Catnav|页面分类|人物|出生|20世纪出生|1930年代出生}}\\n\\n{{出生年|193|3}}\\n{{Commonscat|1933 births}}", "{{分类重定向|历史学书籍}}", "1978年{{分类重定向|历史学书籍}}ss", "{{cr|基督教新教}}%", "0.25", "中国"
        };

        for (String s : array) {
//            System.out.println(s + "==>" + parseCategories(s));
//            System.out.println(s + "==>" + parseCategoryRedirect(s));
            System.out.println(s + "==>" + parseCommonsCat(s));
        }

    }
}
