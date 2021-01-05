package cone.blockchain.crawler.models.neo;

import lombok.Data;

import java.util.List;

@Data
public class AddressTransactionsSummary {
    private int total_pages;
    private int total_entries;
    private int page_size;
    private int page_number;
    List<AddressTransactionsEntries> entries;
}
