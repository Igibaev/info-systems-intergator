package kz.aday.repservice.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.aday.repservice.api.Fields;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class JsonUtil {
    private final static String text = "Текстовое";
    private final static String array= "Массив";
    private final static ObjectMapper objectMapper = new ObjectMapper();

    public static List<Map<String, JsonNode>> convertToListRow(JsonNode node) {
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

    public static List<Map<String, JsonNode>> convertToListRow(JsonNode node, Map<String, String> requiredFields) {
        List<Map<String, JsonNode>> rows = new ArrayList<>();
        Iterator<JsonNode> iterator = node.get(Fields.items).elements();
        while (iterator.hasNext()) {
            Map<String, JsonNode> row = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> rowNodeIterator = iterator.next().fields();
            while (rowNodeIterator.hasNext()) {
                Map.Entry<String, JsonNode> rowNode = rowNodeIterator.next();
                if (requiredFields.containsKey(rowNode.getKey())) {
                    row.put(rowNode.getKey(), rowNode.getValue());
                }
            }
            rows.add(row);
        }
        return rows;
    }


    public static String getAsText(JsonNode value) {
        if (!value.isArray()) {
            return value.asText();
        }
        return null;
    }

    public static String toJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static ObjectMapper objectMapper() {
        return objectMapper;
    }
}
