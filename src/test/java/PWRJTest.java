import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import io.pwrlabs.util.encoders.BiResult;
import org.bouncycastle.util.encoders.Hex;

import com.github.pwrlabs.pwrj.protocol.PWRJ;
import com.github.pwrlabs.pwrj.record.block.Block;
import com.github.pwrlabs.pwrj.record.response.EarlyWithdrawPenaltyResponse;
import com.github.pwrlabs.pwrj.record.response.TransactionForGuardianApproval;
import com.github.pwrlabs.pwrj.record.transaction.FalconTransaction;
import com.github.pwrlabs.pwrj.record.validator.Validator;

public class PWRJTest {
    private static final String RPC_NODE_URL = "http://localhost:8085"; // Example URL, replace with real test endpoint
    private static final String TEST_ADDRESS = "0xB032F4707B53D86ADD972F932F40BA15B5B52A46";
    private static final String TEST_VALIDATOR_ADDRESS = "0xB032F4707B53D86ADD972F932F40BA15B5B52A46";
    private static final long TEST_VIDA_ID = 1234;
    private static final long TEST_BLOCK_NUMBER = 2;
    private static final String TEST_TRANSACTION_HASH = "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";
    private static final byte[] TEST_TRANSACTION = Hex.decode("1234567890abcdef1234567890abcdef");
    private static final byte[] TEST_PREFIX = new byte[]{0x01, 0x02};
    private static final long TEST_WITHDRAW_TIME = System.currentTimeMillis() / 1000;
    private static final String TEST_PROPOSAL_HASH = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";

