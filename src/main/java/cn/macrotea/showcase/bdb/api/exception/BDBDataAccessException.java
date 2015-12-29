package cn.macrotea.showcase.bdb.api.exception;

/**
 * @author macrotea@qq.com
 * @since 2014-8-8 下午7:26
 */
public class BDBDataAccessException extends RuntimeException {

    public BDBDataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public BDBDataAccessException(String message) {
        super(message);
    }

    public BDBDataAccessException() {
    }

    public static BDBDataAccessException throwMe(String message, Throwable cause) {
        throw new BDBDataAccessException(message, cause);
    }
}
