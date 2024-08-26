import com.github.pwrlabs.pwrj.protocol.PWRJ;
import com.github.pwrlabs.pwrj.record.block.Block;
import com.github.pwrlabs.pwrj.record.transaction.Transaction;
import com.github.pwrlabs.pwrj.record.transaction.VmDataTransaction;

public class NewRpcNodeTest {

    public static void main(String[] args) throws Exception {
        PWRJ pwrj = new PWRJ("http://localhost:8085/");

        Block block = pwrj.getBlockByNumber(50);
        System.out.println(block.getHash());
        System.out.println(block.getNumber());
        System.out.println(block.getTransactions().length);
        System.out.println(block.getSubmitter());
        System.out.println(block.getSize());
        System.out.println(block.getReward());
        System.out.println(block.getTimestamp());
        System.out.println(block.isProcessedWithoutCriticalErrors());

        for(Transaction txn: block.getTransactions()) {
            System.out.println(txn.getHash());
        }

        Transaction tx = pwrj.getTransactionByHash("0x73426e5354137ae57b1114fc0e21e9cdf52e9939b1ccd4d810072ef5743ca8a2");
        System.out.println(tx.getHash());
        System.out.println(tx.toJSON());

        VmDataTransaction[] txns = pwrj.getVMDataTransactions(1, 100, 69);
        for (VmDataTransaction txn : txns) {
            System.out.println(txn.getHash());
            System.out.println(txn.toJSON());
        }

    }

}