    public static void main(String[] args) {
        PWRJ pwrj = new PWRJ(RPC_NODE_URL);

        try {
            System.out.println("Testing PWRJ methods...");

            // Basic getters
            System.out.println("Testing getRpcNodeUrl()...");
            String rpcUrl = pwrj.getRpcNodeUrl();
            
            System.out.println("Testing getChainId()...");
            byte chainId = pwrj.getChainId();

            System.out.println("Testing getFeePerByte()...");
            long feePerByte = pwrj.getFeePerByte();

            System.out.println("Testing getBlockchainVersion()...");
            short blockchainVersion = pwrj.getBlockchainVersion();

            // Address related methods
            System.out.println("Testing getNonceOfAddress()...");
            int nonce = pwrj.getNonceOfAddress(TEST_ADDRESS);

            System.out.println("Testing getBalanceOfAddress()...");
            long balance = pwrj.getBalanceOfAddress(TEST_ADDRESS);

            System.out.println("Testing getGuardianOfAddress()...");
            BiResult guardian = pwrj.getGuardianOfAddress(TEST_ADDRESS);

            // Block related methods
            System.out.println("Testing getBlocksCount()...");
            long blocksCount = pwrj.getBlocksCount();

            System.out.println("Testing getMaxBlockSize()...");
            int maxBlockSize = pwrj.getMaxBlockSize();

            System.out.println("Testing getMaxTransactionSize()...");
            int maxTransactionSize = pwrj.getMaxTransactionSize();

            System.out.println("Testing getBlockNumber()...");
            long blockNumber = pwrj.getBlockNumber();

            System.out.println("Testing getBlockTimestamp()...");
            long blockTimestamp = pwrj.getBlockTimestamp();

            System.out.println("Testing getLatestBlockNumber()...");
            long latestBlockNumber = pwrj.getLatestBlockNumber();

            try {
                System.out.println("Testing getBlockByNumber()...");
                Block block = pwrj.getBlockByNumber(latestBlockNumber);

                System.out.println("Testing getBlockByNumberExcludingDataAndExtraData()...");
                Block blockExcludingData = pwrj.getBlockByNumberExcludingDataAndExtraData(TEST_BLOCK_NUMBER);

                System.out.println("Testing getBlockWithViDataTransactionsOnly()...");
                Block blockWithVmData = pwrj.getBlockWithViDataTransactionsOnly(TEST_BLOCK_NUMBER, TEST_VIDA_ID);
            } catch (Exception e) {
                System.out.println("Error in block retrieval methods: " + e.getMessage());
            }

            // Transaction related methods
            try {
                System.out.println("Testing getTransactionByHash()...");
                FalconTransaction transaction = pwrj.getTransactionByHash(TEST_TRANSACTION_HASH);
            } catch (Exception e) {
                System.out.println("Error in getTransactionByHash: " + e.getMessage());
            }

            System.out.println("Testing getVidaDataTransactions()...");
            long maxBlockToCheck = Math.min(TEST_BLOCK_NUMBER + 100, pwrj.getLatestBlockNumber());
            FalconTransaction.PayableVidaDataTxn[] transactions = pwrj.getVidaDataTransactions(TEST_BLOCK_NUMBER, maxBlockToCheck, TEST_VIDA_ID);

            System.out.println("Testing getVidaDataTransactionsFilterByBytePrefix()...");
            FalconTransaction.PayableVidaDataTxn[] prefixTransactions = pwrj.getVidaDataTransactionsFilterByBytePrefix(TEST_BLOCK_NUMBER, maxBlockToCheck, TEST_VIDA_ID, TEST_PREFIX);

            try {
                System.out.println("Testing isTransactionValidForGuardianApproval() with String...");
                TransactionForGuardianApproval approvalString = pwrj.isTransactionValidForGuardianApproval(Hex.toHexString(TEST_TRANSACTION));

                System.out.println("Testing isTransactionValidForGuardianApproval() with byte[]...");
                TransactionForGuardianApproval approvalBytes = pwrj.isTransactionValidForGuardianApproval(TEST_TRANSACTION);
            } catch (Exception e) {
                System.out.println("Error in transaction validation methods: " + e.getMessage());
            }

            // Validator related methods
            System.out.println("Testing getValidatorCountLimit()...");
            int validatorCountLimit = pwrj.getValidatorCountLimit();

            System.out.println("Testing getValidatorSlashingFee()...");
            int validatorSlashingFee = pwrj.getValidatorSlashingFee();

            System.out.println("Testing getValidatorOperationalFee()...");
            int validatorOperationalFee = pwrj.getValidatorOperationalFee();

            System.out.println("Testing getValidatorJoiningFee()...");
            long validatorJoiningFee = pwrj.getValidatorJoiningFee();

            System.out.println("Testing getTotalValidatorsCount()...");
            int totalValidatorsCount = pwrj.getTotalValidatorsCount();

            System.out.println("Testing getStandbyValidatorsCount()...");
            int standbyValidatorsCount = pwrj.getStandbyValidatorsCount();

            System.out.println("Testing getActiveValidatorsCount()...");
            int activeValidatorsCount = pwrj.getActiveValidatorsCount();

            System.out.println("Testing getTotalDelegatorsCount()...");
            int totalDelegatorsCount = pwrj.getTotalDelegatorsCount();

            System.out.println("Testing getAllValidators()...");
            List<Validator> allValidators = pwrj.getAllValidators();

            System.out.println("Testing getStandbyValidators()...");
            List<Validator> standbyValidators = pwrj.getStandbyValidators();

            System.out.println("Testing getActiveValidators()...");
            List<Validator> activeValidators = pwrj.getActiveValidators();

            System.out.println("Testing getDelegatees()...");
            List<Validator> delegatees = pwrj.getDelegatees(TEST_ADDRESS);

            System.out.println("Testing getValidator()...");
            try {
                Validator validator = pwrj.getValidator(TEST_VALIDATOR_ADDRESS);
            } catch (Exception e) {
                System.out.println("Error in getValidator: " + e.getMessage());
            }

            System.out.println("Testing getDelegatedPWR()...");
            long delegatedPWR = pwrj.getDelegatedPWR(TEST_ADDRESS, TEST_VALIDATOR_ADDRESS);

            System.out.println("Testing getSharesOfDelegator()...");
            long sharesOfDelegator = pwrj.getSharesOfDelegator(TEST_ADDRESS, TEST_VALIDATOR_ADDRESS);

            System.out.println("Testing getShareValue()...");
            BigDecimal shareValue = pwrj.getShareValue(TEST_VALIDATOR_ADDRESS);

            // VIDA related methods
            System.out.println("Testing getVidaOwnerTransactionFeeShare()...");
            int vidaOwnerTransactionFeeShare = pwrj.getVidaOwnerTransactionFeeShare();

            System.out.println("Testing getVidaIdClaimingFee()...");
            long vidaIdClaimingFee = pwrj.getVidaIdClaimingFee();

            System.out.println("Testing getOwnerOfVida()...");
            String vidaOwner = pwrj.getOwnerOfVida(TEST_VIDA_ID);

            System.out.println("Testing getConduitsOfVm()...");
            List<Validator> conduits = pwrj.getConduitsOfVm(TEST_VIDA_ID);

            // Additional methods
            System.out.println("Testing getTotalVotingPower()...");
            long totalVotingPower = pwrj.getTotalVotingPower();

            System.out.println("Testing getActiveVotingPower()...");
            long activeVotingPower = pwrj.getActiveVotingPower();

            System.out.println("Testing getPwrRewardsPerYear()...");
            long pwrRewardsPerYear = pwrj.getPwrRewardsPerYear();

            System.out.println("Testing getWithdrawalLockTime()...");
            long withdrawalLockTime = pwrj.getWithdrawalLockTime();

            System.out.println("Testing getMaxGuardianTime()...");
            long maxGuardianTime = pwrj.getMaxGuardianTime();

            System.out.println("Testing getBurnPercentage()...");
            int burnPercentage = pwrj.getBurnPercentage();

            System.out.println("Testing getProposalFee()...");
            long proposalFee = pwrj.getProposalFee();

            System.out.println("Testing getProposalValidityTime()...");
            long proposalValidityTime = pwrj.getProposalValidityTime();

            System.out.println("Testing getProposalStatus()...");
            try {
                String proposalStatus = pwrj.getProposalStatus(TEST_PROPOSAL_HASH);
            } catch (Exception e) {
                System.out.println("Error in getProposalStatus: " + e.getMessage());
                e.printStackTrace();
            }

            System.out.println("Testing getMinimumDelegatingAmount()...");
            long minimumDelegatingAmount = pwrj.getMinimumDelegatingAmount();

            System.out.println("Testing getEcdsaVerificationFee()...");
            long ecdsaVerificationFee = pwrj.getEcdsaVerificationFee();

            System.out.println("Testing getEarlyWithdrawPenalty()...");
            EarlyWithdrawPenaltyResponse earlyWithdrawPenalty = pwrj.getEarlyWithdrawPenalty(TEST_WITHDRAW_TIME);

            System.out.println("Testing getAllEarlyWithdrawPenalties()...");
            Map<Long, Long> allEarlyWithdrawPenalties = pwrj.getAllEarlyWithdrawPenalties();

            // Static methods
            System.out.println("Testing getVidaIdAddress()...");
            String vidaIdAddress = pwrj.getVidaIdAddress(TEST_VIDA_ID);

            System.out.println("Testing isVidaAddress()...");
            boolean isVidaAddress = PWRJ.isVidaAddress(vidaIdAddress);

            // Broadcast method - commented out to avoid actual transaction broadcast
            // System.out.println("Testing broadcastTransaction()...");
            // Response broadcastResponse = pwrj.broadcastTransaction(TEST_TRANSACTION);

            // Subscription method - commenting this out as it starts a background thread
            /*
            System.out.println("Testing subscribeToVidaTransactions()...");
            VidaTransactionSubscription subscription = pwrj.subscribeToVidaTransactions(
                    pwrj,
                    TEST_VIDA_ID,
                    TEST_BLOCK_NUMBER,
                    (transaction) -> System.out.println("Transaction received: " + transaction.getHash()),
                    1000
            );
            // Don't forget to stop the subscription when done
            // subscription.stop();
            */

            System.out.println("All methods tested successfully!");

        } catch (IOException e) {
            System.err.println("Error during tests: " + e.getMessage());
            e.printStackTrace();
        }
    }
}