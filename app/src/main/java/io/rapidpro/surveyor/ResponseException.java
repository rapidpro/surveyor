package io.rapidpro.surveyor;

/**
 * Created by eric on 7/28/16.
 */
public class ResponseException extends TembaException {

    public ResponseException(String message) {
        super(message);
    }

    public ResponseException(Exception e) {
        super(e);
    }

    public ResponseException(String message, Exception e) {
        super(message, e);
    }
}
