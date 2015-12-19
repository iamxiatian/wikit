package ruc.irm.wikit.common.exception;

/**
 *
 * @author Tian Xia
 * @date Dec 19, 2015 4:12 PM
 */
public class MissedException extends WikitException {
    public MissedException(String msg) {
        super(msg);
    }

    public MissedException(int id) {
        super("Does not exist for id " + id);
    }
}
