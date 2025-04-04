package com.github.pwrlabs.pwrj.record.transaction;

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
public abstract class FalconTransaction {
    private final String transactionHash;
    private final String sender;
    private final int nonce;
    private final int size;
    private final long feePerByte;
    private final long paidActionFee;
    private final long paidTotalFee;

    private final boolean success;
    private final String errorMessage;

    public FalconTransaction(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage) {
        this.transactionHash = transactionHash;
        this.sender = sender;
        this.nonce = nonce;
        this.size = size;
        this.feePerByte = feePerByte;
        this.paidActionFee = paidActionFee;
        this.paidTotalFee = paidTotalFee;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public FalconTransaction(JSONObject json) {
        this.transactionHash = json.getString(BinaryJSONKeyMapper.TRANSACTION_HASH);
        this.sender = json.getString(BinaryJSONKeyMapper.SENDER);
        this.nonce = json.getInt(BinaryJSONKeyMapper.NONCE);
        this.size = json.getInt(BinaryJSONKeyMapper.SIZE);
        this.feePerByte = json.getLong(BinaryJSONKeyMapper.FEE_PER_BYTE);
        this.paidActionFee = json.optLong(BinaryJSONKeyMapper.PAID_ACTION_FEE, 0);
        this.paidTotalFee = json.getLong(BinaryJSONKeyMapper.PAID_TOTAL_FEE);
        this.success = json.optBoolean(BinaryJSONKeyMapper.SUCCESS, false);
        this.errorMessage = json.optString(BinaryJSONKeyMapper.ERROR_MESSAGE, null);
    }

    public abstract int getIdentifier();

    public abstract String getType();

    //This function must be overriden by child classes to add more data to it
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

    public static FalconTransaction fromJson(JSONObject json) {
        Reflections reflections = new Reflections("com.github.pwrlabs.pwrj.record.transaction");
        Set<Class<? extends FalconTransaction>> subclasses = reflections.getSubTypesOf(FalconTransaction.class);
        System.out.println("Sub classes size: " + subclasses.size());

        long identifier = json.getLong(BinaryJSONKeyMapper.IDENTIFIER);

        for (Class<? extends FalconTransaction> subclass : subclasses) {
            try {
                FalconTransaction instance = subclass.getDeclaredConstructor(JSONObject.class).newInstance(json);

                if(instance.getIdentifier() == identifier) {
                    return instance;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        throw new IllegalArgumentException("Unknown transaction identifier: " + identifier);
    }

    @Getter
    public static class FalconTransfer extends FalconTransaction {
        public static final int IDENTIFIER = 1006;

        private final String receiver;
        private final long amount;

        public FalconTransfer(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage, String receiver, long amount) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
            this.receiver = receiver;
            this.amount = amount;
        }

        public FalconTransfer(JSONObject json) {
            super(json);

            receiver = json.getString(BinaryJSONKeyMapper.RECEIVER);
            amount = json.getLong(BinaryJSONKeyMapper.AMOUNT);
        }

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Falcon Transfer";
        }

        @Override
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("receiver", receiver);
            data.put("amount", amount);
            return data;
        }
    }

    @Getter
    public static class SetPublicKey extends FalconTransaction {
        public static final int IDENTIFIER = 1001;

        private final byte[] publicKey;

        public SetPublicKey(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage, byte[] publicKey) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
            this.publicKey = publicKey;
        }

        public SetPublicKey(JSONObject json) {
            super(json);
            this.publicKey = json.getString(BinaryJSONKeyMapper.PUBLIC_KEY).getBytes();
        }

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Set Public Key";
        }

