package cone.blockchain.crawler;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.text.SimpleDateFormat;
import java.util.Date;

@SpringBootTest
class CoinoneCrawlerApplicationTests {

	@Test
	void contextLoads() {
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

}
