package cone.blockchain.crawler.models.common;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CsvModel implements Serializable, Cloneable {
    String symbol;      // 코인이름 : 토큰이름을 넣습니다.
    String addr;        // 조사한 주소
    String nonce;       // nonce (없으면 넣지마세요)
    String from;        // from주소 (NEO 처럼 애매하면 안넣어도 됩니다)
    String to;          // to주소
    String tag;         // detag, memo 를 넣습니다.
    String sequence;    // bulk transfer 이면 순서를 넣습니다.
    String hash;        // txid
    String contract;    // 토큰이면 token contract 를 넣으세요
    Long block;         // 블럭넘버
    BigDecimal amount;  // 수량 (1e18)  (샘플데이터와 다릅니다.)
    BigDecimal fee;     // 출금 수수료 (1e18)  (샘플데이터와 다릅니다.)
    int isValid;        // 실제 전송이 되었으면 1, 아니면 0 (성공해도 전송 안됤 수 있음)
    int isDeposit;      // 조사한 주소로 입금된 건은 1, 아니면 0
    String type;
    int unit;
    LocalDateTime created_at;    // UTC 시간

    String tableSymbol;

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public CsvModel() {
        super();
    }

    public CsvModel(String symbol, String addr) {
        super();
        this.symbol = symbol;
        this.addr = addr;
    }
}
