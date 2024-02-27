package kz.aday.repservice.talday.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatDataRequest {
    Long statPeriodId;
    Long statId;
    String statMeasureId;
    String firstTermId;
    String filterTermIds;
    String dicIds;
    String parentId;
    String segmentTermIds;
    Long filterId;
    boolean isFound;
}
