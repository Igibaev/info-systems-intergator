package kz.aday.repservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class EntityMigration {
    private Long id;
    private String name;
    private String nameRu;
    private String token;
    private String url;
    private Map<String,String> localization;

    public EntityMigration(Long id, String name, String nameRu, String token, String url, Map<String, String> localization) {
        this.id = id;
        this.name = name;
        this.nameRu = nameRu;
        this.token = token;
        this.url = url;
        this.localization = localization;
    }

    public void addLocalization(List<EntityMigrationField> fields) {
        localization = new HashMap<>();
        fields.forEach(field -> {
            if (field.getIsRequired()) {
                localization.put(field.getName(), field.getLocalizedName());
            }
        });
    }
}
