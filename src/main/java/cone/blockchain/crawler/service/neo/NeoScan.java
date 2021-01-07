package cone.blockchain.crawler.service.neo;

import cone.blockchain.crawler.biz.service.TransactionService;
import cone.blockchain.crawler.models.common.AccountInfo;
import cone.blockchain.crawler.models.common.CsvModel;
import cone.blockchain.crawler.models.neo.AddressTransactionsEntries;
import cone.blockchain.crawler.models.neo.AddressTransactionsSummary;
import cone.blockchain.crawler.service.CommonService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

@RequiredArgsConstructor
@Service
public class NeoScan {
    private static final Logger logger = LogManager.getLogger(NeoScan.class);

    private final WebClient customWebClinet;

    private final CommonService commonService;

    private final TransactionService transactionService;

    /**
     * Get address transactions summary from neoscan.io
     * https://neoscan.io/docs/index.html#api-v1-get
     * 
     * @param accountInfo
     */
    public void getTransactions(AccountInfo accountInfo) {
        HashMap<String, String> map = new HashMap<>();
        map.put("tableSymbol", "neo");
        map.put("address", accountInfo.getAddress());
        HashMap<String, String> lastTxInfo = transactionService.getTransactionInfoOne(map);

        customWebClinet.mutate() /* 기존 설정값 상속하여 사용 */
                .baseUrl("https://api.neoscan.io/api/main_net/v1/get_address_abstracts/" + accountInfo.getAddress() + "/" + accountInfo.getPage()).build()
                .get()
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError()
                        , clientResponse ->
                                clientResponse.bodyToMono(String.class)
                                        .map(body -> new RuntimeException(body)))
                .bodyToMono(AddressTransactionsSummary.class)
                .subscribe(response -> {
                    HashMap<String, String> accountInfoMap = new HashMap<>();
                    accountInfoMap.put("tableSymbol", "neo");
                    accountInfoMap.put("address", accountInfo.getAddress());

                    if (response.getTotal_entries() == 0) {
                        accountInfoMap.put("complete", "Y");
                        transactionService.putAccountStatus(accountInfoMap);
                    }

                    if (response.getEntries().size() > 0) {
                        for (AddressTransactionsEntries entry : response.getEntries()) {
                            // 트랜잭션이 이미 수집이 된 경우,
                            if (null != lastTxInfo && lastTxInfo.get("hash").equals(entry.getTxid())) {

                                accountInfoMap.put("complete", "Y");
                                transactionService.putAccountStatus(accountInfoMap);

                                return;
                            }

                            CsvModel csvModel = new CsvModel();

                            csvModel.setTableSymbol("neo");
                            csvModel.setAddr(accountInfo.getAddress());
                            csvModel.setBlock(entry.getBlock_height());

                            String date = commonService.getEpochTimeToDate(entry.getTime());
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                            csvModel.setCreated_at(LocalDateTime.parse(date, formatter));

                            csvModel.setHash(entry.getTxid());
                            csvModel.setAmount(new BigDecimal(entry.getAmount()));
                            csvModel.setFrom(entry.getAddress_from());
                            csvModel.setTo(entry.getAddress_to());
                            csvModel.setSymbol(entry.getAsset());
                            csvModel.setIsValid(1);

                            if (accountInfo.getAddress().equals(entry.getAddress_to())) {
                                csvModel.setIsDeposit(1);
                            } else {
                                csvModel.setIsDeposit(0);
                            }

                            transactionService.putBlockTransaction(csvModel);

                        }

                        if (response.getTotal_pages() == accountInfo.getPage()) {
                            accountInfoMap.put("complete", "Y");
                        } else {
                            accountInfoMap.put("page", String.valueOf(accountInfo.getPage() + 1));
                        }

                        transactionService.putAccountStatus(accountInfoMap);
                    }
                });
    }

}
