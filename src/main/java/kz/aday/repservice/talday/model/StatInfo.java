package kz.aday.repservice.talday.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Column;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatInfo {
    private Long id;
    private String namePath;
    private String name;
    private String shortName;
    private String fullCode;
    private String cgParams;
    private String termNames;
    private List<Passport> passport;
    private String measureId;
    private String measureKfc;
    private String measureSign;
    private String measureName;
    private String preferredMeasureId;
    private String preferredMeasureName;
    private String preferredMeasureKfc;
    private String preferredMeasureSign;

    @Override
    public String toString() {
        return "StatInfo{" +
                "id=" + id +
                ", namePath='" + namePath + '\'' +
                ", name='" + name + '\'' +
                ", shortName='" + shortName + '\'' +
                ", fullCode='" + fullCode + '\'' +
                ", cgParams='" + cgParams + '\'' +
                ", termNames='" + termNames + '\'' +
                ", passport=" + passport.size() +
                ", measureId=" + measureId +
                ", measureKfc=" + measureKfc +
                ", measureSign='" + measureSign + '\'' +
                ", measureName='" + measureName + '\'' +
                ", preferredMeasureId=" + preferredMeasureId +
                ", preferredMeasureName='" + preferredMeasureName + '\'' +
                ", preferredMeasureKfc=" + preferredMeasureKfc +
                ", preferredMeasureSign='" + preferredMeasureSign + '\'' +
                '}';
    }

}

