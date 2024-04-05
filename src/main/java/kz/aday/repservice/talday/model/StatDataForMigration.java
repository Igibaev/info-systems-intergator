package kz.aday.repservice.talday.model;

import lombok.Builder;
import lombok.Data;

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
}
