package cone.blockchain.crawler.scheduler;


import cone.blockchain.crawler.biz.service.TransactionService;
import cone.blockchain.crawler.models.common.AccountInfo;
import cone.blockchain.crawler.service.trx.TrxScan;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

@RequiredArgsConstructor
@Component
public class TrxCrawler {
    private static final Logger logger = LogManager.getLogger(TrxCrawler.class);

    private final TransactionService transactionService;

    private final TrxScan trxScan;

    @Scheduled(fixedDelay = 1500)
    public void NeoCrawler() {
        logger.info("Trx crawler job execute..");

        HashMap<String, String> accountMap = new HashMap<>();
        accountMap.put("symbol", "trx");
        accountMap.put("complete", "N");
        List<AccountInfo> accountList = transactionService.getAccountInfo(accountMap);

        for (AccountInfo accountInfo : accountList) {
            trxScan.getTransactions(accountInfo);

        }
    }
    /**
     * token id
     * btt : 1002000
     *
     * token address
     * sun : TKkeiboTkxXKJpbmVFbv4a8ov5rAfRDMf9
     * jst : TCFLL5dx5ZJdKnWuesXxi1VPwjLVmWZZy9
     */
}
