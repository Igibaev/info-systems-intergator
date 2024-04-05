package kz.aday.repservice.talday.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import kz.aday.repservice.util.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StatPeriod {
    private Long id;
    private String text;

    public String toJson() {
        return JsonUtil.toJson(this);
    }
}
