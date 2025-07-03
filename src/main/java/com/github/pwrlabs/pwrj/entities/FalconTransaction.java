package com.github.pwrlabs.pwrj.entities;

import com.github.pwrlabs.pwrj.Utils.Hex;
import com.github.pwrlabs.pwrj.protocol.PWRJ;
import io.pwrlabs.utils.BinaryJSONKeyMapper;
import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.reflections.Reflections;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
/**
 * FalconTransaction class.
 */
public abstract class FalconTransaction {
    private final String transactionHash;
    private final String sender;
    private final int nonce;
    private final int size;
    private final int positionInBlock;
    private final long blockNumber;
    private final long timestamp;
    private final long feePerByte;
    private final long paidActionFee;
    private final long paidTotalFee;

    private final boolean wrapped;
    private final int positionInWrappedTransaction;

    private final boolean success;
    private final String errorMessage;

    public FalconTransaction(JSONObject json) {
        this.transactionHash = json.getString(BinaryJSONKeyMapper.TRANSACTION_HASH);
        this.sender = json.getString(BinaryJSONKeyMapper.SENDER);
        this.nonce = json.getInt(BinaryJSONKeyMapper.NONCE);
        this.size = json.getInt(BinaryJSONKeyMapper.SIZE);
        positionInBlock = json.getInt(BinaryJSONKeyMapper.POSITION_IN_BLOCK);
        blockNumber = json.getLong(BinaryJSONKeyMapper.BLOCK_NUMBER);
        timestamp = json.getLong(BinaryJSONKeyMapper.TIME_STAMP);
        this.feePerByte = json.getLong(BinaryJSONKeyMapper.FEE_PER_BYTE);
        this.paidActionFee = json.optLong(BinaryJSONKeyMapper.PAID_ACTION_FEE, 0);
        this.paidTotalFee = json.getLong(BinaryJSONKeyMapper.PAID_TOTAL_FEE);
        this.success = json.optBoolean(BinaryJSONKeyMapper.SUCCESS, false);
        this.errorMessage = json.optString(BinaryJSONKeyMapper.ERROR_MESSAGE, null);

        wrapped = json.optBoolean(BinaryJSONKeyMapper.WRAPPED, false);
        if(wrapped) {
            positionInWrappedTransaction = json.getInt(BinaryJSONKeyMapper.POSITION_IN_WRAPPED_TXN);
        } else {
            positionInWrappedTransaction = -1;
        }
    }

    public FalconTransaction() {
        this.transactionHash = null;
        this.sender = null;
        this.nonce = 0;
        this.size = 0;
        this.positionInBlock = 0;
        this.blockNumber = 0;
        this.timestamp = 0;
        this.feePerByte = 0;
        this.paidActionFee = 0;
        this.paidTotalFee = 0;
        this.success = false;
        this.errorMessage = null;

        wrapped = false;
        positionInWrappedTransaction = -1;
    }

/**
 * getIdentifier method.
 * @return value
 */
    public abstract int getIdentifier();

/**
 * getType method.
 * @return value
 */
    public abstract String getType();

/**
 * getReceiver method.
 * @return value
 */
    public abstract String getReceiver();

