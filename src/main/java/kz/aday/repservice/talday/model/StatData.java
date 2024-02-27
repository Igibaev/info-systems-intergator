package kz.aday.repservice.talday.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import kz.aday.repservice.talday.StatDataDeserializer;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Data
@NoArgsConstructor
@JsonDeserialize(using = StatDataDeserializer.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StatData {
    private String id;
    private String rownum;
    private String text;
    private boolean leaf;
    private boolean expanded;
    private String measureName;
    private Map<String,String> dateDataMap = new HashMap<>();
    private String parentId;
    private Long statId;
    private Long statPeriodId;
    private Long statFilterId;
    private String termIds;
    private String dicIds;
    private String filterTermIds;
    private String statMeasureId;

    public StatData(String id, String rownum, String text, boolean leaf, boolean expanded, String measureName,
                    Map<String, String> dateDataMap, String parentId, Long statId, Long statPeriodId, Long statFilterId,
                    String termIds, String dicIds, String filterTermIds, String statMeasureId) {
        this.id = id;
        this.rownum = rownum;
        this.text = text;
        this.leaf = leaf;
        this.expanded = expanded;
        this.measureName = measureName;
        this.dateDataMap = dateDataMap;
        this.parentId = parentId;
        this.statId = statId;
        this.statPeriodId = statPeriodId;
        this.statFilterId = statFilterId;
        this.termIds = termIds;
        this.dicIds = dicIds;
        this.filterTermIds = filterTermIds;
        this.statMeasureId = statMeasureId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StatData statData = (StatData) o;
        return leaf == statData.leaf && expanded == statData.expanded && Objects.equals(id, statData.id) && Objects.equals(rownum, statData.rownum) && Objects.equals(text, statData.text) && Objects.equals(measureName, statData.measureName) && Objects.equals(dateDataMap, statData.dateDataMap) && Objects.equals(parentId, statData.parentId) && Objects.equals(statId, statData.statId) && Objects.equals(statPeriodId, statData.statPeriodId) && Objects.equals(statFilterId, statData.statFilterId) && Objects.equals(termIds, statData.termIds) && Objects.equals(dicIds, statData.dicIds) && Objects.equals(filterTermIds, statData.filterTermIds) && Objects.equals(statMeasureId, statData.statMeasureId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, rownum, text, leaf, expanded, measureName, dateDataMap, parentId, statId, statPeriodId, statFilterId, termIds, dicIds, filterTermIds, statMeasureId);
    }
}
