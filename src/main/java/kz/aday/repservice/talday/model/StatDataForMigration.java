package kz.aday.repservice.talday.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Builder
@Data
public class StatDataForMigration {
    private String statName;
    private String filterName;
    private String statDataText;
    private String statMeasureName;
    private String statDate;
    private String statValue;
    private String statRawValue;

    public static Map<String, JsonNode> headers = new HashMap<>();

    static {
        headers.put("stat_name", new TextNode("Наименование статистики"));
        headers.put("filter_name", new TextNode("Примененные фильтры"));
        headers.put("stat_data_text", new TextNode("Наименочание данных статистики(обычно там регион или другие данные)"));
        headers.put("stat_measure_name", new TextNode("Тип измерения"));
        headers.put("stat_date", new TextNode("Дата"));
        headers.put("stat_value", new TextNode("Значение по статистике согласно типу измерения"));
        headers.put("stat_raw_value", new TextNode("Значение по статистике(без применения типа измерения)"));
    }

    public Map<String, JsonNode> row() {
        Map<String, JsonNode> row = new HashMap<>();
        row.put("stat_name", new TextNode(getEmptyStringIfNull(statName)));
        row.put("filter_name", new TextNode(getEmptyStringIfNull(filterName)));
        row.put("stat_data_text", new TextNode(getEmptyStringIfNull(statDataText)));
        row.put("stat_measure_name", new TextNode(getEmptyStringIfNull(statMeasureName)));
        row.put("stat_date", new TextNode(getEmptyStringIfNull(statDate)));
        row.put("stat_value", new TextNode(getEmptyStringIfNull(statValue)));
        row.put("stat_raw_value", new TextNode(getEmptyStringIfNull(statRawValue)));
        return row;
    }

    private String getEmptyStringIfNull(String value) {
        return value == null ? "" : value;
    }
}
