import com.github.pwrlabs.pwrj.protocol.PWRJ;
import com.github.pwrlabs.pwrj.record.block.Block;
import com.github.pwrlabs.pwrj.record.response.EarlyWithdrawPenaltyResponse;
import com.github.pwrlabs.pwrj.record.response.Response;
import com.github.pwrlabs.pwrj.record.response.TransactionForGuardianApproval;
import com.github.pwrlabs.pwrj.record.transaction.Interface.Transaction;
import com.github.pwrlabs.pwrj.record.transaction.ecdsa.VmDataTransaction;
import com.github.pwrlabs.pwrj.record.validator.Validator;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class PWRJTester {
    private static PWRJ pwrj;
    private static final String RPC_NODE_URL = "https://pwrrpc.pwrlabs.io/"; // PWR Labs RPC URL

    // Sample data for testing - replace with valid values for your network
    private static final String SAMPLE_ADDRESS = "0x1234567890123456789012345678901234567890";
    private static final String SAMPLE_VALIDATOR_ADDRESS = "0x2345678901234567890123456789012345678901";
    private static final long SAMPLE_VM_ID = 1;
    private static final String SAMPLE_TRANSACTION_HASH = "0x3456789012345678901234567890123456789012345678901234567890123456";
    private static final String SAMPLE_PROPOSAL_HASH = "0x4567890123456789012345678901234567890123456789012345678901234567";
    private static final byte[] SAMPLE_TRANSACTION = new byte[]{0x01, 0x02, 0x03}; // This should be a valid transaction

    public static void main(String[] args) {
        try {
            System.out.println("Starting PWRJ tests...");
            initialize();
            testBasicInfo();
            testBlockOperations();
            testAddressOperations();
            testValidatorOperations();
            testVmOperations();
            testTransactionOperations();

            System.out.println("\nAll tests completed!");
        } catch (Exception e) {
            System.out.println("Test suite failed with exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void initialize() {
        System.out.println("Initializing PWRJ...");
        pwrj = new PWRJ(RPC_NODE_URL);
        System.out.println("PWRJ initialized with RPC URL: " + pwrj.getRpcNodeUrl());
    }

    private static void testBasicInfo() {
        System.out.println("\n=== Testing Basic Information Methods ===");

        try {
            byte chainId = pwrj.getChainId();
            printTestResult("getChainId", chainId);

            long feePerByte = pwrj.getFeePerByte();
            printTestResult("getFeePerByte", feePerByte);

            short blockchainVersion = pwrj.getBlockchainVersion();
            printTestResult("getBlockchainVersion", blockchainVersion);

            int maxBlockSize = pwrj.getMaxBlockSize();
            printTestResult("getMaxBlockSize", maxBlockSize);

            int maxTransactionSize = pwrj.getMaxTransactionSize();
            printTestResult("getMaxTransactionSize", maxTransactionSize);

            int validatorCountLimit = pwrj.getValidatorCountLimit();
            printTestResult("getValidatorCountLimit", validatorCountLimit);

            int validatorSlashingFee = pwrj.getValidatorSlashingFee();
            printTestResult("getValidatorSlashingFee", validatorSlashingFee);

            int vmOwnerTransactionFeeShare = pwrj.getVmOwnerTransactionFeeShare();
            printTestResult("getVmOwnerTransactionFeeShare", vmOwnerTransactionFeeShare);

            int burnPercentage = pwrj.getBurnPercentage();
            printTestResult("getBurnPercentage", burnPercentage);

            int validatorOperationalFee = pwrj.getValidatorOperationalFee();
            printTestResult("getValidatorOperationalFee", validatorOperationalFee);

            long blockNumber = pwrj.getBlockNumber();
            printTestResult("getBlockNumber", blockNumber);

            long blockTimestamp = pwrj.getBlockTimestamp();
            printTestResult("getBlockTimestamp", blockTimestamp);

            long totalVotingPower = pwrj.getTotalVotingPower();
            printTestResult("getTotalVotingPower", totalVotingPower);

            long pwrRewardsPerYear = pwrj.getPwrRewardsPerYear();
            printTestResult("getPwrRewardsPerYear", pwrRewardsPerYear);

            long withdrawalLockTime = pwrj.getWithdrawalLockTime();
            printTestResult("getWithdrawalLockTime", withdrawalLockTime);

            long validatorJoiningFee = pwrj.getValidatorJoiningFee();
            printTestResult("getValidatorJoiningFee", validatorJoiningFee);

            long maxGuardianTime = pwrj.getMaxGuardianTime();
            printTestResult("getMaxGuardianTime", maxGuardianTime);

            long vmIdClaimingFee = pwrj.getVmIdClaimingFee();
            printTestResult("getVmIdClaimingFee", vmIdClaimingFee);

            long proposalFee = pwrj.getProposalFee();
            printTestResult("getProposalFee", proposalFee);

            long proposalValidityTime = pwrj.getProposalValidityTime();
            printTestResult("getProposalValidityTime", proposalValidityTime);

            long minimumDelegatingAmount = pwrj.getMinimumDelegatingAmount();
            printTestResult("getMinimumDelegatingAmount", minimumDelegatingAmount);

            long ecdsaVerificationFee = pwrj.getEcdsaVerificationFee();
            printTestResult("getEcdsaVerificationFee", ecdsaVerificationFee);

            long activeVotingPower = pwrj.getActiveVotingPower();
            printTestResult("getActiveVotingPower", activeVotingPower);
        } catch (Exception e) {
            handleException("Basic info tests", e);
        }
    }

    private static void testBlockOperations() {
        System.out.println("\n=== Testing Block Operations ===");

        try {
            long blocksCount = pwrj.getBlocksCount();
            printTestResult("getBlocksCount", blocksCount);

            long latestBlockNumber = pwrj.getLatestBlockNumber();
            printTestResult("getLatestBlockNumber", latestBlockNumber);

            if (latestBlockNumber >= 0) {
                try {
                    Block block = pwrj.getBlockByNumber(latestBlockNumber);
                    printTestResult("getBlockByNumber", "Successfully retrieved block " + latestBlockNumber);
                    printTestResult("Block hash", block.getHash());
                    printTestResult("Block timestamp", block.getTimestamp());

                    Block blockExcludingData = pwrj.getBlockByNumberExcludingDataAndExtraData(latestBlockNumber);
                    printTestResult("getBlockByNumberExcludingDataAndExtraData", "Successfully retrieved block " + latestBlockNumber);

                    Block blockWithVmData = pwrj.getBlockWithVmDataTransactionsOnly(latestBlockNumber, SAMPLE_VM_ID);
                    printTestResult("getBlockWithVmDataTransactionsOnly", "Successfully retrieved block " + latestBlockNumber + " for VM " + SAMPLE_VM_ID);

                    Map<String, Long> validatorsReward = pwrj.getValidatorsReward(latestBlockNumber);
                    printTestResult("getValidatorsReward", "Retrieved rewards for " + validatorsReward.size() + " validators");
                } catch (Exception e) {
                    handleException("Block retrieval tests", e);
                }
            }
        } catch (Exception e) {
            handleException("Block operations tests", e);
        }
    }

    private static void testAddressOperations() {
        System.out.println("\n=== Testing Address Operations ===");

        try {
            int nonce = pwrj.getNonceOfAddress(SAMPLE_ADDRESS);
            printTestResult("getNonceOfAddress", nonce);

            long balance = pwrj.getBalanceOfAddress(SAMPLE_ADDRESS);
            printTestResult("getBalanceOfAddress", balance);

            String guardian = pwrj.getGuardianOfAddress(SAMPLE_ADDRESS);
            printTestResult("getGuardianOfAddress", guardian != null ? guardian : "No guardian");

            // Test VM ID to address conversion
            String vmAddress = PWRJ.getVmIdAddress(SAMPLE_VM_ID);
            printTestResult("getVmIdAddress", vmAddress);

            boolean isVmAddress = PWRJ.isVmAddress(vmAddress);
            printTestResult("isVmAddress", isVmAddress);
        } catch (Exception e) {
            handleException("Address operations tests", e);
        }
    }

    private static void testValidatorOperations() {
        System.out.println("\n=== Testing Validator Operations ===");

        try {
            int totalValidatorsCount = pwrj.getTotalValidatorsCount();
            printTestResult("getTotalValidatorsCount", totalValidatorsCount);

            int standbyValidatorsCount = pwrj.getStandbyValidatorsCount();
            printTestResult("getStandbyValidatorsCount", standbyValidatorsCount);

            int activeValidatorsCount = pwrj.getActiveValidatorsCount();
            printTestResult("getActiveValidatorsCount", activeValidatorsCount);

            int totalDelegatorsCount = pwrj.getTotalDelegatorsCount();
            printTestResult("getTotalDelegatorsCount", totalDelegatorsCount);

            List<Validator> allValidators = pwrj.getAllValidators();
            printTestResult("getAllValidators", "Retrieved " + allValidators.size() + " validators");

            if (!allValidators.isEmpty()) {
                String firstValidatorAddress = allValidators.get(0).getAddress();
                printTestResult("First validator address", firstValidatorAddress);

                Validator validator = pwrj.getValidator(firstValidatorAddress);
                printTestResult("getValidator", "Retrieved validator " + firstValidatorAddress);
                printTestResult("Validator IP", validator.getIp());
                printTestResult("Validator voting power", validator.getVotingPower());

                BigDecimal shareValue = pwrj.getShareValue(firstValidatorAddress);
                printTestResult("getShareValue", shareValue);

                try {
                    long delegatedPWR = pwrj.getDelegatedPWR(SAMPLE_ADDRESS, firstValidatorAddress);
                    printTestResult("getDelegatedPWR", delegatedPWR);

                    long sharesOfDelegator = pwrj.getSharesOfDelegator(SAMPLE_ADDRESS, firstValidatorAddress);
                    printTestResult("getSharesOfDelegator", sharesOfDelegator);
                } catch (Exception e) {
                    handleException("Delegator-specific tests", e);
                }
            }

            List<Validator> standbyValidators = pwrj.getStandbyValidators();
            printTestResult("getStandbyValidators", "Retrieved " + standbyValidators.size() + " standby validators");

            List<Validator> activeValidators = pwrj.getActiveValidators();
            printTestResult("getActiveValidators", "Retrieved " + activeValidators.size() + " active validators");

            List<Validator> delegatees = pwrj.getDelegatees(SAMPLE_ADDRESS);
            printTestResult("getDelegatees", "Retrieved " + delegatees.size() + " delegatees for address " + SAMPLE_ADDRESS);

            // Test early withdrawal penalty
            long currentTime = System.currentTimeMillis();
            EarlyWithdrawPenaltyResponse earlyWithdrawPenalty = pwrj.getEarlyWithdrawPenalty(currentTime);
            printTestResult("getEarlyWithdrawPenalty", "Available: " + earlyWithdrawPenalty.isEarlyWithdrawAvailable() +
                    ", Penalty: " + earlyWithdrawPenalty.getPenalty());

            Map<Long, Long> allEarlyWithdrawPenalties = pwrj.getAllEarlyWithdrawPenalties();
            printTestResult("getAllEarlyWithdrawPenalties", "Retrieved " + allEarlyWithdrawPenalties.size() + " early withdraw penalties");
        } catch (Exception e) {
            handleException("Validator operations tests", e);
        }
    }

    private static void testVmOperations() {
        System.out.println("\n=== Testing VM Operations ===");

        try {
            String ownerOfVm = pwrj.getOwnerOfVm(SAMPLE_VM_ID);
            printTestResult("getOwnerOfVm", ownerOfVm != null ? ownerOfVm : "No owner for VM " + SAMPLE_VM_ID);

            List<Validator> conduitsOfVm = pwrj.getConduitsOfVm(SAMPLE_VM_ID);
            printTestResult("getConduitsOfVm", "Retrieved " + conduitsOfVm.size() + " conduits for VM " + SAMPLE_VM_ID);

            try {
                // Get latest block for VM transaction tests
                long latestBlock = pwrj.getLatestBlockNumber();
                long startingBlock = Math.max(0, latestBlock - 1000); // Look at last 1000 blocks or from genesis

                VmDataTransaction[] vmTransactions = pwrj.getVMDataTransactions(startingBlock, latestBlock, SAMPLE_VM_ID);
                printTestResult("getVMDataTransactions", "Retrieved " + vmTransactions.length + " VM transactions");

                byte[] prefix = new byte[]{0x01}; // Example prefix
                VmDataTransaction[] vmTransactionsFiltered = pwrj.getVMDataTransactionsFilterByBytePrefix(
                        startingBlock, latestBlock, SAMPLE_VM_ID, prefix);
                printTestResult("getVMDataTransactionsFilterByBytePrefix", "Retrieved " + vmTransactionsFiltered.length + " filtered VM transactions");
            } catch (Exception e) {
                handleException("VM transactions tests", e);
            }
        } catch (Exception e) {
            handleException("VM operations tests", e);
        }
    }

    private static void testTransactionOperations() {
        System.out.println("\n=== Testing Transaction Operations ===");

        try {
            // This test requires a valid transaction hash on the network
            try {
                Transaction transaction = pwrj.getTransactionByHash(SAMPLE_TRANSACTION_HASH);
                printTestResult("getTransactionByHash", "Successfully retrieved transaction " + SAMPLE_TRANSACTION_HASH);
            } catch (Exception e) {
                handleException("getTransactionByHash", e);
            }

            try {
                JSONObject txInfo = pwrj.getTransactionExplorerInfo(SAMPLE_TRANSACTION_HASH);
                printTestResult("getTransactionExplorerInfo", "Successfully retrieved transaction info " + SAMPLE_TRANSACTION_HASH);
            } catch (Exception e) {
                handleException("getTransactionExplorerInfo", e);
            }

            try {
                String sampleTxHex = Hex.toHexString(SAMPLE_TRANSACTION);
                TransactionForGuardianApproval txApproval = pwrj.isTransactionValidForGuardianApproval(sampleTxHex);
                printTestResult("isTransactionValidForGuardianApproval", "Valid: " + txApproval.isValid());
            } catch (Exception e) {
                handleException("isTransactionValidForGuardianApproval", e);
            }

            try {
                String proposalStatus = pwrj.getProposalStatus(SAMPLE_PROPOSAL_HASH);
                printTestResult("getProposalStatus", "Status: " + proposalStatus);
            } catch (Exception e) {
                handleException("getProposalStatus", e);
            }

            // Broadcasting - demonstrating API call, will likely fail with sample data
            try {
                Response response = pwrj.broadcastTransaction(SAMPLE_TRANSACTION);
                printTestResult("broadcastTransaction", "Success: " + response.isSuccess() +
                        ", Hash: " + (response.getTransactionHash() != null ? response.getTransactionHash() : "N/A") +
                        ", Error: " + (response.getError() != null ? response.getError() : "N/A"));
            } catch (Exception e) {
                handleException("broadcastTransaction", e);
            }
        } catch (Exception e) {
            handleException("Transaction operations tests", e);
        }
    }

    private static void printTestResult(String testName, Object result) {
        System.out.println(testName + ": " + result);
    }

    private static void handleException(String testName, Exception e) {
        System.out.println(testName + " FAILED: " + e.getMessage());
        e.printStackTrace();
    }
}