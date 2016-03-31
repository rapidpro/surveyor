package io.rapidpro.surveyor.net;

public class APIError {

    private int statusCode;
    private String message;

    public APIError() {
    }

    public APIError(int status, String message) {
        this.statusCode = status;
        this.message = message;
    }

    public int getStatus() {
        return statusCode;
    }

    public String getMessage() {
        if (message != null) {
            return message;
        } else {
            return "Server error, please try again.";
        }
    }
}