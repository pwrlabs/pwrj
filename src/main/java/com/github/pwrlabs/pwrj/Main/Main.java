package com.github.pwrlabs.pwrj.Main;

import com.github.pwrlabs.pwrj.Utils.Response;
import com.github.pwrlabs.pwrj.protocol.PWRJ;
import com.github.pwrlabs.pwrj.wallet.PWRWallet;

import java.math.BigInteger;

public class Main {


    public static void main(String[] args) throws Exception {
        PWRJ.setRpcNodeUrl("http://localhost:8085/");
        PWRWallet wallet = new PWRWallet(new BigInteger("39104763650379367717310524491242454405164680811325363855668185181721146718196"));
        System.out.println("Address: " + wallet.getAddress());
        System.out.println("Balance: " + wallet.getBalance());

        Response r = wallet.delegate("0x61bd8fc1e30526aaf1c4706ada595d6d236d9883", 10000000);

        if(r.isSuccess()) {
            System.out.println("Txn Hash: " + r.getMessage());
        } else {
            System.out.println("Fail: " + r.getError());
        }
    }
}
