package reactor;

/**
 * Created by jorgeluis on 30/05/16.
 */
public class IllegalTableException extends Exception {

    public IllegalTableException() { super(); }
    public IllegalTableException(String message) { super(message); }
    public IllegalTableException(String message, Throwable cause) { super(message, cause); }
    public IllegalTableException(Throwable cause) { super(cause); }
}
