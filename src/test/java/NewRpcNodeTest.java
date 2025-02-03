import com.github.pwrlabs.pwrj.protocol.PWRJ;
import com.github.pwrlabs.pwrj.record.transaction.ecdsa.PayableVmDataTransaction;
import com.github.pwrlabs.pwrj.record.transaction.Interface.Transaction;
import com.github.pwrlabs.pwrj.record.transaction.ecdsa.VmDataTransaction;

public class NewRpcNodeTest {

    public static void main(String[] args) throws Exception {
        PWRJ pwrj = new PWRJ("http://147.182.172.216:8085/");

//        Block block = pwrj.getBlockByNumber(50);
//        System.out.println(block.getHash());
//        System.out.println(block.getNumber());
//        System.out.println(block.getTransactions().length);
//        System.out.println("submitter: " + block.getSubmitter());
//        System.out.println(block.getSize());
//        System.out.println(block.getReward());
//        System.out.println(block.getTimestamp());
//        System.out.println(block.isProcessedWithoutCriticalErrors());
//
//        System.out.println(block.getTransactionCount());

//        for(Transaction txn: block.getTransactions()) {
//            System.out.println(txn.getHash());
//        }
//
        Transaction tx = pwrj.getTransactionByHash("0x74867c60302d6a97dd73b3289a7ebbac31571253e2ae66b2491bc412aef15dc0");
        String data = null;
        if(tx instanceof VmDataTransaction) {
            data = ((VmDataTransaction) tx).getData();
        } else if(tx instanceof PayableVmDataTransaction) {
            data = ((PayableVmDataTransaction) tx).getData();
        }

        System.out.println(tx.getHash());
        System.out.println(tx.toJSON());
//
//        VmDataTransaction[] txns = pwrj.getVMDataTransactions(1, 100, 69);
//        for (VmDataTransaction txn : txns) {
//            System.out.println(txn.getHash());
//            System.out.println(txn.toJSON());
//        }

    }

}
