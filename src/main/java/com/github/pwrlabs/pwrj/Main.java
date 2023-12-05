package com.github.pwrlabs.pwrj;

import com.github.pwrlabs.pwrj.Utils.Response;
import com.github.pwrlabs.pwrj.protocol.PWRJ;
import com.github.pwrlabs.pwrj.wallet.PWRWallet;

import java.io.IOException;
import java.math.BigInteger;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        PWRJ.setRpcNodeUrl("http://localhost:8085/");
        PWRWallet wallet = new PWRWallet(new BigInteger("48157030754737918552913355043337580418007638602253380155554472945119041505152"));
        System.out.println(wallet.getAddress());

//        wallet.join("11.11.11.11");

//        Response r = wallet.delegate(wallet.getAddress(), 10000000L);
//        System.out.println(r.isSuccess());
//        System.out.println(r.getTxnHash());
//        System.out.println(r.getError());
//
        Response r = wallet.claimActiveNodeSpot();
        System.out.println(r.isSuccess());
        System.out.println(r.getTxnHash());
        System.out.println(r.getError());
    }
}
