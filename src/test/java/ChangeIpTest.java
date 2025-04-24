//import com.github.pwrlabs.pwrj.protocol.PWRJ;
//import com.github.pwrlabs.pwrj.record.response.Response;
//
//import java.io.IOException;
//
//import static com.github.pwrlabs.pwrj.Utils.NewError.errorIf;
//
//public class ChangeIpTest {
//    private static final PWRJ pwrj = new PWRJ("http://localhost:8085");
//
//    public static void main(String[] args) {
//        changeIpTest();
//    }
//
//    private static void giveTokensToValidator(String validator) throws IOException {
//        if(!validator.startsWith("0x")) validator = "0x" + validator;
//
//        pwrj.httpGet("http://localhost:8085/giveTokensToValidatorNode?validatorAddress=" + validator);
//    }
//
//    private static String generateRandomIp() {
//        String randomIp = "";
//        for (int i = 0; i < 4; i++) {
//            randomIp += (int) (Math.random() * 255) + ".";
//        }
//        randomIp = randomIp.substring(0, randomIp.length() - 1);
//
//        return randomIp;
//    }
//    public static void changeIpTest() {
//        try {
//            PWRFalconWallet wallet = new PWRFalconWallet(12, pwrj);
//            String randomIp1 = generateRandomIp();
//            String randomIp2 = generateRandomIp();
//
//            String validator = wallet.getAddress();
//            giveTokensToValidator(validator);
//            Thread.sleep(3000);
//
//            long balance = wallet.getBalance();
//            errorIf(balance == 0, "Balance is 0");
//
//            Response res = wallet.join(randomIp1, wallet.getNonce());
//            errorIf(!res.isSuccess(), res.getError());
//            Thread.sleep(3000);
//
//            String ip = pwrj.getValidator(validator).getIp();
//            errorIf(!ip.equals(randomIp1), "Failed to set validator ip");
//            System.out.println("Successfully set validator ip to: " + ip);
//
//            Response res2 = wallet.changeIp(randomIp2, wallet.getNonce());
//            errorIf(!res2.isSuccess(), res2.getError());
//            Thread.sleep(3000);
//
//            String ip2 = pwrj.getValidator(validator).getIp();
//            errorIf(!ip2.equals(randomIp2), "Failed to change validator ip");
//            System.out.println("Successfully changed validator ip to: " + ip2);
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.err.println("Failed to change validator ip");
//        }
//    }
//}
