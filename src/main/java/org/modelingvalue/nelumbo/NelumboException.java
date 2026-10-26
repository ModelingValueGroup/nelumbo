package org.modelingvalue.nelumbo;

public abstract class NelumboException extends RuntimeException {

    private static final long serialVersionUID = 2552128208267407358L;

    public NelumboException(String message) {
        super(message);
    }

    public NelumboException(String message, Throwable cause) {
        super(message, cause);
    }

    public NelumboException(Throwable cause) {
        super(cause);
    }

}
