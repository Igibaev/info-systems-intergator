package kz.aday.repservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EntityMigrationField {
    String name;
    String exampleValue;
    String localizedName;
    Boolean isRequired;


}
