package kz.aday.repservice.talday.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatMeasure {
    private String statMeasureId;
    private String id;
    private String text;
    private Long kfc; // это значение для того чтобы расчитать значение по заданому критерию, нужно на это число делить
    private String sign;
    private Boolean leaf;
    private Boolean expand;
}
