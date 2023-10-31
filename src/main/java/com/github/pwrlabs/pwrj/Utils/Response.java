package com.github.pwrlabs.pwrj.Utils;

public class Response {
    private final boolean success;
    private final String message;
    private final String error;

    public Response(boolean success, String message, String error) {
        this.success = success;
        this.message = message;
        this.error = error;
    }

    /**
     * @return true if the operation was successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * @return message returned if the operation was successful
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return error returned if the operation was not successful
     */
    public String getError() {
        return error;
    }
}
