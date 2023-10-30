package com.github.pwrlabs.pwrj.Transaction;

public class Transaction {
    private final int size;
    private final int positionInTheBlock;
    private final long fee;
    private final String type;
    private final String from;
    private final String to;
    private final String hash;

    public Transaction(int size, int positionInTheBlock, long fee, String type, String from, String to, String hash) {
        this.size = size;
        this.positionInTheBlock = positionInTheBlock;
        this.fee = fee;
        this.type = type;
        this.from = from;
        this.to = to;
        this.hash = hash;
    }

    //Getters

    public int getSize() {
        return size;
    }

    public int getPositionInTheBlock() {
        return positionInTheBlock;
    }

    public long getFee() {
        return fee;
    }

    public String getType() {
        return type;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getHash() {
        return hash;
    }



}
