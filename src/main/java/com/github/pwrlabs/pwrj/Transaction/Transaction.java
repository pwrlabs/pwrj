package com.github.pwrlabs.pwrj.Transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@Getter
@SuperBuilder
public class Transaction {
    private final int size;
    private final long blockNumber;
    private final int positionInTheBlock;
    private final long fee;
    private final String type;
    private final String sender;
    private final String receiver;
    private final int nonce;
    private final String hash;
    private final long timestamp;
    private final long value;

    public Transaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        this.size = json.optInt("size", 0);
        this.positionInTheBlock = positionInTheBlock;
        this.fee = json.optLong("fee", 0);
        this.type = json.optString("type", "unknown");
        this.sender = json.optString("sender", "0x");
        this.receiver = json.optString("receiver", "0x");
        this.nonce = json.optInt("nonce", 0);
        this.hash = json.optString("hash", "0x");
        this.blockNumber = blockNumber;
        this.timestamp = timestamp;
        this.value = json.optLong("value", 0);
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        json.put("size", size);
        json.put("positionInTheBlock", positionInTheBlock);
        json.put("fee", fee);
        json.put("type", type);
        json.put("sender", sender);
        json.put("receiver", receiver);
        json.put("nonce", nonce);
        json.put("hash", hash);
        json.put("blockNumber", blockNumber);
        json.put("nonce", nonce);
        json.put("timestamp", timestamp);
        json.put("value", value);

        return json;
    }

    public static Transaction fromJSON(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        String txnType = json.optString("type", "Unknown");

        if(txnType.equalsIgnoreCase(TransferTxn.type)) {
            return new TransferTxn(json, blockNumber, timestamp, positionInTheBlock);
        } else if(txnType.equalsIgnoreCase(VmDataTxn.type)) {
            return new VmDataTxn(json, blockNumber, timestamp, positionInTheBlock);
        } else if(txnType.equalsIgnoreCase(DelegateTxn.type)) {
            return new DelegateTxn(json, blockNumber, timestamp, positionInTheBlock);
        } else if(txnType.equalsIgnoreCase(WithdrawTxn.type)) {
            return new WithdrawTxn(json, blockNumber, timestamp, positionInTheBlock);
        } else if(txnType.equalsIgnoreCase(JoinTxn.type)) {
            return new JoinTxn(json, blockNumber, timestamp, positionInTheBlock);
        } else if(txnType.equalsIgnoreCase(ClaimVmIdTxn.type)) {
            return new ClaimVmIdTxn(json, blockNumber, timestamp, positionInTheBlock);
        } else if(txnType.equalsIgnoreCase(SetGuardianTxn.type)) {
            return new SetGuardianTxn(json, blockNumber, timestamp, positionInTheBlock);
        } else if(txnType.equalsIgnoreCase(PayableVmDataTxn.type)) {
            return new PayableVmDataTxn(json, blockNumber, timestamp, positionInTheBlock);
        } else if(txnType.equalsIgnoreCase(GuardianApprovalTxn.type)) {
            return new GuardianApprovalTxn(json, blockNumber, timestamp, positionInTheBlock);
        } else if(txnType.equalsIgnoreCase(ConduitApprovalTxn.type)) {
            return new ConduitApprovalTxn(json, blockNumber, timestamp, positionInTheBlock);
        } else if(txnType.equalsIgnoreCase(RemoveGuardianTxn.type)) {
            return new RemoveGuardianTxn(json, blockNumber, timestamp, positionInTheBlock);
        } else if(txnType.equalsIgnoreCase(ClaimSpotTxn.type)) {
            return new ClaimSpotTxn(json, blockNumber, timestamp, positionInTheBlock);
        } else {
            return new Transaction(json, blockNumber, timestamp, positionInTheBlock);
        }
    }


}
