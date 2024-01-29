package kz.aday.repservice.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ResponseGZ {
    private Long total;
    private int limit;
    private String nextPage;
    private List<Map<String, JsonNode>> rows;

    public ResponseGZ(Long total, int limit, String nextPage, List<Map<String, JsonNode>> rows) {
        this.total = total;
        this.limit = limit;
        this.nextPage = nextPage;
        this.rows = rows;
    }
}
