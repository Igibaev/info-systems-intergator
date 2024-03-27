package kz.aday.repservice.api;

import com.fasterxml.jackson.databind.JsonNode;
import kz.aday.repservice.model.EntityMigration;
import kz.aday.repservice.model.RequestGZ;
import kz.aday.repservice.model.ResponseGZ;
import kz.aday.repservice.repository.EntityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static kz.aday.repservice.util.JsonUtil.convertToListRow;

@Slf4j
@Component
public class GosZakupApi {
    private static final String SEARCH_AFTER = "search_after";
    private static final String PAGE_NEXT = "page";
    public static final int BATCH_SIZE = 500;
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_TOKEN = "Bearer ";
    private final WebClient webClient;
    private final EntityRepository entityRepository;
    private final ConcurrentMap<String, Map<String, String>> requiredLocalizedFields = new ConcurrentHashMap<>();

    public GosZakupApi(@Qualifier("gosZakupApiClient") WebClient webClient, EntityRepository entityRepository) {
        this.webClient = webClient;
        this.entityRepository = entityRepository;
    }

    public Mono<ResponseGZ> execute(RequestGZ request) {
        Map<String,String> fields = requiredLocalizedFields.getOrDefault(
                request.getGzEntityName(),
                Optional.ofNullable(entityRepository.getByName(request.getGzEntityName())).orElse(new EntityMigration()).getLocalization()
                );
        if (fields == null || fields.isEmpty()) {
            return executeJson(request)
                    .map(jsonNode -> {
                        Long total = jsonNode.get(Fields.total).asLong();
                        int limit = jsonNode.get(Fields.limit).asInt();
                        String nextPage = jsonNode.get(Fields.nexPage).asText();
                        return new ResponseGZ(total, limit, nextPage, convertToListRow(jsonNode));
                    });
        } else {
            return executeJson(request)
                    .map(jsonNode -> {
                        Long total = jsonNode.get(Fields.total).asLong();
                        int limit = jsonNode.get(Fields.limit).asInt();
                        String nextPage = jsonNode.get(Fields.nexPage).asText();
                        return new ResponseGZ(total, limit, nextPage, convertToListRow(jsonNode, fields));
                    });
        }

    }

    public Mono<ResponseGZ> execute(String url, String token) {
        return executeJson(url, token)
                .map(jsonNode -> {
                    Long total = jsonNode.get(Fields.total).asLong();
                    int limit = jsonNode.get(Fields.limit).asInt();
                    String nextPage = jsonNode.get(Fields.nexPage).asText();
                    return new ResponseGZ(total, limit, nextPage, convertToListRow(jsonNode));
                });
    }

    public Mono<JsonNode> executeJson(String url, String token) {
        log.info("SEND GET TOTAL REQUEST URL:{}", url);
        return webClient
                .get()
                .uri(url)
                .header(AUTHORIZATION_HEADER, BEARER_TOKEN + token)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .retry(10);
    }

    public Mono<JsonNode> executeJson(RequestGZ request) {
        String url = createUrl(request);
        log.info("SEND GET REQUEST URL:{}", url);
        return webClient
                .get()
                .uri(url)
                .header(AUTHORIZATION_HEADER, BEARER_TOKEN + request.getToken())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .retry(10);
    }

    private String createUrl(RequestGZ request) {
        if (request.getUrl().contains("?")) {
            return String.format("%s&%s=%d", request.getUrl(), Fields.limit, BATCH_SIZE);
        } else {
            if (request.getUrl().contains("search_after") || request.getSearchAfter() == null || request.getSearchAfter() == 0) {
                return String.format("%s?%s=%d", request.getUrl(), Fields.limit, BATCH_SIZE);
            } else {
                return String.format("%s?%s=%d&%s=%s&%s=%d",
                        request.getUrl(),
                        Fields.limit,
                        BATCH_SIZE,
                        PAGE_NEXT,
                        "next",
                        SEARCH_AFTER,
                        request.getSearchAfter()
                );

            }
        }
    }
}
