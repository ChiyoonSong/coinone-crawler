package cone.blockchain.crawler.configuration.webclient;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.LoggingCodecSupport;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

/**
 * https://medium.com/@odysseymoon/spring-webclient-%EC%82%AC%EC%9A%A9%EB%B2%95-5f92d295edc0
 */
@Configuration
public class WebClientConfiguration {
    private static final Logger logger = LogManager.getLogger(WebClientConfiguration.class);

    @Bean(name = "customWebClinet")
    public WebClient webClient() {
        /**
         * default in-memory buffer size : 256KB
         * unlimited memory size : -1
         * ex. 10MB : 10 * 1024 * 1024
         */
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)).build();

        /**
         * from Data, header 정보는 민감한 저보를 포함하기 때문에, 기본 WebClient 설정에서는 위 정보를 로그에서 확인할 수 없다.
         * Request/Response 정보를 상세히 하기 위해 ExchangeStrateges, Logging Level 설정을 통해 로그 확인 가능.
         *
         * application.yml 에 개발용 로깅 레벨은 DEBUG로 설정
         *  logging:
         *    level:
         *      org.springframework.web.reactive.function.client.ExchangeFunctions: DEBUG
         */
        exchangeStrategies
                .messageWriters().stream()
                .filter(LoggingCodecSupport.class::isInstance)
                .forEach(writer -> ((LoggingCodecSupport) writer).setEnableLoggingRequestDetails(true));
        /**
         * WebClient.builder()를 통해 아래와 같은 설정이 가능
         *  모든 호출에 대한 기본 Header / Cookie 값 설정
         *  filter 를 통한 Request/Response 처리
         *  Http 메시지 Reader/Writer 조작
         *  Http Client Library 설정
         */
        return WebClient.builder()
                /*HttpClient TimeOut*/
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                /*.secure(
                                        *//*HTTPS 인증서를 검증하지 않고 바로 접속하는 설정*//*
                                        com.crawling.configurations.ThrowingConsumer.unchecked(
                                                sslContextSpec -> sslContextSpec.sslContext(
                                                        SslContextBuilder
                                                                .forClient()
                                                                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                                                .build()
                                                )
                                        )
                                )*/
                                .tcpConfiguration(client -> client.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                                        .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(5000, TimeUnit.MILLISECONDS))
                                                .addHandlerLast((new WriteTimeoutHandler(5000, TimeUnit.MILLISECONDS))))
                                )
                ))
                .exchangeStrategies(exchangeStrategies)
                .filter(ExchangeFilterFunction.ofRequestProcessor(
                        clientRequest -> {
                            clientRequest.headers()
                                    .forEach((name, values) -> values.forEach(value -> logger.debug("request - {} : {}", name, value)));
                            return Mono.just(clientRequest);
                        }
                ))
                .filter(ExchangeFilterFunction.ofResponseProcessor(
                        clientResponse -> {
                            clientResponse.headers()
                                    .asHttpHeaders()
                                    .forEach((name, values) ->
                                            values.forEach(value -> logger.debug("response - {} : {}", name, value)));
                            return Mono.just(clientResponse);
                        }
                ))
                .defaultHeader("user-agent", "WebClient")
                .build();
    }
}
