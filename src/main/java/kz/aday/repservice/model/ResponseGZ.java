package kz.aday.repservice.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ResponseGZ {
    private Long total;
    private int limit;
    private String nextPage;
    private List<Map<String,String>> rows;

    public ResponseGZ(Long total, int limit, String nextPage, List<Map<String, String>> rows) {
        this.total = total;
        this.limit = limit;
        this.nextPage = nextPage;
        this.rows = rows;
    }
}
