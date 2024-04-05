package kz.aday.repservice.talday.model;

import kz.aday.repservice.util.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatCombination {
    private Integer dicCount;
    private String fullText;
    private String id;
    private String text;
    private Long statPeriodId;
    private Long statId;

    public String toJson() {
        return JsonUtil.toJson(this);
    }
}
