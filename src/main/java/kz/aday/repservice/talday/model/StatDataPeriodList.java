package kz.aday.repservice.talday.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class StatDataPeriodList {
    List<String> dateList;
    List<String> datesIds;
    List<String> datesToDraw;
    List<String> periodNameList;
}
