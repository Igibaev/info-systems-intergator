package kz.aday.repservice.talday.model.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import kz.aday.repservice.talday.model.Stat;
import kz.aday.repservice.talday.model.StatFilter;
import kz.aday.repservice.talday.model.StatInfo;
import kz.aday.repservice.talday.model.StatMeasure;
import kz.aday.repservice.talday.model.StatPeriod;
import kz.aday.repservice.talday.model.StatSegment;
import kz.aday.repservice.util.JsonUtil;
import lombok.Data;

@Data
public class TaldayRequest {
    Boolean isFiltered;
    String tableName;
    String statPeriodJson;
    String statJson;
    String statInfoJson;
    String statSegmentJson;
    String statFilterJson;
    String statMeasureJson;
    Long statPeriodId;
    Long statId;
    String statInfoMeasureId;

    public StatMeasure getStatMeasure() {
        try {
            return JsonUtil.objectMapper().readValue(statMeasureJson, StatMeasure.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public StatFilter getStatFilter() {
        try {
            return JsonUtil.objectMapper().readValue(statFilterJson, StatFilter.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public StatSegment getStatSegment() {
        try {
            return JsonUtil.objectMapper().readValue(statSegmentJson, StatSegment.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public StatPeriod getStatPeriod() {
        try {
            return JsonUtil.objectMapper().readValue(statPeriodJson, StatPeriod.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public Stat getStat() {
        try {
            return JsonUtil.objectMapper().readValue(statJson, Stat.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public StatInfo getStatInfo() {
        try {
            return JsonUtil.objectMapper().readValue(statInfoJson, StatInfo.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public Long getStatPeriodId() {
        return getStatPeriod().getId();
    }

    public Long getStatId() {
        return getStat().getId();
    }

    public String getStatInfoMeasureId() {
        return getStatInfo().getMeasureId();
    }

    @Override
    public String toString() {
        return "TaldayRequest{" +
                "isFiltered=" + isFiltered +
                ", tableName='" + tableName + '\'' +
                ", statPeriodJson='" + statPeriodJson + '\'' +
                ", statJson='" + statJson + '\'' +
                ", statInfoJson='" + statInfoJson + '\'' +
                ", statSegmentJson='" + statSegmentJson + '\'' +
                ", statFilterJson='" + statFilterJson + '\'' +
                ", statMeasureJson='" + statMeasureJson + '\'' +
                ", statPeriodId=" + statPeriodId +
                ", statId=" + statId +
                ", statInfoMeasureId='" + statInfoMeasureId + '\'' +
                '}';
    }
}
