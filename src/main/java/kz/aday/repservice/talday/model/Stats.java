package kz.aday.repservice.talday.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Stats {
    private Long statPeriodId;
    private String total;
    private List<Stat> results;

    @Override
    public String toString() {
        return "Stats{" +
                "statPeriodId=" + statPeriodId +
                ", total='" + total + '\'' +
                ", results=" + results.size() +
                '}';
    }
}
