package cone.blockchain.crawler.biz.service;

import cone.blockchain.crawler.biz.mapper.TransactionMapper;
import cone.blockchain.crawler.models.common.AccountInfo;
import cone.blockchain.crawler.models.common.CsvModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionMapper transactionMapper;

    public TransactionService(TransactionMapper transactionMapper) {
        this.transactionMapper = transactionMapper;
    }

    @Transactional(readOnly = true)
    public List<AccountInfo> getAccountInfo(HashMap<String, String> map) {
        return transactionMapper.selectAccountInfo(map);
    }

    @Transactional(readOnly = true)
    public HashMap<String, String> getTransactionInfoOne(HashMap<String, String> map) {
        return transactionMapper.selectTransactionInfoOne(map);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public int putBlockTransaction(CsvModel csvModel) {
        return transactionMapper.insertBlockTransaction(csvModel);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public int putAccountStatus(HashMap<String, String> map) {
        return transactionMapper.updateAccountStatus(map);
    }

    /*@Transactional(propagation = Propagation.REQUIRED)
    public int putTransferTransaction(HashMap<String, String> map) {
        return transactionMapper.insertTransferTransaction(map);
    }*/


}