    //This function must be overriden by child classes to add more data to it
/**
 * toJson method.
 * @return value
 */
    public JSONObject toJson() {
        JSONObject data = new JSONObject();
        data.put("transactionHash", transactionHash);
        data.put("sender", sender);
        data.put("nonce", nonce);
        data.put("size", size);
        data.put("feePerByte", feePerByte);
        data.put("paidActionFee", paidActionFee);
        data.put("paidTotalFee", paidTotalFee);
        data.put("type", getType());
        return data;
    }

/**
 * fromJson method.
 * @param json parameter
 * @return value
 */
    public static FalconTransaction fromJson(JSONObject json) {
        String packageName = FalconTransaction.class.getPackage().getName();
        Reflections reflections = new Reflections(packageName);
        Set<Class<? extends FalconTransaction>> subclasses = reflections.getSubTypesOf(FalconTransaction.class);
        //System.out.println("Subclasses: " + subclasses.size());
        int identifier = json.getInt(BinaryJSONKeyMapper.IDENTIFIER);

        for (Class<? extends FalconTransaction> subclass : subclasses) {
            try {
                FalconTransaction instance = subclass.getDeclaredConstructor().newInstance();
                int thisInstanceIdentifier = instance.getIdentifier();
                //System.out.println("Identifier: " + thisInstanceIdentifier);

                if(thisInstanceIdentifier == identifier) {
//                    System.out.println("Found subclass: " + subclass.getName());
//                    System.out.println("Identifier: " + identifier);
//                    System.out.println(json);
                    return subclass.getDeclaredConstructor(JSONObject.class).newInstance(json);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        throw new IllegalArgumentException("Unknown transaction identifier: " + identifier);
    }

/**
 * main method.
 * @param args parameter
 * @throws Exception exception
 */
    public static void main(String[] args) throws Exception {
        PWRJ pwrj = new PWRJ("http://localhost:8085");

        Block block = pwrj.getBlockByNumber(2);
        for(String txn: block.getTransactionHashes()) {
            FalconTransaction transaction = pwrj.getTransactionByHash(txn);
            System.out.println(transaction.toJson());
        }
    }

    @Getter
/**
 * FalconTransfer class.
 */
    public static class FalconTransfer extends FalconTransaction {
        public static final int IDENTIFIER = 1006;

        private final String receiver;
        private final long amount;

        public FalconTransfer(JSONObject json) {
            super(json);

            receiver = json.getString(BinaryJSONKeyMapper.RECEIVER);
            amount = json.getLong(BinaryJSONKeyMapper.AMOUNT);
        }

        public FalconTransfer() {
            super();
            this.receiver = null;
            this.amount = 0;
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Falcon Transfer";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return receiver;
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("receiver", receiver);
            data.put("amount", amount);
            return data;
        }
    }

    @Getter
/**
 * SetPublicKey class.
 */
    public static class SetPublicKey extends FalconTransaction {
        public static final int IDENTIFIER = 1001;

        private final byte[] publicKey;

        public SetPublicKey(JSONObject json) {
            super(json);
            this.publicKey = json.getString(BinaryJSONKeyMapper.PUBLIC_KEY).getBytes();
        }

        public SetPublicKey() {
            super();
            this.publicKey = new byte[0];
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Set Public Key";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return "PWR Chain";
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("publicKey", publicKey);
            return data;
        }
    }

    @Getter
/**
 * FalconJoinAsValidator class.
 */
    public static class FalconJoinAsValidator extends FalconTransaction {
        public static final int IDENTIFIER = 1002;

        private final String ip;

        public FalconJoinAsValidator(JSONObject json) {
            super(json);
            this.ip = json.getString(BinaryJSONKeyMapper.IP);
        }

        public FalconJoinAsValidator() {
            super();
            this.ip = null;
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Join As Validator";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return "PWR Chain";
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("ip", ip);
            return data;
        }
    }

    @Getter
/**
 * FalconChangeIp class.
 */
    public static class FalconChangeIp extends FalconTransaction {
        public static final int IDENTIFIER = 1004;

        private final String newIp;

        public FalconChangeIp(JSONObject json) {
            super(json);
            this.newIp = json.getString(BinaryJSONKeyMapper.IP);
        }

        public FalconChangeIp() {
            super();
            this.newIp = null;
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Change IP";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return "PWR Chain";
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("newIp", newIp);
            return data;
        }
    }

    @Getter
/**
 * FalconDelegate class.
 */
    public static class FalconDelegate extends FalconTransaction {
        public static final int IDENTIFIER = 1003;

        private final String validator;
        private final long pwrAmount;

        public FalconDelegate(JSONObject json) {
            super(json);
            this.validator = json.getString(BinaryJSONKeyMapper.VALIDATOR_ADDRESS);
            this.pwrAmount = json.getLong(BinaryJSONKeyMapper.AMOUNT);
        }

        public FalconDelegate() {
            super();
            this.validator = null;
            this.pwrAmount = 0;
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Delegate";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return validator;
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("validator", validator);
            data.put("pwrAmount", pwrAmount);
            return data;
        }
    }

    @Getter
/**
 * FalconClaimActiveNodeSpot class.
 */
    public static class FalconClaimActiveNodeSpot extends FalconTransaction {
        public static final int IDENTIFIER = 1005;

        public FalconClaimActiveNodeSpot(JSONObject json) {
            super(json);
            // No additional fields to initialize
        }

        public FalconClaimActiveNodeSpot() {
            super();
            // No additional fields to initialize
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return "PWR Chain";
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Claim Active Node Spot";
        }
    }

    @Getter
/**
 * WithdrawTxn class.
 */
    public static class WithdrawTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1026;

        private final String validator;
        private final BigInteger sharesAmount;
        private final long withdrawnPwr;
        private final long remainingDelegatedPWR;

        public WithdrawTxn(JSONObject json) {
            super(json);
            this.validator = json.getString(BinaryJSONKeyMapper.VALIDATOR_ADDRESS);
            this.sharesAmount = new BigInteger(json.getString(BinaryJSONKeyMapper.SHARES_AMOUNT));
            this.withdrawnPwr = json.getLong(BinaryJSONKeyMapper.WITHDRAWN_PWR);
            this.remainingDelegatedPWR = json.getLong(BinaryJSONKeyMapper.REMAINING_DELEGATED_PWR);
        }

        public WithdrawTxn() {
            super();
            this.validator = null;
            this.sharesAmount = BigInteger.ZERO;
            this.withdrawnPwr = 0;
            this.remainingDelegatedPWR = 0;
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Withdraw";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return validator;
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("validator", validator);
            data.put("sharesAmount", sharesAmount.toString());
            return data;
        }
    }

    @Getter
/**
 * ClaimVidaId class.
 */
    public static class ClaimVidaId extends FalconTransaction {
        public static final int IDENTIFIER = 1028;

        private final long vidaId;

        public ClaimVidaId(JSONObject json) {
            super(json);
            this.vidaId = json.getLong(BinaryJSONKeyMapper.VIDA_ID);
        }

        public ClaimVidaId() {
            super();
            this.vidaId = 0;
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Claim VIDA ID";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return vidaId + "";
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("vidaId", vidaId);
            return data;
        }
    }

    @Getter
/**
 * RemoveValidatorTxn class.
 */
    public static class RemoveValidatorTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1025;

        private final String validatorAddress;

        public RemoveValidatorTxn(JSONObject json) {
            super(json);
            this.validatorAddress = json.getString(BinaryJSONKeyMapper.VALIDATOR_ADDRESS);
        }

        public RemoveValidatorTxn() {
            super();
            this.validatorAddress = null;
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Remove Validator";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return "PWR Chain";
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("validatorAddress", validatorAddress);
            return data;
        }
    }

    @Getter
/**
 * SetGuardianTxn class.
 */
    public static class SetGuardianTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1023;

        private final String guardianAddress;
        private final long expiryDate;

        public SetGuardianTxn(JSONObject json) {
            super(json);
            this.guardianAddress = json.getString(BinaryJSONKeyMapper.GUARDIAN_ADDRESS);
            this.expiryDate = json.getLong(BinaryJSONKeyMapper.GUARDIAN_EXPIRY_DATE);
        }

        public SetGuardianTxn() {
            super();
            this.guardianAddress = null;
            this.expiryDate = 0;
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Set Guardian";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return "PWR Chain";
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("guardianAddress", guardianAddress);
            data.put("expiryDate", expiryDate);
            return data;
        }
    }

