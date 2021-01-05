package cone.blockchain.crawler.models.neo;

import lombok.Data;

@Data
public class AddressTransactionsEntries {
    private String txid;
    private Long time;
    private Long block_height;
    private String asset;
    private String amount;
    private String address_to;
    private String address_from;
}
