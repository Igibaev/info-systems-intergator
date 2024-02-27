package kz.aday.repservice.talday;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import kz.aday.repservice.talday.model.StatData;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class StatDataDeserializer extends StdDeserializer<StatData> {
    public StatDataDeserializer() {
        this(null);
    }

    public StatDataDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public StatData deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        StatData statData = new StatData();
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> mapJson = iterator.next();
            switch (mapJson.getKey()) {
                case "id" -> statData.setId(mapJson.getValue().asText());
                case "rownum" -> statData.setRownum(mapJson.getValue().asText());
                case "text" -> statData.setText(mapJson.getValue().asText());
                case "leaf" -> statData.setLeaf(mapJson.getValue().asBoolean());
                case "expanded" -> statData.setExpanded(mapJson.getValue().asBoolean());
                case "measureName" -> statData.setMeasureName(mapJson.getValue().asText());
                default -> statData.getDateDataMap().put(mapJson.getKey(), mapJson.getValue().asText());
            }
        }
        return statData;
    }
}
