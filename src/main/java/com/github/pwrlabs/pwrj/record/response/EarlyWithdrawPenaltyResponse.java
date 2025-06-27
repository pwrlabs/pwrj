package com.github.pwrlabs.pwrj.record.response;

/**
 * EarlyWithdrawPenaltyResponse class.
 */
public class EarlyWithdrawPenaltyResponse {
    private boolean earlyWithdrawAvailable;
    private long penalty;

    public EarlyWithdrawPenaltyResponse(boolean earlyWithdrawAvailable, long penalty) {
        this.earlyWithdrawAvailable = earlyWithdrawAvailable;
        this.penalty = penalty;
    }

/**
 * isEarlyWithdrawAvailable method.
 * @return value
 */
    public boolean isEarlyWithdrawAvailable() {
        return earlyWithdrawAvailable;
    }

/**
 * getPenalty method.
 * @return value
 */
    public long getPenalty() {
        return penalty;
    }
}
