package cone.blockchain.crawler.service.trx;

import com.fasterxml.jackson.databind.ObjectMapper;
import cone.blockchain.crawler.biz.service.TransactionService;
import cone.blockchain.crawler.lib.tronprotocol.common.utils.Base58;
import cone.blockchain.crawler.lib.tronprotocol.common.utils.Sha256Hash;
import cone.blockchain.crawler.models.common.AccountInfo;
import cone.blockchain.crawler.models.common.CsvModel;
import cone.blockchain.crawler.service.CommonService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class TrxScan {
    private static final Logger logger = LogManager.getLogger(TrxScan.class);

    private final WebClient customWebClinet;

    private final TransactionService transactionService;

    private final CommonService commonService;

    /**
     * https://developers.tron.network/reference#account-info-by-address
     * Get transaction info by account address
     * 2019-12-01 00:00:00 UTC (1575158400000)
     *
     * 2019-12-30 00:00:00 UTC (1577664000000)
     *  ~  2021-01-07 00:00:00 UTC (1609977600000)
     *
     * https://api.trongrid.io/v1/accounts/TDoyjmPJHzRFmYfCRLRsPhKjLETwd9fKr9/transactions?min_timestamp=1577664000000&max_timestamp=1609977600000
     * @param accountInfo
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void getTransactions(AccountInfo accountInfo) {
        String baseUrl = "https://api.trongrid.io/v1/accounts/" +
                accountInfo.getAddress() + "/transactions" +
                "?min_timestamp=" + accountInfo.getStartBlock() +
                "&max_timestamp=" + accountInfo.getEndBlock();

        if (Strings.isNotBlank(accountInfo.getMemo())) {
            baseUrl = accountInfo.getMemo();
        }
        Mono<String> result = customWebClinet.mutate() /* 기존 설정값 상속하여 사용 */
                .baseUrl(baseUrl).build()
                .get()
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono(clientResponse -> {
                    return clientResponse.bodyToMono(String.class);
                });

        logger.info("baseUrl : {}", baseUrl);
        result.subscribe(response -> {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> convertDataMap;
            try {
                convertDataMap = objectMapper.readValue(response, Map.class);
            } catch (Exception e) {
                e.printStackTrace();
                throw new IllegalArgumentException("trx response  error");
            }
            HashMap<String, String> accountInfoMap = new HashMap<>();
            accountInfoMap.put("tableSymbol", "trx");
            accountInfoMap.put("address", accountInfo.getAddress());

            if ("Exceeds the maximum limit, please change your query time range".equals(convertDataMap.get("error"))) {
                HashMap<String, String> lastTx = transactionService.getTransactionInfoOne(accountInfoMap);
                String lastBlockDate = lastTx.get("created_at");
                Long blockTime = commonService.getUnixTimeStampFromDate(lastBlockDate);
                if(blockTime.compareTo(0L) == 0){
                    throw new IllegalArgumentException("date parse error");
                }
                accountInfoMap.put("end_block", blockTime.toString());
                accountInfoMap.put("memo", "");
                accountInfoMap.put("tag", "");

                transactionService.putAccountStatus(accountInfoMap);
            } else if ("false".equals(convertDataMap.get("success"))) {
                throw new IllegalArgumentException("response error : " + convertDataMap.get("error").toString());
            } else{

                inputData(convertDataMap, accountInfo);

                LinkedHashMap<String, Object> links = (LinkedHashMap<String, Object>) ((LinkedHashMap<String, Object>) convertDataMap.get("meta")).get("links");
                if (null != links) {
                    accountInfoMap.put("memo", links.get("next").toString());
                    accountInfoMap.put("tag", accountInfo.getMemo());
                } else {
                    logger.info("complete set : next page is null : {}", accountInfo.getAddress());
                    accountInfoMap.put("complete", "Y");
                }
                transactionService.putAccountStatus(accountInfoMap);
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void inputData(Map<String, Object> convertDataMap, AccountInfo accountInfo) {
        HashMap<String, String> accountInfoMap = new HashMap<>();
        accountInfoMap.put("tableSymbol", "trx");
        accountInfoMap.put("address", accountInfo.getAddress());

        HashMap<String, String> map = new HashMap<>();
        map.put("tableSymbol", "trx");
        map.put("address", accountInfo.getAddress());
        HashMap<String, String> lastTxInfo = transactionService.getTransactionInfoOne(map);

        ArrayList<LinkedHashMap<String, Object>> dataList = ((ArrayList<LinkedHashMap<String, Object>>) convertDataMap.get("data"));
        if (dataList.isEmpty()) {
            logger.info("complete set : transaction data is null : {}", accountInfo.getAddress());
            accountInfoMap.put("complete", "Y");
            transactionService.putAccountStatus(accountInfoMap);
        } else {
            for (LinkedHashMap<String, Object> dataMap : dataList) {
                LinkedHashMap<String, Object> internalData = ((LinkedHashMap<String, Object>) dataMap.get("data"));
                LinkedHashMap<String, Object> rawData = ((LinkedHashMap<String, Object>) dataMap.get("raw_data"));

                // 트랜잭션이 이미 수집이 된 경우,
                if (null != lastTxInfo &&
                        (lastTxInfo.get("hash").equals(String.valueOf(dataMap.get("txID")))
                                || lastTxInfo.get("hash").equals(String.valueOf(dataMap.get("tx_id"))))) {
                    logger.warn("complete set : duplicate tx existence : {}", accountInfo.getAddress());
                    logger.warn("[{}]", dataMap.toString());
                   /* accountInfoMap.put("complete", "Y");
                    transactionService.putAccountStatus(accountInfoMap);

                    return;*/
                }

                if (null == rawData && null == internalData) {
                    logger.warn("{} : get raw_data is null :: {}", accountInfo.getAddress(), dataMap.toString());
                    continue;
                }
                if (null != rawData) {
                    for (Object contract : ((ArrayList) rawData.get("contract"))) {
                        CsvModel csvModel = new CsvModel();
                        csvModel.setTableSymbol("trx");

                        ArrayList retList = (ArrayList) dataMap.get("ret");
                        // 수수료 정보가 1개 이상인 경우.. 확인!!
                        if (retList.size() > 1) {
                            throw new IllegalArgumentException("ret fee data size over : " + dataMap.get("txID").toString());
                        } else {
                            LinkedHashMap<String, Object> retMap = (LinkedHashMap<String, Object>) retList.get(0);
                            if (null == retMap.get("contractRet") || "SUCCESS".equals(retMap.get("contractRet").toString())) {
                                csvModel.setIsValid(1);
                            } else {
                                csvModel.setIsValid(0);
                            }
                            csvModel.setFee(new BigDecimal(retMap.get("fee").toString())/*.multiply(BigDecimal.valueOf(Math.pow(10, 18)))*/);
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

                        csvModel.setFrom(hexStringTobBase58(value.get("owner_address").toString()));
                        if (null != value.get("contract_address")) {
                            csvModel.setContract(hexStringTobBase58(value.get("contract_address").toString()));
                        }
                        if (null != value.get("data")) {
                            HashMap<String, String> inputData = new HashMap<>();
                            getTrxtokenInfo(inputData, value.get("data").toString());

                            csvModel.setAmount(new BigDecimal(commonService.getHexaToDecimal(inputData.get("amount"))));
                            csvModel.setTo(hexStringTobBase58(inputData.get("address")));
                        } else {
                            BigDecimal amount = new BigDecimal(value.get("amount").toString());
                            csvModel.setAmount(amount/*.multiply(BigDecimal.valueOf(Math.pow(10, 18)))*/);
                            csvModel.setTo(hexStringTobBase58(value.get("to_address").toString()));
                        }

                        if (accountInfo.getAddress().equals(csvModel.getFrom())) {
                            csvModel.setIsDeposit(0);
                        } else {
                            csvModel.setIsDeposit(1);
                        }
                        transactionService.putBlockTransaction(csvModel);
                    }
                }
                if (null != internalData) {

                    CsvModel csvModel = new CsvModel();
                    csvModel.setTableSymbol("trx");
                    csvModel.setSymbol("TRX");
                    csvModel.setAddr(accountInfo.getAddress());
                    csvModel.setHash(dataMap.get("tx_id").toString());
                    csvModel.setFrom(hexStringTobBase58(dataMap.get("from_address").toString()));
                    csvModel.setTo(hexStringTobBase58(dataMap.get("to_address").toString()));
                    // block 데이터가 없음..
                    csvModel.setBlock(0L);

                    String date = commonService.getEpochTimeToDate(Long.valueOf(dataMap.get("block_timestamp").toString()) / 1000);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    csvModel.setCreated_at(LocalDateTime.parse(date, formatter));

                    csvModel.setAmount(new BigDecimal(String.valueOf(((LinkedHashMap<String, Object>) internalData.get("call_value")).get("_"))));
                    if ("false".equals(String.valueOf(internalData.get("rejected")))) {
                        csvModel.setIsValid(1);
                    } else {
                        csvModel.setIsValid(0);
                    }
                    if (accountInfo.getAddress().equals(csvModel.getFrom())) {
                        csvModel.setIsDeposit(0);
                    } else {
                        csvModel.setIsDeposit(1);
                    }
                    csvModel.setFee(BigDecimal.ZERO);
                    transactionService.putBlockTransaction(csvModel);
                }

            }

        }
    }

    public String hexStringTobBase58(String hexString) {
        hexString = adjustHex(hexString);
        byte[] decodedHex = hexString == null ? new byte[0] : org.spongycastle.util.encoders.Hex.decode(hexString);
        String base58 = encode58(decodedHex);
        return base58;
    }

    private String adjustHex(String hexString) {
        if (hexString.startsWith("0x")) {
            hexString = "41" + hexString.substring(2);
        }
        if (hexString.length() % 2 == 1) {
            hexString = "0" + hexString;
        }
        return hexString;
    }

    private String encode58(byte[] input) {
        byte[] hash0 = Sha256Hash.hash(true, input);
        byte[] hash1 = Sha256Hash.hash(true, hash0);
        byte[] inputCheck = new byte[input.length + 4];
        System.arraycopy(input, 0, inputCheck, 0, input.length);
        System.arraycopy(hash1, 0, inputCheck, input.length, 4);
        return Base58.encode(inputCheck);
    }

    public void getTrxtokenInfo(HashMap<String, String> inputData, String inputDataStr) {
        /**
         * input data string
         * [transfer]
         * a9059cbb00000000000000000000000059037e73927ad5ba52b27c8c21947b3186ed70950000000000000000000000000000000000000000000002d9ebe14384a9d2d000
         * [transferFrom(address src,address dst,uint256 wad)]
         * 23b872dd0000000000000000000000002a21b0018f7e74bea56efc04eb907911d55ed0680000000000000000000000007f65532897c052a0af0c3de41ac77f9c7a6fee310000000000000000000000000000000000000000000000000e2da2a751e26400
         */
        if (inputDataStr.startsWith("a9059cbb")) {
            inputData.put("method", inputDataStr.substring(0, 8));
            inputData.put("address", "41" + inputDataStr.substring(32, 72));
            inputData.put("amount", "0x" + inputDataStr.substring(72, 136));
        } else if (inputDataStr.startsWith("23b872dd")) {
//            inputData.put("address", "41" + inputDataStr.substring(32, 72)); // from
            inputData.put("address", "41" + inputDataStr.substring(96, 136)); // to
            inputData.put("amount", "0x" + inputDataStr.substring(136));
        }


    }
}
