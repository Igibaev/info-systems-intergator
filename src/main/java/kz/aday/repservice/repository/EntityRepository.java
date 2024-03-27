package kz.aday.repservice.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.aday.repservice.model.EntityMigration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Repository
public class EntityRepository {
    private final String SELECT_ALL = "SELECT id, entityName, nameRu, token, url, localization FROM entity_migration;";
    private final String SELECT_BY_NAME = "SELECT id, entityName, nameRu, token, url, localization FROM entity_migration WHERE entityName = :name;";
    private final String SELECT_BY_ID = "SELECT id, entityName, nameRu, token, url, localization FROM entity_migration WHERE id = :id;";
    private final String DROP_TABLE = "DROP TABLE %s;";
    private final String DELETE = "DELETE FROM entity_migration WHERE id = :id;";
    private final String INSERT = "INSERT INTO entity_migration (entityName, nameRu, token, url, localization)\n" +
            "VALUES (:entityName, :nameRu, :token, :url, :localization)";
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EntityRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public EntityMigration getById(Long id) {
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("id", id);
        List<EntityMigration> entityMigrationList = jdbcTemplate.queryForList(SELECT_BY_ID, parameterSource).stream()
                .map(this::toEntityMigration)
                .collect(Collectors.toList());
        return entityMigrationList.isEmpty() ? null : entityMigrationList.get(0);
    }

    public EntityMigration getByName(String name) {
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("name", name);
        List<EntityMigration> entityMigrationList = jdbcTemplate.queryForList(SELECT_BY_NAME, parameterSource).stream()
                .map(this::toEntityMigration)
                .collect(Collectors.toList());
        return entityMigrationList.isEmpty() ? null : entityMigrationList.get(0);
    }

    public List<EntityMigration> getAll() {
        return jdbcTemplate.queryForList(SELECT_ALL, EmptySqlParameterSource.INSTANCE).stream()
                .map(this::toEntityMigration)
                .collect(Collectors.toList());
    }

    private EntityMigration toEntityMigration(Map<String, Object> map) {
        Long id = (Long) map.get("id");
        String entityName = (String) map.get("entityName");
        String nameRu = (String) map.get("nameRu");
        String token = (String) map.get("token");
        String url = (String) map.get("url");
        Map<String, String> localization = convertToMap(map.get("localization"));
        return new EntityMigration(id, entityName, nameRu, token, url, localization);
    }

    private Map<String, String> convertToMap(Object field) {
        String localization = field == null ? null : (String) field;
        if (localization == null || localization.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(localization, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void save(EntityMigration entityMigration) {
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("entityName", entityMigration.getName())
                .addValue("url", entityMigration.getUrl())
                .addValue("token", entityMigration.getToken())
                .addValue("nameRu", entityMigration.getNameRu())
                .addValue("localization", asJsonText(entityMigration.getLocalization()));
        jdbcTemplate.update(INSERT, parameterSource);
    }

    public void delete(Long entityId) {
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("id", entityId);
        jdbcTemplate.update(DELETE, parameterSource);

    }

    private Object asJsonText(Map<String, String> localization) {
        if (localization == null || localization.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.writeValueAsString(localization);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void dropAssosiatedTable(String name) {
        try {
            jdbcTemplate.getJdbcOperations().execute(String.format(DROP_TABLE, name));
        } catch (Exception e) {
            log.warn("CANNOT DROP TABLE {}, because {}",name, e.getMessage());
        }
    }
}
