import com.github.pwrlabs.pwrj.Utils.Falcon;
import com.github.pwrlabs.pwrj.Utils.Hex;
import com.github.pwrlabs.pwrj.entities.FalconTransaction;
import com.github.pwrlabs.pwrj.protocol.PWRJ;
import com.github.pwrlabs.pwrj.record.response.EarlyWithdrawPenaltyResponse;
import com.github.pwrlabs.pwrj.record.response.Response;
import com.github.pwrlabs.pwrj.wallet.PWRFalconWallet;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.pqc.crypto.falcon.FalconParameters;
import java.math.BigInteger;
import java.util.Random;

import static com.github.pwrlabs.pwrj.Utils.NewError.errorIf;

public class GovernanceTest {

    // Note: The wallet initialization has changed. We need to use AsymmetricCipherKeyPair
    // instead of directly using a private key.
    private static final String PRIVATE_KEY_HEX = "1648717022721337370284939639895780772097759458487621889076665061529456636895";
    private static final PWRJ pwrj = new PWRJ("http://localhost:8085");

    public static void main(String[] args) throws Exception {
        // Create wallet with proper key pair initialization
        PWRFalconWallet validator = PWRFalconWallet.loadWallet(pwrj, "wallet");

        // Uncomment tests as needed
         testChangeFeePerByteProposalTxn(validator, pwrj);
         testChangeEarlyWithdrawPenaltyProposalTxn(validator, pwrj);
         testChangeMaxBlockSizeProposalTxn(validator, pwrj);
         testChangeMaxTxnSizeProposalTxn(validator, pwrj);
         testChangeOverallBurnPercentageProposalTxn(validator, pwrj);
         testChangeRewardPerYearProposalTxn(validator, pwrj);
         testChangeValidatorCountLimitProposalTxn(validator, pwrj);
         testChangeValidatorJoiningFeeProposalTxn(validator, pwrj);
         testChangeVidaIdClaimingFeeProposalTxn(validator, pwrj);
         testChangeVmOwnerTxnFeeShareProposalTxn(validator, pwrj);
         testOtherProposalTxn(validator, pwrj);

        System.out.println("All tests completed successfully");

    }

