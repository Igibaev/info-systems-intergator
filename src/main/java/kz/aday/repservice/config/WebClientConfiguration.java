package kz.aday.repservice.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import javax.net.ssl.SSLException;
import java.time.Duration;

@Configuration
public class WebClientConfiguration {
    private static final String BASE_URL = "https://ows.goszakup.gov.kz";
    public static final int TIMEOUT = 1000;

    @Bean
    public WebClient webClientWithTimeout() throws SSLException {
        ConnectionProvider provider = ConnectionProvider.builder("random")
                .maxConnections(5)
                .pendingAcquireMaxCount(100)
                .pendingAcquireTimeout(Duration.ofSeconds(60))
                .build();

        SslContext context = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();
        HttpClient httpClient = HttpClient.create(provider)
                .responseTimeout(Duration.ofSeconds(100)) // set response timeout
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 100000) // set connection timeout
                .secure(t -> t.sslContext(context))
                .doOnConnected(conn -> conn // set read and write timeout
                        .addHandlerLast(new ReadTimeoutHandler(100))
                        .addHandlerLast(new WriteTimeoutHandler(100)));
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(BASE_URL)
                .build();
    }
}