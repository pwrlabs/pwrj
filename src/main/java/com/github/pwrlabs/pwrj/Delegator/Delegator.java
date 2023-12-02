package com.github.pwrlabs.pwrj.Delegator;

public class Delegator {
    private final String address;
    private final String validatorAddress;
    private final long shares;
    private final long delegatedPWR;

    public Delegator(String address, String validatorAddress, long shares, long delegatedPWR) {
        this.address = address;
        this.validatorAddress = validatorAddress;
        this.shares = shares;
        this.delegatedPWR = delegatedPWR;
    }

    //Getters

    /**
     * @return the address of the delegator
     */
    public String getAddress() {
        return address;
    }

    /**
     * @return the address of the validator
     */
    public String getValidatorAddress() {
        return validatorAddress;
    }

    /**
     * @return the shares of the delegator
     */
    public long getShares() {
        return shares;
    }

    /**
     * @return the delegated PWR of the delegator
     */
    public long getDelegatedPWR() {
        return delegatedPWR;
    }


}