    @Getter
/**
 * RemoveGuardianTxn class.
 */
    public static class RemoveGuardianTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1022;

        public RemoveGuardianTxn(JSONObject json) {
            super(json);
            // No additional fields to initialize
        }

        public RemoveGuardianTxn() {
            super();
            // No additional fields to initialize
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Remove Guardian";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return "PWR Chain";
        }
    }

    @Getter
/**
 * GuardianApprovalTxn class.
 */
    public static class GuardianApprovalTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1021;

        private final List<String> transactions = new ArrayList<>();

        public GuardianApprovalTxn(JSONObject json) {
            super(json);

            // Parse the transactions array
            JSONArray txArray = json.getJSONArray(BinaryJSONKeyMapper.TRANSACTIONS);
            for (int i = 0; i < txArray.length(); i++) {
                String txnHash = txArray.getString(i);
                transactions.add(txnHash);
            }
        }

        public GuardianApprovalTxn() {
            super();
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Guardian Approval";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return "PWR Chain";
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONArray txnArray = new JSONArray();
            for (String txnHash : transactions) {
                txnArray.put(txnHash);
            }

            JSONObject data = super.toJson();
            data.put(BinaryJSONKeyMapper.TRANSACTIONS, txnArray);
            return data;
        }
    }

    @Getter
/**
 * PayableVidaDataTxn class.
 */
    public static class PayableVidaDataTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1030;

        private final long vidaId;
        private final byte[] data;
        private final long value;

        public PayableVidaDataTxn(JSONObject json) {
            super(json);
            this.vidaId = json.getLong(BinaryJSONKeyMapper.VIDA_ID);
            this.value = json.getLong(BinaryJSONKeyMapper.AMOUNT);

            String dataStr = json.getString(BinaryJSONKeyMapper.DATA);
            if (dataStr == null || dataStr.isEmpty()) {
                this.data = new byte[0];
            } else {
                if(dataStr.startsWith("0x")) dataStr = dataStr.substring(2);
                this.data = Hex.decode(dataStr);
            }
        }

        public PayableVidaDataTxn() {
            super();
            this.vidaId = 0;
            this.data = new byte[0];
            this.value = 0;
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Payable VIDA Data";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return vidaId + "";
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONObject dataJson = super.toJson();
            dataJson.put("vidaId", vidaId);
            dataJson.put("data", Hex.toHexString(data));
            dataJson.put("value", value);
            return dataJson;
        }
    }

    @Getter
