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
        String TransactionType = json.optString("type", "Unknown");

        if(TransactionType.equalsIgnoreCase(TransferTransaction.type)) {
            return new TransferTransaction(json, blockNumber, timestamp, positionInTheBlock);
        } else if(TransactionType.equalsIgnoreCase(VmDataTransaction.type)) {
            return new VmDataTransaction(json, blockNumber, timestamp, positionInTheBlock);
        } else if(TransactionType.equalsIgnoreCase(DelegateTransaction.type)) {
            return new DelegateTransaction(json, blockNumber, timestamp, positionInTheBlock);
        } else if(TransactionType.equalsIgnoreCase(WithdrawTransaction.type)) {
            return new WithdrawTransaction(json, blockNumber, timestamp, positionInTheBlock);
        } else if(TransactionType.equalsIgnoreCase(JoinTransaction.type)) {
            return new JoinTransaction(json, blockNumber, timestamp, positionInTheBlock);
        } else if(TransactionType.equalsIgnoreCase(ClaimVmIdTransaction.type)) {
            return new ClaimVmIdTransaction(json, blockNumber, timestamp, positionInTheBlock);
        } else if(TransactionType.equalsIgnoreCase(SetGuardianTransaction.type)) {
            return new SetGuardianTransaction(json, blockNumber, timestamp, positionInTheBlock);
        } else if(TransactionType.equalsIgnoreCase(PayableVmDataTransaction.type)) {
            return new PayableVmDataTransaction(json, blockNumber, timestamp, positionInTheBlock);
        } else if(TransactionType.equalsIgnoreCase(GuardianApprovalTransaction.type)) {
            return new GuardianApprovalTransaction(json, blockNumber, timestamp, positionInTheBlock);
        } else if(TransactionType.equalsIgnoreCase(ConduitApprovalTransaction.type)) {
            return new ConduitApprovalTransaction(json, blockNumber, timestamp, positionInTheBlock);
        } else if(TransactionType.equalsIgnoreCase(RemoveGuardianTransaction.type)) {
            return new RemoveGuardianTransaction(json, blockNumber, timestamp, positionInTheBlock);
        } else if(TransactionType.equalsIgnoreCase(ClaimSpotTransaction.type)) {
            return new ClaimSpotTransaction(json, blockNumber, timestamp, positionInTheBlock);
        } else {
            return new Transaction(json, blockNumber, timestamp, positionInTheBlock);
        }
    }


}
