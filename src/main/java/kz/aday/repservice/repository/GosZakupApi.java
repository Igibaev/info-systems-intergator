package kz.aday.repservice.repository;

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

    private Mono<JsonNode> executeJson(RequestGZ request) {
        return webClient
                .get()
                .uri(createUrl(request))
                .header(AUTHORIZATION_HEADER, BEARER_TOKEN + request.getToken())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .retry(10);
    }

    private String createUrl(RequestGZ request) {
        if (request.getUrl().contains("?")) {
            return String.format("%s&%s=%d", request.getUrl(), Fields.limit, BATCH_SIZE);
        } else {
            return String.format("%s?%s=%d", request.getUrl(), Fields.limit, BATCH_SIZE);
        }
    }

    private List<Map<String, String>> convertToListRow(JsonNode node) {
        List<Map<String, String>> rows = new ArrayList<>();
        Iterator<JsonNode> iterator = node.get(Fields.items).elements();
        while (iterator.hasNext()) {
            Map<String, String> row = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> rowNodeIterator = iterator.next().fields();
            while (rowNodeIterator.hasNext()) {
                Map.Entry<String, JsonNode> rowNode = rowNodeIterator.next();
                row.put(rowNode.getKey(), convertToText(rowNode.getValue()));
            }
            rows.add(row);
        }
        return rows;
    }

    private String convertToText(JsonNode nodeValue) {
        if (nodeValue.isValueNode()) {
            return nodeValue.asText();
        } else {
            if (nodeValue.isArray()) {
                log.error("Implement if value is array");
            } else if (nodeValue.isObject()) {
                log.error("Implement if value is object");
            } else if (nodeValue.isMissingNode()) {
                log.error("Implement if value is missing");
            }
        }
        return "@NULL@";
    }
}
