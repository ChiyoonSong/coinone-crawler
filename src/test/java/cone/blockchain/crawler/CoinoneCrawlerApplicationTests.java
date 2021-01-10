package cone.blockchain.crawler;

import com.fasterxml.jackson.databind.ObjectMapper;
import cone.blockchain.crawler.biz.service.TransactionService;
import cone.blockchain.crawler.lib.tronprotocol.common.utils.Base58;
import cone.blockchain.crawler.lib.tronprotocol.common.utils.Sha256Hash;
import cone.blockchain.crawler.models.common.AccountInfo;
import cone.blockchain.crawler.service.CommonService;
import cone.blockchain.crawler.service.trx.TrxScan;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@SpringBootTest
class CoinoneCrawlerApplicationTests {
    private static final Logger logger = LogManager.getLogger(CoinoneCrawlerApplicationTests.class);
    @Autowired
    private WebClient customWebClinet;
    @Autowired
    private TrxScan trxScan;
    @Autowired
    private CommonService commonService;
    @Autowired
    private TransactionService transactionService;

    @Test
    void contextLoads() {
    }

    @Test
    @Transactional
    public void webClinet() throws Exception {

        HashMap<String, String> accountMap = new HashMap<>();
        accountMap.put("symbol", "trx");
        accountMap.put("complete", "N");
        List<AccountInfo> accountList = transactionService.getAccountInfo(accountMap);
        AccountInfo accountInfo = accountList.get(0);

        Mono<String> result = customWebClinet.mutate()
//                .baseUrl("https://api.trongrid.io/v1/accounts/" + accountInfo.getAddress() + "/transactions?min_timestamp=1577664000000&max_timestamp=1605877752000").build()
//                .baseUrl("https://api.trongrid.io:443/v1/accounts/TDoyjmPJHzRFmYfCRLRsPhKjLETwd9fKr9/transactions?max_timestamp=1605877752000&min_timestamp=1577664000000&fingerprint=DziomHhU9FH9N2sNmrJLtNxZyS7XWBRDJqfy7rxPQmRoYBQx5UGt792Qjn8F5akgunfx1b2R7BMfJrwhkLc1Pt5CeCigAPpAoc3Fb").build()
                .baseUrl("https://api.trongrid.io/v1/accounts/" +
                        accountInfo.getAddress() +
                        "/transactions" +
                        "?min_timestamp=" + accountInfo.getStartBlock() +
                        "&max_timestamp=" + accountInfo.getEndBlock()).build()
                .get()
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono(clientResponse -> {
                   return clientResponse.bodyToMono(String.class);
                });
                result.subscribe(response -> {
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, Object> convertDataMap = new HashMap<>();
                    try {
                        convertDataMap = objectMapper.readValue(response, Map.class);
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new IllegalArgumentException("trx response conver error");
                    }

                    HashMap<String, String> accountInfoMap = new HashMap<>();
                    accountInfoMap.put("tableSymbol", "trx");
                    accountInfoMap.put("address", accountInfo.getAddress());

                    if ("Exceeds the maximum limit, please change your query time range".equals(convertDataMap.get("error"))) {
                        HashMap<String, String> lastTx = transactionService.getTransactionInfoOne(accountInfoMap);
                        String lastBlockDate = lastTx.get("created_at");
                        Long blockTime = commonService.getUnixTimeStampFromDate(lastBlockDate);

                        accountInfoMap.put("end_block", blockTime.toString());
                        accountInfoMap.put("memo", "");
                        accountInfoMap.put("tag", "");

                        transactionService.putAccountStatus(accountInfoMap);

                    }
                });

        Thread.sleep(10000);
    }

