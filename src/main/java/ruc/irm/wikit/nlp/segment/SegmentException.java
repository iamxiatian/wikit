package ruc.irm.wikit.nlp.segment;

import java.io.Serializable;

/**
 * 切分异常类，对于切分过程出现的错误，抛出该异常
 */
public class SegmentException extends Error implements Serializable {
    private static final long serialVersionUID = 4384313232322L;

    public SegmentException(String message) {
        super(message);
    }

    public SegmentException(Throwable cause) {
        super(cause);
    }

    public SegmentException(String message, Throwable cause) {
        super(message, cause);
    }

}
