package kz.aday.repservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import javax.net.ssl.SSLException;
import java.time.Duration;

@Configuration
public class WebClientConfiguration {
    private static final String GOS_ZAKUP_BASE_URL = "https://ows.goszakup.gov.kz";
    private static final String TALDAY_BASE_URL = "https://ows.goszakup.gov.kz";
    public static final int TIMEOUT = 1000;

    @Bean
    public WebClient taldayApiClient() throws SSLException {
        return getWebClient(TALDAY_BASE_URL);
    }

    @Bean
    public WebClient gosZakupApiClient() throws SSLException {
        return getWebClient(GOS_ZAKUP_BASE_URL);
    }

    private static WebClient getWebClient(String baseUrl) throws SSLException {
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
                .baseUrl(baseUrl)
                .codecs(configurer -> {
                    // This API returns JSON with content type text/plain, so need to register a custom
                    // decoder to deserialize this response via Jackson

                    // Get existing decoder's ObjectMapper if available, or create new one
                    ObjectMapper objectMapper = configurer.getReaders().stream()
                            .filter(reader -> reader instanceof Jackson2JsonDecoder)
                            .map(reader -> (Jackson2JsonDecoder) reader)
                            .map(reader -> reader.getObjectMapper())
                            .findFirst()
                            .orElseGet(() -> Jackson2ObjectMapperBuilder.json().build());

                    Jackson2JsonDecoder decoder = new Jackson2JsonDecoder(objectMapper, MediaType.TEXT_HTML);
                    configurer.customCodecs().registerWithDefaultConfig(decoder);
                    configurer.defaultCodecs().maxInMemorySize(5000 * 1024);
                })
                .build();
    }
}