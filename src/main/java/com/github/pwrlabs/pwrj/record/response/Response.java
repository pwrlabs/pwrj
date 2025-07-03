package com.github.pwrlabs.pwrj.record.response;

/**
 * Response class.
 */
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
/**
 * isSuccess method.
 * @return value
 */
    public boolean isSuccess() {
        return success;
    }

    /**
     * @return message returned if the operation was successful
     */
/**
 * getTransactionHash method.
 * @return value
 */
    public String getTransactionHash() {
        return transactionHash;
    }

    /**
     * @return error returned if the operation was not successful
     */
/**
 * getError method.
 * @return value
 */
    public String getError() {
        return error;
    }

/**
 * toString method.
 * @return value
 */
    public String toString() {
        return "Response(success=" + this.isSuccess() + ", transactionHash=" + this.getTransactionHash() + ", error=" + this.getError() + ")";
    }
}