/**
 * ConduitApprovalTxn class.
 */
    public static class ConduitApprovalTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1029;

        private final long vidaId;
        private final List<String> transactions = new ArrayList<>();

        public ConduitApprovalTxn(JSONObject json) {
            super(json);

            this.vidaId = json.getLong(BinaryJSONKeyMapper.VIDA_ID);

            // Parse the transactions array
            JSONArray txArray = json.getJSONArray(BinaryJSONKeyMapper.TRANSACTIONS);
            for (int i = 0; i < txArray.length(); i++) {
                String txnHash = txArray.getString(i);
                transactions.add(txnHash);
            }
        }

        public ConduitApprovalTxn() {
            super();
            this.vidaId = 0;
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Conduit Approval";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return vidaId + "";
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONArray txnArray = new JSONArray();
            for (String txnHash : transactions) {
                txnArray.put(txnHash);
            }

            JSONObject data = super.toJson();
            data.put(BinaryJSONKeyMapper.VIDA_ID, vidaId);
            data.put(BinaryJSONKeyMapper.TRANSACTIONS, txnArray);
            return data;
        }
    }

    @Getter
/**
 * RemoveConduitsTxn class.
 */
    public static class RemoveConduitsTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1031;

        private final long vidaId;
        private final List<String> conduits;

        public RemoveConduitsTxn(JSONObject json) {
            super(json);
            this.vidaId = json.getLong(BinaryJSONKeyMapper.VIDA_ID);

            // Parse the conduits array
            JSONArray conduitArray = json.getJSONArray(BinaryJSONKeyMapper.CONDUITS);
            this.conduits = new ArrayList<>();
            for (int i = 0; i < conduitArray.length(); i++) {
                this.conduits.add(conduitArray.getString(i));
            }
        }

        public RemoveConduitsTxn() {
            super();
            this.vidaId = 0;
            this.conduits = new ArrayList<>();
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Remove Conduits";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return vidaId + "";
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("vidaId", vidaId);
            data.put("conduits", conduits);
            return data;
        }
    }

    @Getter
/**
 * MoveStakeTxn class.
 */
    public static class MoveStakeTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1024;

        private final String fromValidator;
        private final String toValidator;
        private final BigInteger sharesAmount;
        private final long pwrAmount;
        private final long remainingDelegatedPWR;

        public MoveStakeTxn(JSONObject json) {
            super(json);
            this.fromValidator = json.getString(BinaryJSONKeyMapper.FROM_VALIDATOR_ADDRESS);
            this.toValidator = json.getString(BinaryJSONKeyMapper.TO_VALIDATOR_ADDRESS);
            this.sharesAmount = new BigInteger(json.getString(BinaryJSONKeyMapper.SHARES_AMOUNT));
            this.pwrAmount = json.getLong(BinaryJSONKeyMapper.AMOUNT);
            this.remainingDelegatedPWR = json.getLong(BinaryJSONKeyMapper.REMAINING_DELEGATED_PWR);
        }

        public MoveStakeTxn() {
            super();
            this.fromValidator = null;
            this.toValidator = null;
            this.sharesAmount = BigInteger.ZERO;
            this.pwrAmount = 0;
            this.remainingDelegatedPWR = 0;
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Move Stake";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return fromValidator;
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("fromValidator", fromValidator);
            data.put("toValidator", toValidator);
            data.put("sharesAmount", sharesAmount.toString());
            return data;
        }
    }

    @Getter
/**
 * SetConduitModeTxn class.
 */
    public static class SetConduitModeTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1033;

        private final long vidaId;
        private final byte mode;
        private final int conduitThreshold;
        private final Set<String> conduits = new HashSet<>();
        private final Map<String, Long> vidaConduits = new HashMap<>();

        public SetConduitModeTxn(JSONObject json) {
            super(json);

            this.vidaId = json.getLong(BinaryJSONKeyMapper.VIDA_ID);
            this.mode = (byte) json.getInt(BinaryJSONKeyMapper.MODE);
            this.conduitThreshold = json.getInt(BinaryJSONKeyMapper.CONDUIT_THRESHOLD);

            // Parse the conduits set
            JSONArray conduitArray = json.optJSONArray(BinaryJSONKeyMapper.CONDUITS);
            if(conduitArray != null && !conduitArray.isEmpty()) {
                for (int i = 0; i < conduitArray.length(); i++) {
                    this.conduits.add(conduitArray.getString(i));
                }
            }

            // Parse the vidaConduits map
            JSONObject vidaConduitObj = json.optJSONObject(BinaryJSONKeyMapper.VIDA_CONDUIT_POWERS);
            if (vidaConduitObj != null && !vidaConduitObj.isEmpty()) {
                for (String key : vidaConduitObj.keySet()) {
                    this.vidaConduits.put(key, vidaConduitObj.getLong(key));
                }
            }
        }

        public SetConduitModeTxn() {
            super();
            this.vidaId = 0;
            this.mode = 0;
            this.conduitThreshold = 0;
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Set Conduit Mode";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return vidaId + "";
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("vidaId", vidaId);
            data.put("mode", mode);
            data.put("conduitThreshold", conduitThreshold);
            data.put("conduits", conduits);
            if (vidaConduits != null && !vidaConduits.isEmpty()) {
                data.put("vidaConduits", new JSONObject(vidaConduits));
            }
            return data;
        }
    }

    // VIDA-related transactions

    @Getter
