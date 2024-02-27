package kz.aday.repservice.talday.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatSegment {
    @JsonIgnore
    private static String SEPARATOR_FOR_DIC_ID = " + ";
    @JsonIgnore
    private static String SEPARATOR_FOR_TERMS = ",";
    private String dicId;
    private String dicClassId;
    private String names;
    private String fullNames;
    private String termIds;
    private String termNames;
    private Integer dicCount;
    private Integer idx;
    private Integer id;
    private Integer order;
    private Integer decFormat;
    private String keywordDic;
    private String maxDate;

    private Long statPeriodId;
    private Long statId;

    public String getDicIdsSeparatedBy(String value) {
        return dicId.replace(SEPARATOR_FOR_DIC_ID, value);
    }

    public Integer getTermIdsCount() {
        return termIds.split(SEPARATOR_FOR_TERMS).length;
    }

    public String getTermIdsReplacedByOrderAndNewTermId(int orderNum, StatFilter statFilter) {
        String[] terms = termIds.split(SEPARATOR_FOR_TERMS);
        if (orderNum >= terms.length) {
            terms[orderNum-1] = statFilter.getId().toString();
        } else {
            terms[orderNum] = statFilter.getId().toString();
        }
        return StringUtils.join(terms, ",");
    }

    public String getFirstTermId() {
        String[] terms = termIds.split(SEPARATOR_FOR_TERMS);
        return terms[0];
    }

}
