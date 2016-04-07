package io.rapidpro.surveyor;

public class TembaException extends RuntimeException {

    public TembaException(String message) {
        super(message);
    }

    public TembaException (Exception e) {
        super(e);
    }
}
