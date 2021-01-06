package cone.blockchain.crawler;

import com.fasterxml.jackson.databind.ObjectMapper;
import cone.blockchain.crawler.lib.tronprotocol.common.utils.Base58;
import cone.blockchain.crawler.lib.tronprotocol.common.utils.Sha256Hash;
import cone.blockchain.crawler.models.neo.AddressTransactionsSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@SpringBootTest
class CoinoneCrawlerApplicationTests {

	@Autowired
	private WebClient customWebClinet;

	@Test
	void contextLoads() {
	}

	@Test
	public void webClinet() throws Exception {
		customWebClinet.mutate() /* 기존 설정값 상속하여 사용 */
//				.baseUrl("https://api.neoscan.io/api/main_net/v1/get_address_abstracts/AeMp7KEahQJ29pVFaQnjpNYZyuBYhxyu7k/1").build()
				.baseUrl("https://api.trongrid.io/v1/accounts/TDoyjmPJHzRFmYfCRLRsPhKjLETwd9fKr9/transactions?limit=20&only_from=false&fingerprint=9mPzvhwtq8uTG3KkJa6hTeawdSUn7rFe4HYN6jr1zhuzv4nrzUZqNzwRrf1XhsEbHLwYqLZd7tSTyQY1fcbab72cYDbRApPPR").build()
				.get()
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.onStatus(status -> status.is4xxClientError() || status.is5xxServerError()
						, clientResponse ->
								clientResponse.bodyToMono(String.class)
										.map(body -> new RuntimeException(body)))
				.bodyToMono(String.class)
				.subscribe(response -> {
					ObjectMapper objectMapper = new ObjectMapper();
					try {
						Map<String, String> map = objectMapper.readValue(response, Map.class);
						System.out.print(map.toString());
						map.get("data");
					}catch (Exception e){
						throw new IllegalArgumentException("convert JSON string to Map ERROR");
					}

					System.out.print(response);
				});
		Thread.sleep(10000);
	}

	@Test
	public void unixSecondsToDate() {
		long unixSeconds = 1609819876L;
		// convert seconds to milliseconds
		Date date = new java.util.Date(unixSeconds * 1000L);
		// the format of your date
		SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		// give a timezone reference for formatting (see comment at the bottom)
		sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
		String formattedDate = sdf.format(date);
		System.out.println(formattedDate);
	}

	@Test
	public void getUnixTimeStampFromDate() throws Exception {
//		Date date = new Date();
//		System.out.print(date.getTime());

		SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
		String dateString = "2019-12-31 00:00:00";
		Date date = dateFormat.parse(dateString);
		System.out.print(date.getTime());
	}


	@Test
	public void hexStringTobBase58() {
		String hexString = "412a21b0018f7e74bea56efc04eb907911d55ed068";
		hexString = adjustHex(hexString);
		byte[] decodedHex = hexString == null? new byte[0] : org.spongycastle.util.encoders.Hex.decode(hexString);
		String base58 = encode58(decodedHex);
		System.out.print(base58);
	}

	public String adjustHex(String hexString) {
		if (hexString.startsWith("0x")) {
			hexString = "41" + hexString.substring(2);
		}
		if (hexString.length() % 2 == 1) {
			hexString = "0" + hexString;
		}
		return hexString;
	}


	public String encode58(byte[] input) {
		byte[] hash0 = Sha256Hash.hash(true, input);
		byte[] hash1 = Sha256Hash.hash(true, hash0);
		byte[] inputCheck = new byte[input.length + 4];
		System.arraycopy(input, 0, inputCheck, 0, input.length);
		System.arraycopy(hash1, 0, inputCheck, input.length, 4);
		return Base58.encode(inputCheck);
	}

}
