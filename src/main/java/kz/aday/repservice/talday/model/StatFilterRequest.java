package kz.aday.repservice.talday.model;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class StatFilterRequest {
    Long statPeriodId;
    Long statId;
    String dicIds;
    String dic;
    Integer idx;
}