    private static void testChangeFeePerByteProposalTxn(PWRFalconWallet validator, PWRJ pwrj) {
        try {
            long feePerByte = pwrj.getFeePerByte();
            long newFeePerByte = feePerByte + 1;

            String title = "Fee per byte change proposal title";
            String description = "I want to change fee per byte";

            Response r = validator.proposeChangeFeePerByte(title, description, newFeePerByte, feePerByte);
            errorIf(!r.isSuccess(), r.getError());

            waitUntilTransactionsIsProcessed(r.getTransactionHash());

            System.out.println("Transaction hash: " + r.getTransactionHash());
            byte[] transactionHash = Hex.decode(r.getTransactionHash().startsWith("0x") ? r.getTransactionHash().substring(2) : r.getTransactionHash());
            r = validator.voteOnProposal(transactionHash, (byte) 0, pwrj.getFeePerByte());
            errorIf(!r.isSuccess(), r.getError());

            waitUntilTransactionsIsProcessed(r.getTransactionHash());

            errorIf(pwrj.getFeePerByte() != newFeePerByte, "Failed to change fee per byte. Expected: " + newFeePerByte + ", Actual: " + pwrj.getFeePerByte());

            System.out.println("Fee per byte changed successfully");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to change fee per byte");
            try { System.out.println("Fee per byte: " + pwrj.getFeePerByte()); } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    private static void testChangeEarlyWithdrawPenaltyProposalTxn(PWRFalconWallet validator, PWRJ pwrj) {
        try {
            Random random = new Random();
            long earlyWithdrawTime = 5 * 24 * 60 * 60; // 5 days
            int earlyWithdrawPenalty = random.nextInt(1000); // 10% x/10000

            String title = "Change early withdraw penalty proposal title";
            String description = "Change early withdrawal penalty";

            Response r = validator.proposeChangeEarlyWithdrawPenalty(title, description, earlyWithdrawTime, earlyWithdrawPenalty, pwrj.getFeePerByte());
            errorIf(!r.isSuccess(), r.getError());

            waitUntilTransactionsIsProcessed(r.getTransactionHash());

            byte[] transactionHash = Hex.decode(r.getTransactionHash().startsWith("0x") ? r.getTransactionHash().substring(2) : r.getTransactionHash());
            r = validator.voteOnProposal(transactionHash, (byte) 0, pwrj.getFeePerByte());
            errorIf(!r.isSuccess(), r.getError());

            waitUntilTransactionsIsProcessed(r.getTransactionHash());

            EarlyWithdrawPenaltyResponse response = pwrj.getEarlyWithdrawPenalty(earlyWithdrawTime);

            errorIf(!response.isEarlyWithdrawAvailable(), "Failed to change early withdrawal penalty. Early withdrawal not available");
            errorIf(response.getPenalty() != earlyWithdrawPenalty, "Failed to change early withdrawal penalty. Expected: " + earlyWithdrawPenalty + ", Actual: " + response.getPenalty());

            System.out.println("Early withdrawal penalty changed successfully");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to change early withdrawal penalty");
        }
    }

    private static void testChangeMaxBlockSizeProposalTxn(PWRFalconWallet validator, PWRJ pwrj) {
        try {
            int currentMaxBlockSize = pwrj.getMaxBlockSize();
            int newMaxBlockSize = currentMaxBlockSize + 100000; // add 100KB

            String title = "Change max block size proposal title";
            String description = "Change max block size";

            Response r = validator.proposeChangeMaxBlockSize(title, description, newMaxBlockSize, pwrj.getFeePerByte());
            errorIf(!r.isSuccess(), r.getError());

            waitUntilTransactionsIsProcessed(r.getTransactionHash());

            byte[] transactionHash = Hex.decode(r.getTransactionHash().startsWith("0x") ? r.getTransactionHash().substring(2) : r.getTransactionHash());
            r = validator.voteOnProposal(transactionHash, (byte) 0, pwrj.getFeePerByte());
            errorIf(!r.isSuccess(), r.getError());

            waitUntilTransactionsIsProcessed(r.getTransactionHash());

            errorIf(pwrj.getMaxBlockSize() != newMaxBlockSize, "Failed to change max block size");

            System.out.println("Max block size changed successfully");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to change max block size");
        }
    }

    private static void testChangeMaxTxnSizeProposalTxn(PWRFalconWallet validator, PWRJ pwrj) {
        try {
            int currentMaxTxnSize = pwrj.getMaxTransactionSize();
            int newMaxTxnSize = currentMaxTxnSize + 10000; // add 10KB

            String title = "Change max transaction size title";
            String description = "Change max transaction size";

            Response r = validator.proposeChangeMaxTxnSize(title, description, newMaxTxnSize, pwrj.getFeePerByte());
            errorIf(!r.isSuccess(), r.getError());

            waitUntilTransactionsIsProcessed(r.getTransactionHash());

            byte[] transactionHash = Hex.decode(r.getTransactionHash().startsWith("0x") ? r.getTransactionHash().substring(2) : r.getTransactionHash());
            r = validator.voteOnProposal(transactionHash, (byte) 0, pwrj.getFeePerByte());
            errorIf(!r.isSuccess(), r.getError());

            waitUntilTransactionsIsProcessed(r.getTransactionHash());

            errorIf(pwrj.getMaxTransactionSize() != newMaxTxnSize, "Failed to change max transaction size");

            System.out.println("Max transaction size changed successfully");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to change max transaction size");
        }
    }

    private static void testChangeOverallBurnPercentageProposalTxn(PWRFalconWallet validator, PWRJ pwrj) {
        try {
            int currentBurnPercentage = pwrj.getBurnPercentage();
            int newBurnPercentage = currentBurnPercentage + 1; // add 1%

            String title = "Change overall burn proposal title";
            String description = "Change overall burn percentage";

            Response r = validator.proposeChangeOverallBurnPercentage(title, description, newBurnPercentage, pwrj.getFeePerByte());
            errorIf(!r.isSuccess(), r.getError());

            waitUntilTransactionsIsProcessed(r.getTransactionHash());

            byte[] transactionHash = Hex.decode(r.getTransactionHash().startsWith("0x") ? r.getTransactionHash().substring(2) : r.getTransactionHash());
            r = validator.voteOnProposal(transactionHash, (byte) 0, pwrj.getFeePerByte());
            errorIf(!r.isSuccess(), r.getError());

            waitUntilTransactionsIsProcessed(r.getTransactionHash());

            errorIf(pwrj.getBurnPercentage() != newBurnPercentage, "Failed to change overall burn percentage");

            System.out.println("Overall burn percentage changed successfully");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to change overall burn percentage");
        }
    }

    private static void testChangeRewardPerYearProposalTxn(PWRFalconWallet validator, PWRJ pwrj) {
        try {
            long currentRewardPerYear = pwrj.getPwrRewardsPerYear();
            long newRewardPerYear = currentRewardPerYear + 1; // add 1 PWR

            String title = "Change reward per year proposal title";
            String description = "Change reward per year";

            Response r = validator.proposeChangeRewardPerYear(title, description, newRewardPerYear, pwrj.getFeePerByte());
            errorIf(!r.isSuccess(), r.getError());

            waitUntilTransactionsIsProcessed(r.getTransactionHash());

            byte[] transactionHash = Hex.decode(r.getTransactionHash().startsWith("0x") ? r.getTransactionHash().substring(2) : r.getTransactionHash());
            r = validator.voteOnProposal(transactionHash, (byte) 0, pwrj.getFeePerByte());
            errorIf(!r.isSuccess(), r.getError());

            waitUntilTransactionsIsProcessed(r.getTransactionHash());

            errorIf(pwrj.getPwrRewardsPerYear() != newRewardPerYear, "Failed to change reward per year. Expected: " + newRewardPerYear + ", Actual: " + pwrj.getPwrRewardsPerYear());

            System.out.println("Reward per year changed successfully");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to change reward per year");
        }
    }

    private static void testChangeValidatorCountLimitProposalTxn(PWRFalconWallet validator, PWRJ pwrj) {
        try {
            int currentValidatorCountLimit = pwrj.getValidatorCountLimit();
            int newValidatorCountLimit = currentValidatorCountLimit + 10; // add 10 validators

            String title = "Change validator count limit proposal title";
            String description = "Change validator count limit";

            Response r = validator.proposeChangeValidatorCountLimit(title, description, newValidatorCountLimit, pwrj.getFeePerByte());
            errorIf(!r.isSuccess(), r.getError());

            waitUntilTransactionsIsProcessed(r.getTransactionHash());

            byte[] transactionHash = Hex.decode(r.getTransactionHash().startsWith("0x") ? r.getTransactionHash().substring(2) : r.getTransactionHash());
            r = validator.voteOnProposal(transactionHash, (byte) 0, pwrj.getFeePerByte());
            errorIf(!r.isSuccess(), r.getError());

            waitUntilTransactionsIsProcessed(r.getTransactionHash());

            errorIf(pwrj.getValidatorCountLimit() != newValidatorCountLimit, "Failed to change validator count limit");

            System.out.println("Validator count limit changed successfully");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to change validator count limit");
        }
    }

    private static void testChangeValidatorJoiningFeeProposalTxn(PWRFalconWallet validator, PWRJ pwrj) {
        try {
            long currentJoiningFee = pwrj.getValidatorJoiningFee();
            long newJoiningFee = currentJoiningFee + 1000000000; // add 1 PWR

            String title = "Change validator joining fee proposal title";
            String description = "Change validator joining fee";

            Response r = validator.proposeChangeValidatorJoiningFee(title, description, newJoiningFee, pwrj.getFeePerByte());
            errorIf(!r.isSuccess(), r.getError());

            waitUntilTransactionsIsProcessed(r.getTransactionHash());

            byte[] transactionHash = Hex.decode(r.getTransactionHash().startsWith("0x") ? r.getTransactionHash().substring(2) : r.getTransactionHash());
            r = validator.voteOnProposal(transactionHash, (byte) 0, pwrj.getFeePerByte());
            errorIf(!r.isSuccess(), r.getError());

            waitUntilTransactionsIsProcessed(r.getTransactionHash());

            errorIf(pwrj.getValidatorJoiningFee() != newJoiningFee, "Failed to change validator joining fee");

            System.out.println("Validator joining fee changed successfully");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to change validator joining fee");
        }
    }

    private static void testChangeVidaIdClaimingFeeProposalTxn(PWRFalconWallet validator, PWRJ pwrj) {
        try {
            long currentClaimingFee = pwrj.getVidaIdClaimingFee();
            long newClaimingFee = currentClaimingFee + 1000000; // add 0.01 PWR

            String title = "Change VIDA ID Claiming Fee Proposal Title";
            String description = "Change VIDA ID claiming fee";

            // Note: The method name has changed from VM to VIDA
            Response r = validator.proposeChangeVidaIdClaimingFee(title, description, newClaimingFee, pwrj.getFeePerByte());
            errorIf(!r.isSuccess(), r.getError());

            waitUntilTransactionsIsProcessed(r.getTransactionHash());

            byte[] transactionHash = Hex.decode(r.getTransactionHash().startsWith("0x") ? r.getTransactionHash().substring(2) : r.getTransactionHash());
            r = validator.voteOnProposal(transactionHash, (byte) 0, pwrj.getFeePerByte());
            errorIf(!r.isSuccess(), r.getError());

            waitUntilTransactionsIsProcessed(r.getTransactionHash());

            errorIf(pwrj.getVidaIdClaimingFee() != newClaimingFee, "Failed to change VIDA ID claiming fee");

            System.out.println("VIDA ID claiming fee changed successfully");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to change VIDA ID claiming fee");
        }
    }

    private static void testChangeVmOwnerTxnFeeShareProposalTxn(PWRFalconWallet validator, PWRJ pwrj) {
        try {
            int currentFeeShare = pwrj.getVidaOwnerTransactionFeeShare();
            int newFeeShare = currentFeeShare + 1; // add 1%

            String title = "Change VM owner transfer fee share title";
            String description = "Change VM owner transaction fee share";

            Response r = validator.proposeChangeVmOwnerTxnFeeShare(title, description, newFeeShare, pwrj.getFeePerByte());
            errorIf(!r.isSuccess(), r.getError());

            waitUntilTransactionsIsProcessed(r.getTransactionHash());

            byte[] transactionHash = Hex.decode(r.getTransactionHash().startsWith("0x") ? r.getTransactionHash().substring(2) : r.getTransactionHash());
            r = validator.voteOnProposal(transactionHash, (byte) 0, pwrj.getFeePerByte());
            errorIf(!r.isSuccess(), r.getError());

            waitUntilTransactionsIsProcessed(r.getTransactionHash());

            errorIf(pwrj.getVidaOwnerTransactionFeeShare() != newFeeShare, "Failed to change VM owner transaction fee share");

            System.out.println("VM owner transaction fee share changed successfully");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to change VM owner transaction fee share");
        }
    }

    private static void testOtherProposalTxn(PWRFalconWallet validator, PWRJ pwrj) {
        try {
            String title = "Other proposal title";
            String description = "Other proposal description";

            // Note: Method name changed to proposeOther
            Response r = validator.proposeOther(title, description, pwrj.getFeePerByte());
            errorIf(!r.isSuccess(), r.getError());

            waitUntilTransactionsIsProcessed(r.getTransactionHash());

            byte[] transactionHash = Hex.decode(r.getTransactionHash().startsWith("0x") ? r.getTransactionHash().substring(2) : r.getTransactionHash());
            r = validator.voteOnProposal(transactionHash, (byte) 0, pwrj.getFeePerByte());
            errorIf(!r.isSuccess(), r.getError());

            System.out.println("Other proposal created and voted on successfully");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to create or vote on other proposal");
        }
    }

    private static void waitUntilTransactionsIsProcessed(String txnHash) throws Exception {
        long maxTime = 5000; // 1 minute
        long timeNow = System.currentTimeMillis();

        while (System.currentTimeMillis() - timeNow < maxTime) {
            try {
                FalconTransaction txn = pwrj.getTransactionByHash(txnHash);
                if(txn != null) return;
            } catch (Exception e) {

            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        throw new Exception("Transaction not processed in time");
    }
}