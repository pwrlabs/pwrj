package com.github.pwrlabs.pwrj.record.transaction.Interface;

import com.github.pwrlabs.pwrj.protocol.PWRJ;
import org.reflections.Reflections;
import com.github.pwrlabs.pwrj.Utils.Hex;
import com.github.pwrlabs.pwrj.record.transaction.ecdsa.*;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

@Getter
@SuperBuilder
public abstract class Transaction {
    public byte chainId;
    private final boolean hasError;
    private final int nonce, size, positionInTheBlock;
    private final String type, sender, receiver, hash, errorMessage;
    private final long timestamp, value, blockNumber, fee, extraFee, feePerByte;
    private final byte[] rawTransaction;
    private final JSONObject extraData;

    public Transaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        this.size = json.optInt("size", 0);
        this.positionInTheBlock = positionInTheBlock;
        this.fee = json.optLong("fee", 0L);
        this.extraFee = json.optLong("extraFee", 0L);
        this.feePerByte = json.optLong("feePerByte", 0L);
        this.type = json.optString("type", "unknown");
        this.sender = json.optString("sender", "0x");
        this.receiver = json.optString("receiver", "0x");
        this.nonce = json.optInt("nonce", 0);
        this.hash = json.optString("hash", "0x");
        this.blockNumber = blockNumber;
        this.timestamp = timestamp;
        this.value = json.optLong("value", 0);
        this.rawTransaction = Hex.decode(json.optString("rawTransaction", ""));
        this.chainId = (byte) json.optInt("chainId", 0);
        this.hasError = !json.optBoolean("success", true);
        this.errorMessage = json.optString("errorMessage", "");
        this.extraData = json.has("extraData") ? json.getJSONObject("extraData") : null;
    }

    public abstract int getIdentifier();

    public boolean hasError() {
        return hasError;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        json.put("size", size);
        json.put("positionInTheBlock", positionInTheBlock);
        json.put("fee", fee);
        json.put("extraFee", extraFee);
        json.put("feePerByte", feePerByte);
        json.put("type", type);
        json.put("sender", sender);
        json.put("receiver", receiver);
        json.put("nonce", nonce);
        json.put("hash", hash);
        json.put("blockNumber", blockNumber);
        json.put("nonce", nonce);
        json.put("timestamp", timestamp);
        json.put("value", value);
        json.put("rawTransaction", Hex.toHexString(rawTransaction));
        json.put("chainId", chainId);
        json.put("success", !hasError);
        json.put("errorMessage", errorMessage);

        return json;
    }

    public static Transaction fromJSON(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) throws Exception {
        String transactionType = json.optString("type", "Unknown");
        System.out.println("Transaction type: " + transactionType);

        Reflections reflections = new Reflections("com.github.pwrlabs.pwrj.record.transaction");
        Set<Class<? extends Transaction>> subclasses = reflections.getSubTypesOf(Transaction.class);
        System.out.println("Sub classes size: " + subclasses.size());

        for (Class<? extends Transaction> subclass : subclasses) {
            try {
                Transaction instance = subclass.getDeclaredConstructor(JSONObject.class, long.class, long.class, int.class)
                        .newInstance(json, blockNumber, timestamp, positionInTheBlock);
                if (transactionType.equalsIgnoreCase(instance.getType())) {
                    return instance;
                }
            } catch (Exception e) {}
        }

        throw new IllegalArgumentException("Unknown transaction type: " + transactionType);
    }

    public abstract String getType();
}
