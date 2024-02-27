package kz.aday.repservice.talday.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Passport {
    private Long statId;
    private String title;
    private String value;
}
