package com.github.pwrlabs.pwrj;

import com.github.pwrlabs.pwrj.Utils.Response;
import com.github.pwrlabs.pwrj.protocol.PWRJ;
import com.github.pwrlabs.pwrj.wallet.PWRWallet;

public class Main {
    public static void main(String[] args) throws Exception {
        PWRJ.setRpcNodeUrl("https://pwrrpc.pwrlabs.io/");

        PWRWallet wallet = new PWRWallet();
        System.out.println(wallet.getAddress());

        Thread.sleep(10000);

        Response r = wallet.claimActiveNodeSpot();
        System.out.println(r.isSuccess());
        System.out.println(r.getTxnHash());
        System.out.println(r.getError());


    }
}
