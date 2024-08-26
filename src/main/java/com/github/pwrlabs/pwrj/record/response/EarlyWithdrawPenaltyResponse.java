package com.github.pwrlabs.pwrj.record.response;

public class EarlyWithdrawPenaltyResponse {
    private boolean earlyWithdrawAvailable;
    private long penalty;

    public EarlyWithdrawPenaltyResponse(boolean earlyWithdrawAvailable, long penalty) {
        this.earlyWithdrawAvailable = earlyWithdrawAvailable;
        this.penalty = penalty;
    }

    public boolean isEarlyWithdrawAvailable() {
        return earlyWithdrawAvailable;
    }

    public long getPenalty() {
        return penalty;
    }
}
