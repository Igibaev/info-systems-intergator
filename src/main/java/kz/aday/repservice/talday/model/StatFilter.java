package kz.aday.repservice.talday.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
@NoArgsConstructor
public class StatFilter {
    private List<StatFilter> children;
    private Long id;
    private Long parentId;
    private String text;
    private boolean leaf;

    private Long statPeriodId;
    private Long statId;
    private String dicIds;
    private String dicId;

    public StatFilter(Long id, Long parentId, String text, Long statPeriodId, Long statId, String dicIds, String dicId) {
        this.id = id;
        this.parentId = parentId;
        this.text = text;
        this.statPeriodId = statPeriodId;
        this.statId = statId;
        this.dicIds = dicIds;
        this.dicId = dicId;
    }

    public void setRequestParamsToAll(Long statPeriodId, Long statId, String dicIds, String dicId) {
        this.statPeriodId = statPeriodId;
        this.statId = statId;
        this.dicIds = dicIds;
        this.dicId = dicId;
        for (StatFilter statFilter: Optional.ofNullable(children).orElse(new ArrayList<>())) {
            statFilter.setRequestParamsToAll(statPeriodId, statId, dicIds, dicId);
        }
    }
}

