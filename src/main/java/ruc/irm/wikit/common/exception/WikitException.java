package ruc.irm.wikit.common.exception;

/**
 * Wikit Base Exception
 *
 * @author Tian Xia
 * @date Dec 19, 2015 4:12 PM
 */
public class WikitException  extends Exception {
    public WikitException(Throwable throwable) {
        super(throwable);
    }

    public WikitException(String msg) {
        super(msg);
    }
}