        @Override
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("publicKey", publicKey);
            return data;
        }
    }

    @Getter
    public static class FalconJoinAsValidator extends FalconTransaction {
        public static final int IDENTIFIER = 1002;

        private final String ip;

        public FalconJoinAsValidator(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage, String ip) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
            this.ip = ip;
        }

        public FalconJoinAsValidator(JSONObject json) {
            super(json);
            this.ip = json.getString(BinaryJSONKeyMapper.IP);
        }

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Join As Validator";
        }

        @Override
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("ip", ip);
            return data;
        }
    }

    @Getter
    public static class FalconChangeIp extends FalconTransaction {
        public static final int IDENTIFIER = 1003;

        private final String newIp;

        public FalconChangeIp(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage, String newIp) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
            this.newIp = newIp;
        }

        public FalconChangeIp(JSONObject json) {
            super(json);
            this.newIp = json.getString(BinaryJSONKeyMapper.IP);
        }

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Change IP";
        }

        @Override
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("newIp", newIp);
            return data;
        }
    }

    @Getter
    public static class FalconDelegate extends FalconTransaction {
        public static final int IDENTIFIER = 1004;

        private final String validator;
        private final long pwrAmount;

        public FalconDelegate(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage, String validator, long pwrAmount) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
            this.validator = validator;
            this.pwrAmount = pwrAmount;
        }

        public FalconDelegate(JSONObject json) {
            super(json);
            this.validator = json.getString(BinaryJSONKeyMapper.VALIDATOR_ADDRESS);
            this.pwrAmount = json.getLong(BinaryJSONKeyMapper.AMOUNT);
        }

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Delegate";
        }

        @Override
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("validator", validator);
            data.put("pwrAmount", pwrAmount);
            return data;
        }
    }

    @Getter
    public static class FalconClaimActiveNodeSpot extends FalconTransaction {
        public static final int IDENTIFIER = 1005;

        public FalconClaimActiveNodeSpot(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
        }

        public FalconClaimActiveNodeSpot(JSONObject json) {
            super(json);
            // No additional fields to initialize
        }

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Claim Active Node Spot";
        }
    }

    @Getter
    public static class WithdrawTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1007;

        private final String validator;
        private final BigInteger sharesAmount;

        public WithdrawTxn(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage, String validator, BigInteger sharesAmount) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
            this.validator = validator;
            this.sharesAmount = sharesAmount;
        }

        public WithdrawTxn(JSONObject json) {
            super(json);
            this.validator = json.getString(BinaryJSONKeyMapper.VALIDATOR_ADDRESS);
            this.sharesAmount = new BigInteger(json.getString(BinaryJSONKeyMapper.SHARES_AMOUNT));
        }

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Withdraw";
        }

        @Override
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("validator", validator);
            data.put("sharesAmount", sharesAmount.toString());
            return data;
        }
    }

    @Getter
    public static class ClaimVidaId extends FalconTransaction {
        public static final int IDENTIFIER = 1008;

        private final long vidaId;

        public ClaimVidaId(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage, long vidaId) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
            this.vidaId = vidaId;
        }

        public ClaimVidaId(JSONObject json) {
            super(json);
            this.vidaId = json.getLong(BinaryJSONKeyMapper.VIDA_ID);
        }

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Claim VIDA ID";
        }

        @Override
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("vidaId", vidaId);
            return data;
        }
    }

    @Getter
    public static class RemoveValidatorTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1009;

        private final String validatorAddress;

        public RemoveValidatorTxn(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage, String validatorAddress) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
            this.validatorAddress = validatorAddress;
        }

        public RemoveValidatorTxn(JSONObject json) {
            super(json);
            this.validatorAddress = json.getString(BinaryJSONKeyMapper.VALIDATOR_ADDRESS);
        }

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Remove Validator";
        }

        @Override
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("validatorAddress", validatorAddress);
            return data;
        }
    }

    @Getter
    public static class SetGuardianTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1010;

        private final String guardianAddress;
        private final long expiryDate;

        public SetGuardianTxn(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage, String guardianAddress, long expiryDate) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
            this.guardianAddress = guardianAddress;
            this.expiryDate = expiryDate;
        }

        public SetGuardianTxn(JSONObject json) {
            super(json);
            this.guardianAddress = json.getString(BinaryJSONKeyMapper.GUARDIAN_ADDRESS);
            this.expiryDate = json.getLong(BinaryJSONKeyMapper.GUARDIAN_EXPIRY_DATE);
        }

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Set Guardian";
        }

        @Override
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("guardianAddress", guardianAddress);
            data.put("expiryDate", expiryDate);
            return data;
        }
    }

    @Getter
    public static class RemoveGuardianTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1011;

        public RemoveGuardianTxn(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
        }

        public RemoveGuardianTxn(JSONObject json) {
            super(json);
            // No additional fields to initialize
        }

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Remove Guardian";
        }
    }

    @Getter
    public static class GuardianApprovalTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1012;

        private final List<FalconTransaction> transactions;

        public GuardianApprovalTxn(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage, List<FalconTransaction> transactions) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
            this.transactions = transactions;
        }

        public GuardianApprovalTxn(JSONObject json) {
            super(json);
            this.transactions = new ArrayList<>();

            // Parse the transactions array
            JSONArray txArray = json.getJSONArray(BinaryJSONKeyMapper.TRANSACTIONS);
            for (int i = 0; i < txArray.length(); i++) {
                JSONObject txJson = txArray.getJSONObject(i);
                // Would need to implement a factory method to create the correct transaction type
                // based on the identifier or type in the JSON
                // For this example, leaving as a placeholder
                // this.transactions.add(FalconTransactionFactory.createFromJson(txJson));
            }
        }

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Guardian Approval";
        }

        @Override
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            // Converting list of transactions to JSON array
            JSONObject[] txnArray = transactions.stream()
                    .map(FalconTransaction::toJson)
                    .toArray(JSONObject[]::new);
            data.put("transactions", txnArray);
            return data;
        }
    }

    @Getter
    public static class PayableVidaDataTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1013;

        private final long vidaId;
        private final byte[] data;
        private final long value;

        public PayableVidaDataTxn(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage, long vidaId, byte[] data, long value) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
            this.vidaId = vidaId;
            this.data = data;
            this.value = value;
        }

        public PayableVidaDataTxn(JSONObject json) {
            super(json);
            this.vidaId = json.getLong(BinaryJSONKeyMapper.VIDA_ID);
            this.data = json.getString(BinaryJSONKeyMapper.DATA).getBytes();
            this.value = json.getLong(BinaryJSONKeyMapper.AMOUNT);
        }

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Payable VIDA Data";
        }

        @Override
        public JSONObject toJson() {
            JSONObject dataJson = super.toJson();
            dataJson.put("vidaId", vidaId);
            dataJson.put("data", data);
            dataJson.put("value", value);
            return dataJson;
        }
    }

    @Getter
    public static class ConduitApprovalTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1014;

        private final long vidaId;
        private final List<FalconTransaction> transactions;

        public ConduitApprovalTxn(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage, long vidaId, List<FalconTransaction> transactions) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
            this.vidaId = vidaId;
            this.transactions = transactions;
        }

        public ConduitApprovalTxn(JSONObject json) {
            super(json);
            this.vidaId = json.getLong(BinaryJSONKeyMapper.VIDA_ID);
            this.transactions = new ArrayList<>();

            // Parse the transactions array
            JSONArray txArray = json.getJSONArray(BinaryJSONKeyMapper.TRANSACTIONS);
            for (int i = 0; i < txArray.length(); i++) {
                JSONObject txJson = txArray.getJSONObject(i);
                // Would need to implement a factory method here as well
                // this.transactions.add(FalconTransactionFactory.createFromJson(txJson));
            }
        }

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Conduit Approval";
        }

        @Override
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("vidaId", vidaId);
            JSONObject[] txnArray = transactions.stream()
                    .map(FalconTransaction::toJson)
                    .toArray(JSONObject[]::new);
            data.put("transactions", txnArray);
            return data;
        }
    }

    @Getter
    public static class RemoveConduitsTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1015;

        private final long vidaId;
        private final List<String> conduits;

        public RemoveConduitsTxn(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage, long vidaId, List<String> conduits) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
            this.vidaId = vidaId;
            this.conduits = conduits;
        }

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

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Remove Conduits";
        }

        @Override
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("vidaId", vidaId);
            data.put("conduits", conduits);
            return data;
        }
    }

    @Getter
    public static class MoveStakeTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1016;

        private final String fromValidator;
        private final String toValidator;
        private final BigInteger sharesAmount;

        public MoveStakeTxn(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage, String fromValidator, String toValidator, BigInteger sharesAmount) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
            this.fromValidator = fromValidator;
            this.toValidator = toValidator;
            this.sharesAmount = sharesAmount;
        }

        public MoveStakeTxn(JSONObject json) {
            super(json);
            this.fromValidator = json.getString(BinaryJSONKeyMapper.FROM_VALIDATOR_ADDRESS);
            this.toValidator = json.getString(BinaryJSONKeyMapper.TO_VALIDATOR_ADDRESS);
            this.sharesAmount = new BigInteger(json.getString(BinaryJSONKeyMapper.SHARES_AMOUNT));
        }

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Move Stake";
        }

        @Override
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("fromValidator", fromValidator);
            data.put("toValidator", toValidator);
            data.put("sharesAmount", sharesAmount.toString());
            return data;
        }
    }

    @Getter
    public static class SetConduitModeTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1017;

        private final long vidaId;
        private final byte mode;
        private final int conduitThreshold;
        private final Set<String> conduits;
        private final Map<String, Long> vidaConduits;

        public SetConduitModeTxn(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage,
                                 long vidaId, byte mode, int conduitThreshold, Set<String> conduits, Map<String, Long> vidaConduits) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
            this.vidaId = vidaId;
            this.mode = mode;
            this.conduitThreshold = conduitThreshold;
            this.conduits = conduits;
            this.vidaConduits = vidaConduits;
        }

        public SetConduitModeTxn(JSONObject json) {
            super(json);
            this.vidaId = json.getLong(BinaryJSONKeyMapper.VIDA_ID);
            this.mode = (byte) json.getInt(BinaryJSONKeyMapper.MODE);
            this.conduitThreshold = json.getInt(BinaryJSONKeyMapper.CONDUIT_THRESHOLD);

            // Parse the conduits set
            JSONArray conduitArray = json.getJSONArray(BinaryJSONKeyMapper.CONDUITS);
            this.conduits = new HashSet<>();
            for (int i = 0; i < conduitArray.length(); i++) {
                this.conduits.add(conduitArray.getString(i));
            }

            // Parse the vidaConduits map
            JSONObject vidaConduitObj = json.optJSONObject(BinaryJSONKeyMapper.VIDA_CONDUIT_POWERS);
            this.vidaConduits = new HashMap<>();
            if (vidaConduitObj != null) {
                for (String key : vidaConduitObj.keySet()) {
                    this.vidaConduits.put(key, vidaConduitObj.getLong(key));
                }
            }
        }

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Set Conduit Mode";
        }

        @Override
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
    public static class AddVidaAllowedSendersTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1018;

        private final long vidaId;
        private final Set<String> allowedSenders;

        public AddVidaAllowedSendersTxn(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage, long vidaId, Set<String> allowedSenders) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
            this.vidaId = vidaId;
            this.allowedSenders = allowedSenders;
        }

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

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Add VIDA Allowed Senders";
        }

        @Override
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("vidaId", vidaId);
            data.put("allowedSenders", allowedSenders);
            return data;
        }
    }

    @Getter
    public static class AddVidaSponsoredAddressesTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1019;

        private final long vidaId;
        private final Set<String> sponsoredAddresses;

        public AddVidaSponsoredAddressesTxn(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage, long vidaId, Set<String> sponsoredAddresses) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
            this.vidaId = vidaId;
            this.sponsoredAddresses = sponsoredAddresses;
        }

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

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Add VIDA Sponsored Addresses";
        }

        @Override
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("vidaId", vidaId);
            data.put("sponsoredAddresses", sponsoredAddresses);
            return data;
        }
    }

    @Getter
    public static class RemoveSponsoredAddressesTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1020;

        private final long vidaId;
        private final Set<String> sponsoredAddresses;

        public RemoveSponsoredAddressesTxn(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage, long vidaId, Set<String> sponsoredAddresses) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
            this.vidaId = vidaId;
            this.sponsoredAddresses = sponsoredAddresses;
        }

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

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Remove Sponsored Addresses";
        }

        @Override
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("vidaId", vidaId);
            data.put("sponsoredAddresses", sponsoredAddresses);
            return data;
        }
    }

    @Getter
    public static class RemoveVidaAllowedSendersTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1021;

        private final long vidaId;
        private final Set<String> allowedSenders;

        public RemoveVidaAllowedSendersTxn(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage, long vidaId, Set<String> allowedSenders) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
            this.vidaId = vidaId;
            this.allowedSenders = allowedSenders;
        }

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

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Remove VIDA Allowed Senders";
        }

        @Override
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("vidaId", vidaId);
            data.put("allowedSenders", allowedSenders);
            return data;
        }
    }

    @Getter
    public static class SetVidaPrivateStateTxn extends FalconTransaction {
        public static final int IDENTIFIER = 1022;

        private final long vidaId;
        private final boolean privateState;

        public SetVidaPrivateStateTxn(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage, long vidaId, boolean privateState) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
            this.vidaId = vidaId;
            this.privateState = privateState;
        }

        public SetVidaPrivateStateTxn(JSONObject json) {
            super(json);
            this.vidaId = json.getLong(BinaryJSONKeyMapper.VIDA_ID);
            this.privateState = json.getBoolean(BinaryJSONKeyMapper.IS_PRIVATE);
        }

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Set VIDA Private State";
        }

        @Override
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("vidaId", vidaId);
            data.put("privateState", privateState);
            return data;
        }
    }

    @Getter
    public static class SetVidaToAbsolutePublic extends FalconTransaction {
        public static final int IDENTIFIER = 1023;

        private final long vidaId;

        public SetVidaToAbsolutePublic(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage, long vidaId) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
            this.vidaId = vidaId;
        }

        public SetVidaToAbsolutePublic(JSONObject json) {
            super(json);
            this.vidaId = json.getLong(BinaryJSONKeyMapper.VIDA_ID);
        }

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Set VIDA To Absolute Public";
        }

        @Override
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("vidaId", vidaId);
            return data;
        }
    }

    // Governance Proposal Transactions

    @Getter
    public static class ChangeEarlyWithdrawPenaltyProposalTxn extends FalconTransaction {
        public static final int IDENTIFIER = 2001;

        private final String title;
        private final String description;
        private final long earlyWithdrawalTime;
        private final int withdrawalPenalty;

        public ChangeEarlyWithdrawPenaltyProposalTxn(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage, String title, String description, long earlyWithdrawalTime, int withdrawalPenalty) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
            this.title = title;
            this.description = description;
            this.earlyWithdrawalTime = earlyWithdrawalTime;
            this.withdrawalPenalty = withdrawalPenalty;
        }

        public ChangeEarlyWithdrawPenaltyProposalTxn(JSONObject json) {
            super(json);
            this.title = json.getString(BinaryJSONKeyMapper.TITLE);
            this.description = json.getString(BinaryJSONKeyMapper.DESCRIPTION);
            this.earlyWithdrawalTime = json.getLong(BinaryJSONKeyMapper.EARLY_WITHDRAW_TIME);
            this.withdrawalPenalty = json.getInt(BinaryJSONKeyMapper.EARLY_WITHDRAW_PENALTY);
        }

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Change Early Withdraw Penalty Proposal";
        }

        @Override
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
    public static class ChangeFeePerByteProposalTxn extends FalconTransaction {
        public static final int IDENTIFIER = 2002;

        private final String title;
        private final String description;
        private final long newFeePerByte;

        public ChangeFeePerByteProposalTxn(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage, String title, String description, long newFeePerByte) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
            this.title = title;
            this.description = description;
            this.newFeePerByte = newFeePerByte;
        }

        public ChangeFeePerByteProposalTxn(JSONObject json) {
            super(json);
            this.title = json.getString(BinaryJSONKeyMapper.TITLE);
            this.description = json.getString(BinaryJSONKeyMapper.DESCRIPTION);
            this.newFeePerByte = json.getLong(BinaryJSONKeyMapper.FEE_PER_BYTE);
        }

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Change Fee Per Byte Proposal";
        }

        @Override
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("title", title);
            data.put("description", description);
            data.put("newFeePerByte", newFeePerByte);
            return data;
        }
    }

    @Getter
    public static class ChangeMaxBlockSizeProposalTxn extends FalconTransaction {
        public static final int IDENTIFIER = 2003;

        private final String title;
        private final String description;
        private final int maxBlockSize;

        public ChangeMaxBlockSizeProposalTxn(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage, String title, String description, int maxBlockSize) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
            this.title = title;
            this.description = description;
            this.maxBlockSize = maxBlockSize;
        }

        public ChangeMaxBlockSizeProposalTxn(JSONObject json) {
            super(json);
            this.title = json.getString(BinaryJSONKeyMapper.TITLE);
            this.description = json.getString(BinaryJSONKeyMapper.DESCRIPTION);
            this.maxBlockSize = json.getInt(BinaryJSONKeyMapper.MAX_BLOCK_SIZE);
        }

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Change Max Block Size Proposal";
        }

        @Override
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("title", title);
            data.put("description", description);
            data.put("maxBlockSize", maxBlockSize);
            return data;
        }
    }

    @Getter
    public static class ChangeMaxTxnSizeProposalTxn extends FalconTransaction {
        public static final int IDENTIFIER = 2004;

        private final String title;
        private final String description;
        private final int maxTxnSize;

        public ChangeMaxTxnSizeProposalTxn(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage, String title, String description, int maxTxnSize) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
            this.title = title;
            this.description = description;
            this.maxTxnSize = maxTxnSize;
        }

        public ChangeMaxTxnSizeProposalTxn(JSONObject json) {
            super(json);
            this.title = json.getString(BinaryJSONKeyMapper.TITLE);
            this.description = json.getString(BinaryJSONKeyMapper.DESCRIPTION);
            this.maxTxnSize = json.getInt(BinaryJSONKeyMapper.MAX_TRANSACTION_SIZE);
        }

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Change Max Transaction Size Proposal";
        }

        @Override
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("title", title);
            data.put("description", description);
            data.put("maxTxnSize", maxTxnSize);
            return data;
        }
    }

    @Getter
    public static class ChangeOverallBurnPercentageProposalTxn extends FalconTransaction {
        public static final int IDENTIFIER = 2005;

        private final String title;
        private final String description;
        private final int burnPercentage;

        public ChangeOverallBurnPercentageProposalTxn(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage, String title, String description, int burnPercentage) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
            this.title = title;
            this.description = description;
            this.burnPercentage = burnPercentage;
        }

        public ChangeOverallBurnPercentageProposalTxn(JSONObject json) {
            super(json);
            this.title = json.getString(BinaryJSONKeyMapper.TITLE);
            this.description = json.getString(BinaryJSONKeyMapper.DESCRIPTION);
            this.burnPercentage = json.getInt(BinaryJSONKeyMapper.OVERALL_BURN_PERCENTAGE);
        }

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Change Overall Burn Percentage Proposal";
        }

        @Override
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("title", title);
            data.put("description", description);
            data.put("burnPercentage", burnPercentage);
            return data;
        }
    }

    @Getter
    public static class ChangeRewardPerYearProposalTxn extends FalconTransaction {
        public static final int IDENTIFIER = 2006;

        private final String title;
        private final String description;
        private final long rewardPerYear;

        public ChangeRewardPerYearProposalTxn(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage, String title, String description, long rewardPerYear) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
            this.title = title;
            this.description = description;
            this.rewardPerYear = rewardPerYear;
        }

        public ChangeRewardPerYearProposalTxn(JSONObject json) {
            super(json);
            this.title = json.getString(BinaryJSONKeyMapper.TITLE);
            this.description = json.getString(BinaryJSONKeyMapper.DESCRIPTION);
            this.rewardPerYear = json.getLong(BinaryJSONKeyMapper.REWARD_PER_YEAR);
        }

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Change Reward Per Year Proposal";
        }

        @Override
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("title", title);
            data.put("description", description);
            data.put("rewardPerYear", rewardPerYear);
            return data;
        }
    }

    @Getter
    public static class ChangeValidatorCountLimitProposalTxn extends FalconTransaction {
        public static final int IDENTIFIER = 2007;

        private final String title;
        private final String description;
        private final int validatorCountLimit;

        public ChangeValidatorCountLimitProposalTxn(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage, String title, String description, int validatorCountLimit) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
            this.title = title;
            this.description = description;
            this.validatorCountLimit = validatorCountLimit;
        }

        public ChangeValidatorCountLimitProposalTxn(JSONObject json) {
            super(json);
            this.title = json.getString(BinaryJSONKeyMapper.TITLE);
            this.description = json.getString(BinaryJSONKeyMapper.DESCRIPTION);
            this.validatorCountLimit = json.getInt(BinaryJSONKeyMapper.VALIDATOR_COUNT_LIMIT);
        }

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Change Validator Count Limit Proposal";
        }

        @Override
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("title", title);
            data.put("description", description);
            data.put("validatorCountLimit", validatorCountLimit);
            return data;
        }
    }

    @Getter
    public static class ChangeValidatorJoiningFeeProposalTxn extends FalconTransaction {
        public static final int IDENTIFIER = 2008;

        private final String title;
        private final String description;
        private final long joiningFee;

        public ChangeValidatorJoiningFeeProposalTxn(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage, String title, String description, long joiningFee) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
            this.title = title;
            this.description = description;
            this.joiningFee = joiningFee;
        }

        public ChangeValidatorJoiningFeeProposalTxn(JSONObject json) {
            super(json);
            this.title = json.getString(BinaryJSONKeyMapper.TITLE);
            this.description = json.getString(BinaryJSONKeyMapper.DESCRIPTION);
            this.joiningFee = json.getLong(BinaryJSONKeyMapper.VALIDATOR_JOINING_FEE);
        }

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Change Validator Joining Fee Proposal";
        }

        @Override
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("title", title);
            data.put("description", description);
            data.put("joiningFee", joiningFee);
            return data;
        }
    }

    @Getter
    public static class ChangeVidaIdClaimingFeeProposalTxn extends FalconTransaction {
        public static final int IDENTIFIER = 2009;

        private final String title;
        private final String description;
        private final long vidaIdClaimingFee;

        public ChangeVidaIdClaimingFeeProposalTxn(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage, String title, String description, long vidaIdClaimingFee) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
            this.title = title;
            this.description = description;
            this.vidaIdClaimingFee = vidaIdClaimingFee;
        }

        public ChangeVidaIdClaimingFeeProposalTxn(JSONObject json) {
            super(json);
            this.title = json.getString(BinaryJSONKeyMapper.TITLE);
            this.description = json.getString(BinaryJSONKeyMapper.DESCRIPTION);
            this.vidaIdClaimingFee = json.getLong(BinaryJSONKeyMapper.VIDA_ID_CLAIMING_FEE);
        }

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Change VIDA ID Claiming Fee Proposal";
        }

        @Override
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("title", title);
            data.put("description", description);
            data.put("vidaIdClaimingFee", vidaIdClaimingFee);
            return data;
        }
    }

    @Getter
    public static class ChangeVmOwnerTxnFeeShareProposalTxn extends FalconTransaction {
        public static final int IDENTIFIER = 2010;

        private final String title;
        private final String description;
        private final int vmOwnerTxnFeeShare;

        public ChangeVmOwnerTxnFeeShareProposalTxn(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage, String title, String description, int vmOwnerTxnFeeShare) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
            this.title = title;
            this.description = description;
            this.vmOwnerTxnFeeShare = vmOwnerTxnFeeShare;
        }

        public ChangeVmOwnerTxnFeeShareProposalTxn(JSONObject json) {
            super(json);
            this.title = json.getString(BinaryJSONKeyMapper.TITLE);
            this.description = json.getString(BinaryJSONKeyMapper.DESCRIPTION);
            this.vmOwnerTxnFeeShare = json.getInt(BinaryJSONKeyMapper.VM_OWNER_TRANSACTION_FEE_SHARE);
        }

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Change VM Owner Transaction Fee Share Proposal";
        }

        @Override
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("title", title);
            data.put("description", description);
            data.put("vmOwnerTxnFeeShare", vmOwnerTxnFeeShare);
            return data;
        }
    }

    @Getter
    public static class OtherProposalTxn extends FalconTransaction {
        public static final int IDENTIFIER = 2011;

        private final String title;
        private final String description;

        public OtherProposalTxn(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage, String title, String description) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
            this.title = title;
            this.description = description;
        }

        public OtherProposalTxn(JSONObject json) {
            super(json);
            this.title = json.getString(BinaryJSONKeyMapper.TITLE);
            this.description = json.getString(BinaryJSONKeyMapper.DESCRIPTION);
        }

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Other Proposal";
        }

        @Override
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("title", title);
            data.put("description", description);
            return data;
        }
    }

    @Getter
    public static class VoteOnProposalTxn extends FalconTransaction {
        public static final int IDENTIFIER = 2012;

        private final String proposalHash;
        private final byte vote;

        public VoteOnProposalTxn(String transactionHash, String sender, int nonce, int size, long feePerByte, long paidActionFee, long paidTotalFee, boolean success, String errorMessage, String proposalHash, byte vote) {
            super(transactionHash, sender, nonce, size, feePerByte, paidActionFee, paidTotalFee, success, errorMessage);
            this.proposalHash = proposalHash;
            this.vote = vote;
        }

        public VoteOnProposalTxn(JSONObject json) {
            super(json);
            this.proposalHash = json.getString(BinaryJSONKeyMapper.PROPOSAL_HASH);
            this.vote = (byte) json.getInt(BinaryJSONKeyMapper.VOTE);
        }

        @Override
        public int getIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public String getType() {
            return "Vote On Proposal";
        }

        @Override
        public JSONObject toJson() {
            JSONObject data = super.toJson();
            data.put("proposalHash", proposalHash);
            data.put("vote", vote);
            return data;
        }
    }
}
