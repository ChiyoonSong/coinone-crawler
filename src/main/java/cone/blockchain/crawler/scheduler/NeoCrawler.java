package cone.blockchain.crawler.scheduler;

import cone.blockchain.crawler.biz.service.TransactionService;
import cone.blockchain.crawler.models.common.AccountInfo;
import cone.blockchain.crawler.service.neo.NeoScan;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

@RequiredArgsConstructor
@Component
public class NeoCrawler {
    private static final Logger logger = LogManager.getLogger(NeoCrawler.class);

    private final TransactionService transactionService;

    private final NeoScan neoScan;

    @Scheduled(fixedDelay = 1000)
    public void NeoCrawler() {
        logger.info("Neo crawler job execute..");

        HashMap<String, String> accountMap = new HashMap<>();
        accountMap.put("symbol", "neo");
        accountMap.put("complete", "N");
        List<AccountInfo> accountList = transactionService.getAccountInfo(accountMap);

        for (AccountInfo accountInfo : accountList) {
            neoScan.getTransactions(accountInfo);

        }
    }
    /**
     *
     * select asset_symbol   as symbol,
     *        target_address as addr,
     *        nonce,
     *        from_address   as 'from',
     *        to_address     as 'to',
     *        tag,
     *        sequence,
     *        hash,
     *        contract,
     *        block,
     *        amount,
     *        fee,
     *        is_valid,
     *        id_deposit,
     *        block_time     as 'created_at'
     * from coinone_neo_tx
     * where block_time > '2019-12-31'
     * order by block_time;
     */
}
