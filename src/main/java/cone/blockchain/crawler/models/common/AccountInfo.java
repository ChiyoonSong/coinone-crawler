package cone.blockchain.crawler.models.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class AccountInfo implements Serializable {
    int seq;
    String symbol;
    String address;
    String contract;
    String tag;
    String memo;
    Long startBlock;
    Long endBlock;
    int page;
    int transferPage;
    String complete;

    public AccountInfo(){super();}

    public AccountInfo(String symbol, String address, String contract, String tag, String memo){
        super();
        this.symbol = symbol;
        this.address = address;
        this.contract = contract;
        this.tag = tag;
        this.memo = memo;
    }

}
