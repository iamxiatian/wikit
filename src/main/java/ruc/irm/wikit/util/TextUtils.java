package ruc.irm.wikit.util;

/**
 * User: xiatian
 * Date: 4/7/14
 * Time: 12:24 AM
 */
public class TextUtils {

    public static boolean isNumberOrDate(String source) {
        String s = source.trim();
        boolean hasNumber = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= '0' && ch <= '9' || ch == '.' || ch == '%' || ch == '年' || ch == '月' || ch == '日') {
                if (ch >= '0' && ch <= '9') {
                    hasNumber = true;
                }
                continue;
            } else {
                return false;
            }
        }

        //skip numbers
        return hasNumber;
    }

    public static void main(String[] args) {
        String[] array = new String[]{
                "5月4日", "年月日", "1978年", "25%", "0.25", "中国"
        };

        for (String s : array) {
            System.out.println(s + "==>" + isNumberOrDate(s));
        }

    }
}
