package kz.aday.repservice.api;

import com.fasterxml.jackson.databind.JsonNode;
import kz.aday.repservice.api.Fields;
import kz.aday.repservice.model.RequestGZ;
import kz.aday.repservice.model.ResponseGZ;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GosZakupApi {
    private static final String SEARCH_AFTER = "search_after";
    private static final String PAGE_NEXT = "page";
    public static final int BATCH_SIZE = 500;
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_TOKEN = "Bearer ";
    private final WebClient webClient;

    public GosZakupApi(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<ResponseGZ> execute(RequestGZ request) {
        return executeJson(request)
                .map(jsonNode -> {
                    Long total = jsonNode.get(Fields.total).asLong();
                    int limit = jsonNode.get(Fields.limit).asInt();
                    String nextPage = jsonNode.get(Fields.nexPage).asText();
                    return new ResponseGZ(total, limit, nextPage, convertToListRow(jsonNode));
                });
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

    private Mono<JsonNode> executeJson(String url, String token) {
        log.info("SEND GET TOTAL REQUEST URL:{}",url);
        return webClient
                .get()
                .uri(url)
                .header(AUTHORIZATION_HEADER, BEARER_TOKEN + token)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .retry(10);
    }

    private Mono<JsonNode> executeJson(RequestGZ request) {
        String url = createUrl(request);
        log.info("SEND GET REQUEST URL:{}",url);
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
            if (request.getUrl().contains("search_after") || request.getSearchAfter() == null || request.getSearchAfter() == 0 ) {
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

    private List<Map<String, JsonNode>> convertToListRow(JsonNode node) {
        List<Map<String, JsonNode>> rows = new ArrayList<>();
        Iterator<JsonNode> iterator = node.get(Fields.items).elements();
        while (iterator.hasNext()) {
            Map<String, JsonNode> row = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> rowNodeIterator = iterator.next().fields();
            while (rowNodeIterator.hasNext()) {
                Map.Entry<String, JsonNode> rowNode = rowNodeIterator.next();
                row.put(rowNode.getKey(), rowNode.getValue());
            }
            rows.add(row);
        }
        return rows;
    }
}
