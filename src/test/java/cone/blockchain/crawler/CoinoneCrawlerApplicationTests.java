package cone.blockchain.crawler;

import com.fasterxml.jackson.databind.ObjectMapper;
import cone.blockchain.crawler.biz.service.TransactionService;
import cone.blockchain.crawler.lib.tronprotocol.common.utils.Base58;
import cone.blockchain.crawler.lib.tronprotocol.common.utils.Sha256Hash;
import cone.blockchain.crawler.models.common.AccountInfo;
import cone.blockchain.crawler.models.common.CsvModel;
import cone.blockchain.crawler.service.CommonService;
import cone.blockchain.crawler.service.trx.TrxScan;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@SpringBootTest
class CoinoneCrawlerApplicationTests {

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
        String address = "TA1WRiBiqtmtnimWQq2bkJHkse1L34YbAH";
        customWebClinet.mutate() /* 기존 설정값 상속하여 사용 */
//                .baseUrl("https://api.trongrid.io/v1/accounts/" + address + "/transactions?min_timestamp=1546095600000").build()
                .baseUrl("https://api.trongrid.io:443/v1/accounts/TDoyjmPJHzRFmYfCRLRsPhKjLETwd9fKr9/transactions?min_timestamp=1546095600000&fingerprint=9mPzvhwvPW19bGPv1ETB4mjUWw7qZe9zUayLnVUUrmNCfSrHdBnGQoV14tdgGQNXZmLLTvZixyrEQbF5LWxrA2yP7fdxhssKK").build()
                .get()
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError()
                        , clientResponse ->
                                clientResponse.bodyToMono(String.class)
                                        .map(body -> new RuntimeException(body)))
                .bodyToMono(String.class)
                .subscribe(response -> {

                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, Object> convertDataMap = new HashMap<>();
                    try {
                        convertDataMap = objectMapper.readValue(response, Map.class);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    AccountInfo accountInfo = new AccountInfo();
                    accountInfo.setAddress(address);

                    HashMap<String, String> accountInfoMap = new HashMap<>();
                    accountInfoMap.put("tableSymbol", "trx");
                    accountInfoMap.put("address", accountInfo.getAddress());

                    ArrayList<LinkedHashMap<String, Object>> dataList = ((ArrayList<LinkedHashMap<String, Object>>) convertDataMap.get("data"));
                    if (dataList.isEmpty()) {
                        accountInfoMap.put("complete", "Y");
                        transactionService.putAccountStatus(accountInfoMap);
                    } else {
                        for (LinkedHashMap<String, Object> dataMap : dataList) {
                            // 트랜잭션이 이미 수집이 된 경우,
                            /*if (null != lastTxInfo && lastTxInfo.get("hash").equals(dataMap.get("txID").toString())) {
                                accountInfoMap.put("complete", "Y");
                                transactionService.putAccountStatus(accountInfoMap);

                                return;
                            }*/

                            LinkedHashMap<String, Object> rawData = ((LinkedHashMap<String, Object>) dataMap.get("raw_data"));
                            for (Object contract : ((ArrayList) rawData.get("contract"))) {
                                CsvModel csvModel = new CsvModel();
                                csvModel.setTableSymbol("trx");

                                ArrayList retList = (ArrayList) dataMap.get("ret");
                                // 수수료 정보가 1개 이상인 경우.. 확인!!
                                if (retList.size() > 1) {
                                    throw new IllegalArgumentException("ret fee data size over : " + dataMap.get("txID").toString());
                                } else {
                                    LinkedHashMap<String, Object> retMap = (LinkedHashMap<String, Object>) retList.get(0);
                                    if ("SUCCESS".equals(retMap.get("contractRet").toString())) {
                                        csvModel.setIsValid(1);
                                    } else {
                                        csvModel.setIsValid(0);
                                    }
                                    csvModel.setFee(new BigDecimal(retMap.get("fee").toString()).multiply(BigDecimal.valueOf(Math.pow(10, 18))));
                                }

                                csvModel.setBlock(Long.valueOf(dataMap.get("blockNumber").toString()));
                                csvModel.setHash(dataMap.get("txID").toString());

                                String date = commonService.getEpochTimeToDate(Long.valueOf(dataMap.get("block_timestamp").toString()) / 1000);
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                                csvModel.setCreated_at(LocalDateTime.parse(date, formatter));

                                String type = ((LinkedHashMap<String, Object>) contract).get("type").toString();
                                csvModel.setType(type);

                                LinkedHashMap<String, Object> param = (LinkedHashMap<String, Object>) ((LinkedHashMap<String, Object>) contract).get("parameter");
                                LinkedHashMap<String, Object> value = (LinkedHashMap<String, Object>) param.get("value");

                                String symbol = "TRX";
                                if (null != value.get("asset_name")) {
                                    symbol = value.get("asset_name").toString();
                                }
                                csvModel.setSymbol(symbol);
                                csvModel.setAddr(accountInfo.getAddress());

                                csvModel.setFrom(trxScan.hexStringTobBase58(value.get("owner_address").toString()));
                                if (null != value.get("contract_address")) {
                                    csvModel.setContract(trxScan.hexStringTobBase58(value.get("contract_address").toString()));
                                }
                                if (null != value.get("data")) {
                                    HashMap<String, String> inputData = new HashMap<>();
                                    trxScan.getTrxtokenInfo(inputData, value.get("data").toString());

                                    csvModel.setAmount(new BigDecimal(commonService.getHexaToDecimal(inputData.get("amount"))));
                                    csvModel.setTo(trxScan.hexStringTobBase58(inputData.get("address")));
                                } else {
                                    BigDecimal amount = new BigDecimal(value.get("amount").toString());
                                    csvModel.setAmount(amount.multiply(BigDecimal.valueOf(Math.pow(10, 18))));
                                    csvModel.setTo(trxScan.hexStringTobBase58(value.get("to_address").toString()));
                                }

                                if (accountInfo.getAddress().equals(csvModel.getFrom())) {
                                    csvModel.setIsDeposit(0);
                                } else {
                                    csvModel.setIsDeposit(1);
                                }
                                transactionService.putBlockTransaction(csvModel);
                            }
                        }
                        LinkedHashMap<String, Object> links = (LinkedHashMap<String, Object>) ((LinkedHashMap<String, Object>) convertDataMap.get("meta")).get("links");
                        if (null != links) {
                            accountInfoMap.put("memo", links.get("next").toString());
                        } else {
                            accountInfoMap.put("complete", "Y");
                        }
                        transactionService.putAccountStatus(accountInfoMap);

                    }
                });
        Thread.sleep(5000);
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
        String data = "a9059cbb00000000000000000000000059037e73927ad5ba52b27c8c21947b3186ed70950000000000000000000000000000000000000000000002d9ebe14384a9d2d000";

        HashMap<String, String> inputData = new HashMap<>();
        ;
//        commonService.getTrc20InputData(data, inputData);
        trxScan.getTrxtokenInfo(inputData, data);
        System.out.println(inputData.toString());

        System.out.println(commonService.getHexaToDecimal(inputData.get("amount")));
        System.out.println(trxScan.hexStringTobBase58(inputData.get("address")));
    }

    @Test
    public void unixSecondsToDate() {
        long unixSeconds = 1609819876L;
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

        SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
        String dateString = "2019-12-31 00:00:00";
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
