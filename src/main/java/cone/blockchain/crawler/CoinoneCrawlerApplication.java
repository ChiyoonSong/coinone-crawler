package cone.blockchain.crawler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class}, scanBasePackages = "cone.blockchain.crawler.*")
public class CoinoneCrawlerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoinoneCrawlerApplication.class, args);
	}

}
