package com.github.pwrlabs.pwrj.Validator;

import com.github.pwrlabs.pwrj.Delegator.Delegator;
import com.github.pwrlabs.pwrj.protocol.PWRJ;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Validator {
    private final String address;
    private final String ip;
    private final boolean badActor;
    private final long votingPower;
    private final long shares;
    private final int delegatorsCount;
    private final String status;

    public Validator(String address, String ip, boolean badActor, long votingPower, long shares, int delegatorsCount, String status) {
        this.address = address;
        this.ip = ip;
        this.badActor = badActor;
        this.votingPower = votingPower;
        this.shares = shares;
        this.delegatorsCount = delegatorsCount;
        this.status = status;
    }

    //Getters

    /**
     * @return the address of the validator
     */
    public String getAddress() {
        return address;
    }

    /**
     * @return the ip of the validator
     */
    public String getIp() {
        return ip;
    }

    /**
     * @return true if the validator is a bad actor, false otherwise
     */
    public boolean isBadActor() {
        return badActor;
    }

    /**
     * @return the voting power of the validator
     */
    public long getVotingPower() {
        return votingPower;
    }

    /**
     * @return the shares of the validator
     */
    public long getShares() {
        return shares;
    }

    /**
     * @return the number of delegators of the validator
     */
    public int getDelegatorsCount() {
        return delegatorsCount;
    }

    /**
     * @return the status of the validator
     */
    public String getStatus() {
        return status;
    }

    public List<Delegator> getDelegators() {
        try {
            HttpClient client = HttpClients.createDefault();

            HttpGet request = new HttpGet(PWRJ.getRpcNodeUrl() + "/validator/delegatorsOfValidator/?validatorAddress=" + address);
            HttpResponse response = client.execute(request);

            //System.out.printf(EntityUtils.toString(response.getEntity()));

            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                System.out.println(object.toString());
                JSONObject delegators = object.getJSONObject("delegators");
                List<Delegator> delegatorsList = new LinkedList<>();

                for (String delegatorAddress: delegators.keySet()) {
                    long shares = delegators.getLong(delegatorAddress);
                    long delegatedPWR = BigInteger.valueOf(shares).multiply(BigInteger.valueOf(this.votingPower)).divide(BigInteger.valueOf(this.shares)).longValue();
                    Delegator d = new Delegator("0x" + delegatorAddress, address, shares, delegatedPWR);

                    delegatorsList.add(d);
                }

                return delegatorsList;
            } else if (response.getStatusLine().getStatusCode() == 400) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                throw new RuntimeException("Failed with HTTP error 400 and message: " + object.getString("message"));
            } else {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatusLine().getStatusCode());
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new LinkedList<>();
        }

    }

}
