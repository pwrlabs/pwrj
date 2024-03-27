package com.github.pwrlabs.pwrj.Utils;

public class Response {
    private final boolean success;
    private final String transactionHash;
    private final String error;

    public Response(boolean success, String transactionHash, String error) {
        this.success = success;
        this.transactionHash = transactionHash;
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
    public String getTransactionHash() {
        return transactionHash;
    }

    /**
     * @return error returned if the operation was not successful
     */
    public String getError() {
        return error;
    }
}
