package kz.aday.repservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityMigrationDto {
    List<EntityMigrationField> fields;
    EntityMigration entityMigration;
}
