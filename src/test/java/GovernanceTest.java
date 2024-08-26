import com.github.pwrlabs.pwrj.protocol.PWRJ;
import com.github.pwrlabs.pwrj.record.response.EarlyWithdrawPenaltyResponse;
import com.github.pwrlabs.pwrj.record.response.Response;
import com.github.pwrlabs.pwrj.record.transaction.Transaction;
import com.github.pwrlabs.pwrj.wallet.PWRWallet;

import java.math.BigInteger;
import java.util.Random;

import static com.github.pwrlabs.pwrj.Utils.NewError.errorIf;

public class GovernanceTest {

    private static final BigInteger VALIDATOR_PRIVATE_KEY = new BigInteger("1648717022721337370284939639895780772097759458487621889076665061529456636895");
    private static final PWRJ pwrj = new PWRJ("https://pwrrpc.pwrlabs.io/");

    public static void main(String[] args) throws Exception {
        PWRWallet validator = new PWRWallet(VALIDATOR_PRIVATE_KEY, pwrj);
        Response r = validator.voteOnProposal("0xa0e55410dec3cbfb7868ee79411b17521b38fd9f4519e4f9d202baab8264bfc8", (byte) 0, validator.getNonce());
        System.out.println(r.isSuccess());
        System.out.println(r.getTransactionHash());
        System.out.println(r.getError());
//        long startingBlockNumber = pwrj.getLatestBlockNumber();
//        testChangeFeePerByteProposalTxn(validator, pwrj);
//        testChangeEarlyWithdrawPenaltyProposalTxn(validator, pwrj);
//        testChangeMaxBlockSizeProposalTxn(validator, pwrj);
//        testChangeMaxTxnSizeProposalTxn(validator, pwrj);
//        testChangeOverallBurnPercentageProposalTxn(validator, pwrj);
//        testChangeRewardPerYearProposalTxn(validator, pwrj);
//        testChangeValidatorCountLimitProposalTxn(validator, pwrj);
//        testChangeValidatorJoiningFeeProposalTxn(validator, pwrj);
//        testChangeVmIdClaimingFeeProposalTxn(validator, pwrj);
//        testChangeVmOwnerTxnFeeShareProposalTxn(validator, pwrj);
//        testOtherProposalTxn(validator, pwrj);
//        long endingBlockNumber = pwrj.getLatestBlockNumber();
//
//        System.out.println("starting block number:" + startingBlockNumber);
//        System.out.println("ending block number:" + endingBlockNumber);
//
//        startingBlockNumber = 1;
//        endingBlockNumber = 21;
//
//        testFetchTransactions(startingBlockNumber, endingBlockNumber, pwrj);
    }