/**
 * AddVidaAllowedSendersTxn class.
 */
    public static class AddVidaAllowedSendersTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1037;

        private final long vidaId;
        private final Set<String> allowedSenders;

        public AddVidaAllowedSendersTxn(JSONObject json) {
            super(json);
            this.vidaId = json.getLong(BinaryJSONKeyMapper.VIDA_ID);

            // Parse the allowedSenders set
            JSONArray addressesArray = json.getJSONArray(BinaryJSONKeyMapper.ADDRESSES);
            this.allowedSenders = new HashSet<>();
            for (int i = 0; i < addressesArray.length(); i++) {
                this.allowedSenders.add(addressesArray.getString(i));
            }
        }

        public AddVidaAllowedSendersTxn() {
            super();
            this.vidaId = 0;
            this.allowedSenders = new HashSet<>();
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Add VIDA Allowed Senders";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return vidaId + "";
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("vidaId", vidaId);
            data.put("allowedSenders", allowedSenders);
            return data;
        }
    }

    @Getter
/**
 * AddVidaSponsoredAddressesTxn class.
 */
    public static class AddVidaSponsoredAddressesTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1036;

        private final long vidaId;
        private final Set<String> sponsoredAddresses;

        public AddVidaSponsoredAddressesTxn(JSONObject json) {
            super(json);
            this.vidaId = json.getLong(BinaryJSONKeyMapper.VIDA_ID);

            // Parse the sponsoredAddresses set
            JSONArray addressesArray = json.getJSONArray(BinaryJSONKeyMapper.ADDRESSES);
            this.sponsoredAddresses = new HashSet<>();
            for (int i = 0; i < addressesArray.length(); i++) {
                this.sponsoredAddresses.add(addressesArray.getString(i));
            }
        }

        public AddVidaSponsoredAddressesTxn() {
            super();
            this.vidaId = 0;
            this.sponsoredAddresses = new HashSet<>();
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Add VIDA Sponsored Addresses";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return vidaId + "";
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("vidaId", vidaId);
            data.put("sponsoredAddresses", sponsoredAddresses);
            return data;
        }
    }

    @Getter
/**
 * RemoveSponsoredAddressesTxn class.
 */
    public static class RemoveSponsoredAddressesTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1039;

        private final long vidaId;
        private final Set<String> sponsoredAddresses;

        public RemoveSponsoredAddressesTxn(JSONObject json) {
            super(json);
            this.vidaId = json.getLong(BinaryJSONKeyMapper.VIDA_ID);

            // Parse the sponsoredAddresses set
            JSONArray addressesArray = json.getJSONArray(BinaryJSONKeyMapper.ADDRESSES);
            this.sponsoredAddresses = new HashSet<>();
            for (int i = 0; i < addressesArray.length(); i++) {
                this.sponsoredAddresses.add(addressesArray.getString(i));
            }
        }

        public RemoveSponsoredAddressesTxn() {
            super();
            this.vidaId = 0;
            this.sponsoredAddresses = new HashSet<>();
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Remove Sponsored Addresses";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return vidaId + "";
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("vidaId", vidaId);
            data.put("sponsoredAddresses", sponsoredAddresses);
            return data;
        }
    }

    @Getter
/**
 * RemoveVidaAllowedSendersTxn class.
 */
    public static class RemoveVidaAllowedSendersTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1038;

        private final long vidaId;
        private final Set<String> allowedSenders;

        public RemoveVidaAllowedSendersTxn(JSONObject json) {
            super(json);
            this.vidaId = json.getLong(BinaryJSONKeyMapper.VIDA_ID);

            // Parse the allowedSenders set
            JSONArray addressesArray = json.getJSONArray(BinaryJSONKeyMapper.ADDRESSES);
            this.allowedSenders = new HashSet<>();
            for (int i = 0; i < addressesArray.length(); i++) {
                this.allowedSenders.add(addressesArray.getString(i));
            }
        }

        public RemoveVidaAllowedSendersTxn() {
            super();
            this.vidaId = 0;
            this.allowedSenders = new HashSet<>();
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Remove VIDA Allowed Senders";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return vidaId + "";
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("vidaId", vidaId);
            data.put("allowedSenders", allowedSenders);
            return data;
        }
    }

    @Getter
