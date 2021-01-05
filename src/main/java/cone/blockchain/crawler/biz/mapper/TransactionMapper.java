package cone.blockchain.crawler.biz.mapper;

import cone.blockchain.crawler.models.common.AccountInfo;
import cone.blockchain.crawler.models.common.CsvModel;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;

@Repository
public interface TransactionMapper {

    List<AccountInfo> selectAccountInfo(HashMap<String, String> map);

    int insertBlockTransaction(CsvModel csvModel);

    int updateAccountStatus(HashMap<String, String> map);

    HashMap<String, String> selectTransactionInfoOne(HashMap<String, String> map);

    int insertTransferTransaction(HashMap<String, String> map);
}