    private static void testChangeFeePerByteProposalTxn(PWRWallet validator, PWRJ pwrj) {
        try {
            long feePerByte = pwrj.getFeePerByte();
            long newFeePerByte = feePerByte + 1;

            String title = "Fee per byte change proposal title";

            Response r = validator.createProposal_ChangeFeePerByte(newFeePerByte, title, "I want to change fee epr byte", validator.getNonce());
            errorIf(!r.isSuccess(), r.getError());

            Thread.sleep(5000);

            r = validator.voteOnProposal(r.getTransactionHash(), (byte) 0, validator.getNonce());
            errorIf(!r.isSuccess(), r.getError());

            Thread.sleep(5000);

            errorIf(pwrj.getFeePerByte() != newFeePerByte, "Failed to change fee per byte. Expected: " + newFeePerByte + ", Actual: " + pwrj.getFeePerByte());

            System.out.println("Fee per byte changed successfully");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to change fee per byte");
            try { System.out.println("Fee per byte: " + pwrj.getFeePerByte()); } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    private static void testChangeEarlyWithdrawPenaltyProposalTxn(PWRWallet validator, PWRJ pwrj) {
        try {
            Random random = new Random();
            long earlyWithdrawTime = 5 * 24 * 60 * 60; // 5 days
            int earlyWithdrawPenalty = random.nextInt(1000); // 10% x/10000

            String title = "Change early withdraw penalty proposal tile";

            Response r = validator.createProposal_ChangeEarlyWithdrawalPenalty(earlyWithdrawTime, earlyWithdrawPenalty, title, "Change early withdrawal penalty", validator.getNonce());
            errorIf(!r.isSuccess(), r.getError());

            Thread.sleep(5000);

            r = validator.voteOnProposal(r.getTransactionHash(), (byte) 0, validator.getNonce());
            errorIf(!r.isSuccess(), r.getError());

            Thread.sleep(5000);

            EarlyWithdrawPenaltyResponse response = pwrj.getEarlyWithdrawPenalty(earlyWithdrawTime);

            errorIf(!response.isEarlyWithdrawAvailable(), "Failed to change early withdrawal penalty. Early withdrawal not available");
            errorIf(response.getPenalty() != earlyWithdrawPenalty, "Failed to change early withdrawal penalty. Expected: " + earlyWithdrawPenalty + ", Actual: " + response.getPenalty());

            System.out.println("Early withdrawal penalty changed successfully");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to change early withdrawal penalty");
        }
    }

    private static void testChangeMaxBlockSizeProposalTxn(PWRWallet validator, PWRJ pwrj) {
        try {
            int currentMaxBlockSize = pwrj.getMaxBlockSize();
            int newMaxBlockSize = currentMaxBlockSize + 100000; // add 100KB

            String title = "Change max block size proposal title";

            Response r = validator.createProposal_ChangeMaxBlockSize(newMaxBlockSize, title, "Change max block size", validator.getNonce());
            errorIf(!r.isSuccess(), r.getError());

            Thread.sleep(5000);

            r = validator.voteOnProposal(r.getTransactionHash(), (byte) 0, validator.getNonce());
            errorIf(!r.isSuccess(), r.getError());

            Thread.sleep(5000);

            errorIf(pwrj.getMaxBlockSize() != newMaxBlockSize, "Failed to change max block size");

            System.out.println("Max block size changed successfully");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to change max block size");
        }
    }

    private static void testChangeMaxTxnSizeProposalTxn(PWRWallet validator, PWRJ pwrj) {
        try {
            int currentMaxTxnSize = pwrj.getMaxTransactionSize();
            int newMaxTxnSize = currentMaxTxnSize + 10000; // add 10KB

            String title = "Change max transaction size title";

            Response r = validator.createProposal_ChangeMaxTxnSizeSize(newMaxTxnSize, title, "Change max transaction size", validator.getNonce());
            errorIf(!r.isSuccess(), r.getError());

            Thread.sleep(5000);

            r = validator.voteOnProposal(r.getTransactionHash(), (byte) 0, validator.getNonce());
            errorIf(!r.isSuccess(), r.getError());

            Thread.sleep(5000);

            errorIf(pwrj.getMaxTransactionSize() != newMaxTxnSize, "Failed to change max transaction size");

            System.out.println("Max transaction size changed successfully");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to change max transaction size");
        }
    }

    private static void testChangeOverallBurnPercentageProposalTxn(PWRWallet validator, PWRJ pwrj) {
        try {
            int currentBurnPercentage = pwrj.getBurnPercentage();
            int newBurnPercentage = currentBurnPercentage + 1; // add 1%

            String title = "Change overall burn proposal title";

            Response r = validator.createProposal_ChangeOverallBurnPercentage(newBurnPercentage, title, "Change overall burn percentage", validator.getNonce());
            errorIf(!r.isSuccess(), r.getError());

            Thread.sleep(5000);

            r = validator.voteOnProposal(r.getTransactionHash(), (byte) 0, validator.getNonce());
            errorIf(!r.isSuccess(), r.getError());

            Thread.sleep(5000);

            errorIf(pwrj.getBurnPercentage() != newBurnPercentage, "Failed to change overall burn percentage");

            System.out.println("Overall burn percentage changed successfully");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to change overall burn percentage");
        }
    }

    private static void testChangeRewardPerYearProposalTxn(PWRWallet validator, PWRJ pwrj) {
        try {
            long currentRewardPerYear = pwrj.getPwrRewardsPerYear();
            long newRewardPerYear = currentRewardPerYear + 1; // add 1 PWR

            String title = "Change reward per year proposal title";

            Response r = validator.createProposal_ChangeRewardPerYear(newRewardPerYear, title, "Change reward per year", validator.getNonce());
            errorIf(!r.isSuccess(), r.getError());

            Thread.sleep(5000);

            r = validator.voteOnProposal(r.getTransactionHash(), (byte) 0, validator.getNonce());
            errorIf(!r.isSuccess(), r.getError());

            Thread.sleep(5000);

            errorIf(pwrj.getPwrRewardsPerYear() != newRewardPerYear, "Failed to change reward per year. Expected: " + newRewardPerYear + ", Actual: " + pwrj.getPwrRewardsPerYear());

            System.out.println("Reward per year changed successfully");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to change reward per year");
        }
    }

    private static void testChangeValidatorCountLimitProposalTxn(PWRWallet validator, PWRJ pwrj) {
        try {
            int currentValidatorCountLimit = pwrj.getValidatorCountLimit();
            int newValidatorCountLimit = currentValidatorCountLimit + 10; // add 10 validators

            String title = "Change validator count limit proposal title";

            Response r = validator.createProposal_ChangeValidatorCountLimit(newValidatorCountLimit, title, "Change validator count limit", validator.getNonce());
            errorIf(!r.isSuccess(), r.getError());

            Thread.sleep(5000);

            r = validator.voteOnProposal(r.getTransactionHash(), (byte) 0, validator.getNonce());
            errorIf(!r.isSuccess(), r.getError());

            Thread.sleep(5000);

            errorIf(pwrj.getValidatorCountLimit() != newValidatorCountLimit, "Failed to change validator count limit");

            System.out.println("Validator count limit changed successfully");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to change validator count limit");
        }
    }

    private static void testChangeValidatorJoiningFeeProposalTxn(PWRWallet validator, PWRJ pwrj) {
        try {
            long currentJoiningFee = pwrj.getValidatorJoiningFee();
            long newJoiningFee = currentJoiningFee + 1000000000; // add 1 PWR

            String title = "Change validator joining fee proposal title";

            Response r = validator.createProposal_ChangeValidatorJoiningFee(newJoiningFee, title, "Change validator joining fee", validator.getNonce());
            errorIf(!r.isSuccess(), r.getError());

            Thread.sleep(5000);

            r = validator.voteOnProposal(r.getTransactionHash(), (byte) 0, validator.getNonce());
            errorIf(!r.isSuccess(), r.getError());

            Thread.sleep(5000);

            errorIf(pwrj.getValidatorJoiningFee() != newJoiningFee, "Failed to change validator joining fee");

            System.out.println("Validator joining fee changed successfully");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to change validator joining fee");
        }
    }

    private static void testChangeVmIdClaimingFeeProposalTxn(PWRWallet validator, PWRJ pwrj) {
        try {
            long currentClaimingFee = pwrj.getVmIdClaimingFee();
            long newClaimingFee = currentClaimingFee + 1000000; // add 0.01 PWR

            String title = "Change VM ID Claiming Fee Proposal Title";

            Response r = validator.createProposal_ChangeVmIdClaimingFee(newClaimingFee, title, "Change VM ID claiming fee", validator.getNonce());
            errorIf(!r.isSuccess(), r.getError());

            Thread.sleep(5000);

            r = validator.voteOnProposal(r.getTransactionHash(), (byte) 0, validator.getNonce());
            errorIf(!r.isSuccess(), r.getError());

            Thread.sleep(5000);

            errorIf(pwrj.getVmIdClaimingFee() != newClaimingFee, "Failed to change VM ID claiming fee");

            System.out.println("VM ID claiming fee changed successfully");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to change VM ID claiming fee");
        }
    }

    private static void testChangeVmOwnerTxnFeeShareProposalTxn(PWRWallet validator, PWRJ pwrj) {
        try {
            int currentFeeShare = pwrj.getVmOwnerTransactionFeeShare();
            int newFeeShare = currentFeeShare + 100; // add 1%

            String title = "Change VM owner transfer fee share title";

            Response r = validator.createProposal_ChangeVmOwnerTxnFeeShare(newFeeShare, title, "Change VM owner transaction fee share", validator.getNonce());
            errorIf(!r.isSuccess(), r.getError());

            Thread.sleep(5000);

            r = validator.voteOnProposal(r.getTransactionHash(), (byte) 0, validator.getNonce());
            errorIf(!r.isSuccess(), r.getError());

            Thread.sleep(5000);

            errorIf(pwrj.getVmOwnerTransactionFeeShare() != newFeeShare, "Failed to change VM owner transaction fee share");

            System.out.println("VM owner transaction fee share changed successfully");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to change VM owner transaction fee share");
        }
    }

    private static void testOtherProposalTxn(PWRWallet validator, PWRJ pwrj) {
        try {
            String title = "Other proposal title";
            String description = "Other proposal description";

            Response r = validator.createProposal_OtherProposal(title, description, validator.getNonce());
            errorIf(!r.isSuccess(), r.getError());

            Thread.sleep(5000);

            r = validator.voteOnProposal(r.getTransactionHash(), (byte) 0, validator.getNonce());
            errorIf(!r.isSuccess(), r.getError());

            System.out.println("Other proposal created and voted on successfully");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to create or vote on other proposal");
        }
    }

    //Used to make sure PWRJ is properly reading the new txn types from the RPC node
    private static void testFetchTransactions(long startingBlockNumber, long endingBlockNumber, PWRJ pwrj) {
        for(long i = startingBlockNumber; i <= endingBlockNumber; i++) {
            try {
                for(Transaction transaction: pwrj.getBlockByNumber(i).getTransactions()) {
                    System.out.println(transaction.toJSON().toString());
                    System.out.println();
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Failed to fetch transactions from block " + i);
            }
        }

    }

}