/**
 * SetVidaPrivateStateTxn class.
 */
    public static class SetVidaPrivateStateTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1034;

        private final long vidaId;
        private final boolean privateState;

        public SetVidaPrivateStateTxn(JSONObject json) {
            super(json);
            this.vidaId = json.getLong(BinaryJSONKeyMapper.VIDA_ID);
            this.privateState = json.getBoolean(BinaryJSONKeyMapper.IS_PRIVATE);
        }

        public SetVidaPrivateStateTxn() {
            super();
            this.vidaId = 0;
            this.privateState = false;
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Set VIDA Private State";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return vidaId + "";
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("vidaId", vidaId);
            data.put("privateState", privateState);
            return data;
        }
    }

    @Getter
/**
 * SetVidaToAbsolutePublic class.
 */
    public static class SetVidaToAbsolutePublic extends FalconTransaction {
        public static final int IDENTIFIER = 1035;

        private final long vidaId;

        public SetVidaToAbsolutePublic(JSONObject json) {
            super(json);
            this.vidaId = json.getLong(BinaryJSONKeyMapper.VIDA_ID);
        }

        public SetVidaToAbsolutePublic() {
            super();
            this.vidaId = 0;
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Set VIDA To Absolute Public";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return vidaId + "";
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("vidaId", vidaId);
            return data;
        }
    }

    @Getter
/**
 * SetPWRTransferRightsTxn class.
 */
    public static class SetPWRTransferRightsTxn extends FalconTransfer {
        public static final int IDENTIFIER = 1040;

        private final long vidaId;
        private final boolean ownerCanTransferPWR;

        public SetPWRTransferRightsTxn(JSONObject json) {
            super(json);
            this.vidaId = json.getLong(BinaryJSONKeyMapper.VIDA_ID);
            this.ownerCanTransferPWR = json.getBoolean(BinaryJSONKeyMapper.OWNER_CAN_TRANSFER_PWR);
        }

        public SetPWRTransferRightsTxn() {
            super();
            this.vidaId = 0;
            this.ownerCanTransferPWR = false;
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Set PWR Transfer Rights";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return vidaId + "";
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("vidaId", vidaId);
            data.put("ownerCanTransferPWR", ownerCanTransferPWR);
            return data;
        }
    }

/**
 * TransferPWRFromVidaTxn class.
 */
    public static class TransferPWRFromVidaTxn extends FalconTransfer {
        public static final int IDENTIFIER = 1041;

        private final long vidaId;
        private final String receiver;

        public TransferPWRFromVidaTxn(JSONObject json) {
            super(json);
            this.vidaId = json.getLong(BinaryJSONKeyMapper.VIDA_ID);
            this.receiver = json.getString(BinaryJSONKeyMapper.RECEIVER);
        }

        public TransferPWRFromVidaTxn() {
            super();
            this.vidaId = 0;
            this.receiver = null;
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Transfer PWR From VIDA";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return receiver;
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("vidaId", vidaId);
            data.put("receiver", receiver);
            return data;
        }
    }

    // Governance Proposal Transactions

    @Getter
/**
 * ChangeEarlyWithdrawPenaltyProposalTxn class.
 */
    public static class ChangeEarlyWithdrawPenaltyProposalTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1009;

        private final String title;
        private final String description;
        private final long earlyWithdrawalTime;
        private final int withdrawalPenalty;

        public ChangeEarlyWithdrawPenaltyProposalTxn(JSONObject json) {
            super(json);
            this.title = json.getString(BinaryJSONKeyMapper.TITLE);
            this.description = json.getString(BinaryJSONKeyMapper.DESCRIPTION);
            this.earlyWithdrawalTime = json.getLong(BinaryJSONKeyMapper.EARLY_WITHDRAW_TIME);
            this.withdrawalPenalty = json.getInt(BinaryJSONKeyMapper.EARLY_WITHDRAW_PENALTY);
        }

        public ChangeEarlyWithdrawPenaltyProposalTxn() {
            super();
            this.title = null;
            this.description = null;
            this.earlyWithdrawalTime = 0;
            this.withdrawalPenalty = 0;
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Change Early Withdraw Penalty Proposal";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return "PWR Chain";
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("title", title);
            data.put("description", description);
            data.put("earlyWithdrawalTime", earlyWithdrawalTime);
            data.put("withdrawalPenalty", withdrawalPenalty);
            return data;
        }
    }

    @Getter
/**
 * ChangeFeePerByteProposalTxn class.
 */
    public static class ChangeFeePerByteProposalTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1010;

        private final String title;
        private final String description;
        private final long newFeePerByte;

        public ChangeFeePerByteProposalTxn(JSONObject json) {
            super(json);
            this.title = json.getString(BinaryJSONKeyMapper.TITLE);
            this.description = json.getString(BinaryJSONKeyMapper.DESCRIPTION);
            this.newFeePerByte = json.getLong(BinaryJSONKeyMapper.FEE_PER_BYTE);
        }

        public ChangeFeePerByteProposalTxn() {
            super();
            this.title = null;
            this.description = null;
            this.newFeePerByte = 0;
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Change Fee Per Byte Proposal";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return "PWR Chain";
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("title", title);
            data.put("description", description);
            data.put("newFeePerByte", newFeePerByte);
            return data;
        }
    }

    @Getter
