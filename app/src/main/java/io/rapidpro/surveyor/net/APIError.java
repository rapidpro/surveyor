package io.rapidpro.surveyor.net;

public class APIError {

    private int statusCode;
    private String message;

    public APIError() {
    }

    public APIError(int status) {
        statusCode = status;
    }

    public int getStatus() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }
}