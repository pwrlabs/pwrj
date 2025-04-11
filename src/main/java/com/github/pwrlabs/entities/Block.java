package com.github.pwrlabs.entities;

import io.pwrlabs.utils.BinaryJSONKeyMapper;
import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

@Getter
public class Block {
    private final long blockNumber;
    private final long timeStamp;
    private final long blockReward;
    private final long burnedFees;
    private final int blockSize;
    private final int blockchainVersion;
    private final String blockHash;
    private final String previousBlockHash;
    private final String rootHash;
    private final String proposer;
    private List<String> transactionHash;

    public Block(JSONObject blockJson) {
        this.blockNumber = blockJson.getLong(BinaryJSONKeyMapper.BLOCK_NUMBER);
        this.timeStamp = blockJson.getLong(BinaryJSONKeyMapper.TIME_STAMP);
        this.blockReward = blockJson.getLong(BinaryJSONKeyMapper.BLOCK_REWARD);
        this.burnedFees = blockJson.getLong(BinaryJSONKeyMapper.BURNED_FEES);
        this.blockSize = blockJson.getInt(BinaryJSONKeyMapper.SIZE);
        this.blockchainVersion = blockJson.getInt(BinaryJSONKeyMapper.BLOCKCHAIN_VERSION);
        this.blockHash = blockJson.getString(BinaryJSONKeyMapper.BLOCK_HASH);
        this.previousBlockHash = blockJson.getString(BinaryJSONKeyMapper.PREVIOUS_BLOCK_HASH);
        this.rootHash = blockJson.getString(BinaryJSONKeyMapper.ROOT_HASH);
        this.proposer = blockJson.getString(BinaryJSONKeyMapper.PROPOSER);

        JSONArray transactionHashJson = blockJson.getJSONArray(BinaryJSONKeyMapper.TRANSACTION_HASH);
        for (int i = 0; i < transactionHashJson.length(); i++) {
            this.transactionHash.add(transactionHashJson.getString(i));
        }
    }

}