/**
 * ChangeMaxBlockSizeProposalTxn class.
 */
    public static class ChangeMaxBlockSizeProposalTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1011;

        private final String title;
        private final String description;
        private final int maxBlockSize;

        public ChangeMaxBlockSizeProposalTxn(JSONObject json) {
            super(json);
            this.title = json.getString(BinaryJSONKeyMapper.TITLE);
            this.description = json.getString(BinaryJSONKeyMapper.DESCRIPTION);
            this.maxBlockSize = json.getInt(BinaryJSONKeyMapper.MAX_BLOCK_SIZE);
        }

        public ChangeMaxBlockSizeProposalTxn() {
            super();
            this.title = null;
            this.description = null;
            this.maxBlockSize = 0;
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Change Max Block Size Proposal";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return "PWR Chain";
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("title", title);
            data.put("description", description);
            data.put("maxBlockSize", maxBlockSize);
            return data;
        }
    }

    @Getter
/**
 * ChangeMaxTxnSizeProposalTxn class.
 */
    public static class ChangeMaxTxnSizeProposalTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1012;

        private final String title;
        private final String description;
        private final int maxTxnSize;

        public ChangeMaxTxnSizeProposalTxn(JSONObject json) {
            super(json);
            this.title = json.getString(BinaryJSONKeyMapper.TITLE);
            this.description = json.getString(BinaryJSONKeyMapper.DESCRIPTION);
            this.maxTxnSize = json.getInt(BinaryJSONKeyMapper.MAX_TRANSACTION_SIZE);
        }

        public ChangeMaxTxnSizeProposalTxn() {
            super();
            this.title = null;
            this.description = null;
            this.maxTxnSize = 0;
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Change Max Transaction Size Proposal";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return "PWR Chain";
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("title", title);
            data.put("description", description);
            data.put("maxTxnSize", maxTxnSize);
            return data;
        }
    }

    @Getter
/**
 * ChangeOverallBurnPercentageProposalTxn class.
 */
    public static class ChangeOverallBurnPercentageProposalTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1013;

        private final String title;
        private final String description;
        private final int burnPercentage;

        public ChangeOverallBurnPercentageProposalTxn(JSONObject json) {
            super(json);
            this.title = json.getString(BinaryJSONKeyMapper.TITLE);
            this.description = json.getString(BinaryJSONKeyMapper.DESCRIPTION);
            this.burnPercentage = json.getInt(BinaryJSONKeyMapper.OVERALL_BURN_PERCENTAGE);
        }

        public ChangeOverallBurnPercentageProposalTxn() {
            super();
            this.title = null;
            this.description = null;
            this.burnPercentage = 0;
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Change Overall Burn Percentage Proposal";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return "PWR Chain";
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("title", title);
            data.put("description", description);
            data.put("burnPercentage", burnPercentage);
            return data;
        }
    }

    @Getter
/**
 * ChangeRewardPerYearProposalTxn class.
 */
    public static class ChangeRewardPerYearProposalTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1014;

        private final String title;
        private final String description;
        private final long rewardPerYear;

        public ChangeRewardPerYearProposalTxn(JSONObject json) {
            super(json);
            this.title = json.getString(BinaryJSONKeyMapper.TITLE);
            this.description = json.getString(BinaryJSONKeyMapper.DESCRIPTION);
            this.rewardPerYear = json.getLong(BinaryJSONKeyMapper.REWARD_PER_YEAR);
        }

        public ChangeRewardPerYearProposalTxn() {
            super();
            this.title = null;
            this.description = null;
            this.rewardPerYear = 0;
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Change Reward Per Year Proposal";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return "PWR Chain";
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("title", title);
            data.put("description", description);
            data.put("rewardPerYear", rewardPerYear);
            return data;
        }
    }

    @Getter
/**
 * ChangeValidatorCountLimitProposalTxn class.
 */
    public static class ChangeValidatorCountLimitProposalTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1015;

        private final String title;
        private final String description;
        private final int validatorCountLimit;

        public ChangeValidatorCountLimitProposalTxn(JSONObject json) {
            super(json);
            this.title = json.getString(BinaryJSONKeyMapper.TITLE);
            this.description = json.getString(BinaryJSONKeyMapper.DESCRIPTION);
            this.validatorCountLimit = json.getInt(BinaryJSONKeyMapper.VALIDATOR_COUNT_LIMIT);
        }

        public ChangeValidatorCountLimitProposalTxn() {
            super();
            this.title = null;
            this.description = null;
            this.validatorCountLimit = 0;
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Change Validator Count Limit Proposal";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return "PWR Chain";
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("title", title);
            data.put("description", description);
            data.put("validatorCountLimit", validatorCountLimit);
            return data;
        }
    }

    @Getter
