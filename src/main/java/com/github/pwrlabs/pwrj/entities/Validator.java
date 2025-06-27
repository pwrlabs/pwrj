package com.github.pwrlabs.pwrj.entities;

import com.github.pwrlabs.pwrj.protocol.PWRJ;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

@Getter
@SuperBuilder
/**
 * Validator class.
 */
public class Validator {
    private final String address;
    private final String ip;
    private final boolean isBadActor;
    private final long votingPower;
    private final BigInteger shares;
    private final int delegatorsCount;
    private final String status;

    public Validator(JSONObject object) {
        this.address = object.optString("address", null);
        this.ip = object.optString("ip", null);
        this.isBadActor = object.optBoolean("isBadActor", false);
        this.votingPower = object.optLong("votingPower", 0);
        this.shares = object.optBigInteger("shares", BigInteger.valueOf(0));
        this.delegatorsCount = object.optInt("delegatorsCount", 0);
        this.status = object.optString("status", null);
    }

/**
 * getSharesPerSpark method.
 * @return value
 */
    public BigInteger getSharesPerSpark() {
        if (shares.compareTo(BigInteger.valueOf(0)) == 0) return BigInteger.valueOf(1000000000);
        else return shares.divide(BigInteger.valueOf(getVotingPower()));
    }

/**
 * main method.
 * @param args parameter
 * @throws Exception exception
 */
    public static void main(String[] args) throws Exception {
        PWRJ pwrj = new PWRJ("https://pwrrpc.pwrlabs.io");
        List<Validator> validators = pwrj.getAllValidators();
        for (Validator validator : validators) {
            System.out.println("Address: " + validator.getAddress());
            System.out.println("IP: " + validator.getIp());
            System.out.println("Is Bad Actor: " + validator.isBadActor());
            System.out.println("Voting Power: " + validator.getVotingPower());
            System.out.println("Shares: " + validator.getShares());
            System.out.println("Delegators Count: " + validator.getDelegatorsCount());
            System.out.println("Status: " + validator.getStatus());
            System.out.println();
        }
    }
}