    /**
     * parameter: {
     * value: {
     * data: "a9059cbb00000000000000000000000059037e73927ad5ba52b27c8c21947b3186ed70950000000000000000000000000000000000000000000002d9ebe14384a9d2d000",
     * owner_address: "412a21b0018f7e74bea56efc04eb907911d55ed068",
     * contract_address: "4118fd0626daf3af02389aef3ed87db9c33f638ffa"
     * },
     * type_url: "type.googleapis.com/protocol.TriggerSmartContract"
     * },
     * type: "TriggerSmartContract"
     * }
     */
    @Test
    public void getTrcTokenInfo() {
//        String data = "a9059cbb00000000000000000000000059037e73927ad5ba52b27c8c21947b3186ed70950000000000000000000000000000000000000000000002d9ebe14384a9d2d000";
        String data = "23b872dd0000000000000000000000002a21b0018f7e74bea56efc04eb907911d55ed0680000000000000000000000007f65532897c052a0af0c3de41ac77f9c7a6fee310000000000000000000000000000000000000000000000000e2da2a751e26400";

        HashMap<String, String> inputData = new HashMap<>();
        ;
//        commonService.getTrc20InputData(data, inputData);
        trxScan.getTrxtokenInfo(inputData, data);
        System.out.println(inputData.toString());

        System.out.println(commonService.getHexaToDecimal(inputData.get("amount")));
        System.out.println(trxScan.hexStringTobBase58(inputData.get("address")));
    }

    @Test
    public void getTrxtokenInfoTest() {
        /**
         * input data string
         * a9059cbb00000000000000000000000059037e73927ad5ba52b27c8c21947b3186ed70950000000000000000000000000000000000000000000002d9ebe14384a9d2d000
         */
        HashMap<String, String> inputData = new HashMap<>();
        String inputDataStr = "23b872dd0000000000000000000000002a21b0018f7e74bea56efc04eb907911d55ed0680000000000000000000000007f65532897c052a0af0c3de41ac77f9c7a6fee310000000000000000000000000000000000000000000000000e2da2a751e26400";
        if (inputDataStr.startsWith("a9059cbb")) {
            inputData.put("method", inputDataStr.substring(0, 8));
            inputData.put("address", "41" + inputDataStr.substring(32, 72));
            inputData.put("amount", "0x" + inputDataStr.substring(72, 136));
        } else if (inputDataStr.startsWith("23b872dd")) {
            inputData.put("from_address", "41" + inputDataStr.substring(32, 72)); // from
            inputData.put("to_address", "41" + inputDataStr.substring(96, 136)); // to
            inputData.put("amount", "0x" + inputDataStr.substring(136));
        }

        System.out.println("amount : " + commonService.getHexaToDecimal(inputData.get("amount")));
        System.out.println("fromAddess : " + trxScan.hexStringTobBase58(inputData.get("from_address")));
        System.out.println("toAddess : " + trxScan.hexStringTobBase58(inputData.get("to_address")));
    }

    @Test
    public void unixSecondsToDate() {
        long unixSeconds = 1605877752L;
        // convert seconds to milliseconds
        Date date = new java.util.Date(unixSeconds * 1000L);
        // the format of your date
        SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        // give a timezone reference for formatting (see comment at the bottom)
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        String formattedDate = sdf.format(date);
        System.out.println(formattedDate);
    }

    @Test
    public void getUnixTimeStampFromDate() throws Exception {
//		Date date = new Date();
//		System.out.print(date.getTime());

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        String dateString = "2020-10-22 14:09:21"; // 1609977600000
        Date date = dateFormat.parse(dateString);
        System.out.print(date.getTime());

    }


    @Test
    public void hexStringTobBase58() {
//        String hexString = "412a21b0018f7e74bea56efc04eb907911d55ed068";
        String hexString = "4159037e73927ad5ba52b27c8c21947b3186ed7095";
        hexString = adjustHex(hexString);
        byte[] decodedHex = hexString == null ? new byte[0] : org.spongycastle.util.encoders.Hex.decode(hexString);
        String base58 = encode58(decodedHex);
        System.out.print(base58);
    }

    public String adjustHex(String hexString) {
        if (hexString.startsWith("0x")) {
            hexString = "41" + hexString.substring(2);
        }
        if (hexString.length() % 2 == 1) {
            hexString = "0" + hexString;
        }
        return hexString;
    }


    public String encode58(byte[] input) {
        byte[] hash0 = Sha256Hash.hash(false, input);
        byte[] hash1 = Sha256Hash.hash(false, hash0);
        byte[] inputCheck = new byte[input.length + 4];
        System.arraycopy(input, 0, inputCheck, 0, input.length);
        System.arraycopy(hash1, 0, inputCheck, input.length, 4);
        return Base58.encode(inputCheck);
    }

}