/**
 * ChangeValidatorJoiningFeeProposalTxn class.
 */
    public static class ChangeValidatorJoiningFeeProposalTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1016;

        private final String title;
        private final String description;
        private final long joiningFee;

        public ChangeValidatorJoiningFeeProposalTxn(JSONObject json) {
            super(json);
            this.title = json.getString(BinaryJSONKeyMapper.TITLE);
            this.description = json.getString(BinaryJSONKeyMapper.DESCRIPTION);
            this.joiningFee = json.getLong(BinaryJSONKeyMapper.VALIDATOR_JOINING_FEE);
        }

        public ChangeValidatorJoiningFeeProposalTxn() {
            super();
            this.title = null;
            this.description = null;
            this.joiningFee = 0;
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Change Validator Joining Fee Proposal";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return "PWR Chain";
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("title", title);
            data.put("description", description);
            data.put("joiningFee", joiningFee);
            return data;
        }
    }

    @Getter
/**
 * ChangeVidaIdClaimingFeeProposalTxn class.
 */
    public static class ChangeVidaIdClaimingFeeProposalTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1017;

        private final String title;
        private final String description;
        private final long vidaIdClaimingFee;

        public ChangeVidaIdClaimingFeeProposalTxn(JSONObject json) {
            super(json);
            this.title = json.getString(BinaryJSONKeyMapper.TITLE);
            this.description = json.getString(BinaryJSONKeyMapper.DESCRIPTION);
            this.vidaIdClaimingFee = json.getLong(BinaryJSONKeyMapper.VIDA_ID_CLAIMING_FEE);
        }

        public ChangeVidaIdClaimingFeeProposalTxn() {
            super();
            this.title = null;
            this.description = null;
            this.vidaIdClaimingFee = 0;
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Change VIDA ID Claiming Fee Proposal";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return "PWR Chain";
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("title", title);
            data.put("description", description);
            data.put("vidaIdClaimingFee", vidaIdClaimingFee);
            return data;
        }
    }

    @Getter
/**
 * ChangeVmOwnerTxnFeeShareProposalTxn class.
 */
    public static class ChangeVmOwnerTxnFeeShareProposalTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1018;

        private final String title;
        private final String description;
        private final int vmOwnerTxnFeeShare;

        public ChangeVmOwnerTxnFeeShareProposalTxn(JSONObject json) {
            super(json);
            this.title = json.getString(BinaryJSONKeyMapper.TITLE);
            this.description = json.getString(BinaryJSONKeyMapper.DESCRIPTION);
            this.vmOwnerTxnFeeShare = json.getInt(BinaryJSONKeyMapper.VM_OWNER_TRANSACTION_FEE_SHARE);
        }

        public ChangeVmOwnerTxnFeeShareProposalTxn() {
            super();
            this.title = null;
            this.description = null;
            this.vmOwnerTxnFeeShare = 0;
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Change VM Owner Transaction Fee Share Proposal";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return "PWR Chain";
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("title", title);
            data.put("description", description);
            data.put("vmOwnerTxnFeeShare", vmOwnerTxnFeeShare);
            return data;
        }
    }

    @Getter
/**
 * OtherProposalTxn class.
 */
    public static class OtherProposalTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1019;

        private final String title;
        private final String description;

        public OtherProposalTxn(JSONObject json) {
            super(json);
            this.title = json.getString(BinaryJSONKeyMapper.TITLE);
            this.description = json.getString(BinaryJSONKeyMapper.DESCRIPTION);
        }

        public OtherProposalTxn() {
            super();
            this.title = null;
            this.description = null;
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Other Proposal";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return "PWR Chain";
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("title", title);
            data.put("description", description);
            return data;
        }
    }

    @Getter
/**
 * VoteOnProposalTxn class.
 */
    public static class VoteOnProposalTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1020;

        private final String proposalHash;
        private final byte vote;

        public VoteOnProposalTxn(JSONObject json) {
            super(json);
            this.proposalHash = json.getString(BinaryJSONKeyMapper.PROPOSAL_HASH);
            this.vote = (byte) json.getInt(BinaryJSONKeyMapper.VOTE);
        }

        public VoteOnProposalTxn() {
            super();
            this.proposalHash = null;
            this.vote = 0;
        }

        @Override
/**
 * getIdentifier method.
 * @return value
 */
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
/**
 * getType method.
 * @return value
 */
        public String getType() {
            return "Vote On Proposal";
        }

        @Override
/**
 * getReceiver method.
 * @return value
 */
        public String getReceiver() {
            return "PWR Chain";
        }

        @Override
/**
 * toJson method.
 * @return value
 */
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("proposalHash", proposalHash);
            data.put("vote", vote);
            return data;
        }
    }
}