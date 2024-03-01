package kz.aday.repservice.repository;

import kz.aday.repservice.model.Migration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class MigrationRepository {
    private final String SELECT_ALL = "SELECT id, entityName, total, exported, lastRequestUrl, createdDate, status FROM migration ORDER BY id DESC;";
    private final String EXISTS = "SELECT count(*) > 0 FROM migration WHERE entityName = :entityName AND status = :status;";

    private final String INSERT =
            "INSERT INTO migration (id, entityName, total, exported, lastRequestUrl, createdDate, status) \n" +
            "VALUES (:id, :entityName, :total, :exported, :lastRequestUrl, :createdDate, :status);";

    private final String UPDATE =
            "UPDATE migration SET\n" +
            " exported = :exported,\n" +
            " lastRequestUrl = :lastRequestUrl,\n" +
            " status = :status\n" +
            "WHERE id = :id;";

    private final String DELETE = "DELETE FROM migration where id = :id;";

    private final String GENERATE_ID = "SELECT nextval('migration_id_seq');";
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public MigrationRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void executeQuery(String query) {
        // i don't need safe in this project
        jdbcTemplate.getJdbcTemplate().execute(query);
    }

    public boolean exists(String entityName) {
        if (entityName == null) {
            log.error("WHEN CHECK BY DUPLICATE entityName IS NULL");
            return false;
        }
        return jdbcTemplate.queryForObject(
                EXISTS,
                new MapSqlParameterSource()
                        .addValue("entityName", entityName)
                        .addValue("status", Migration.Status.IN_PROGRESS.name()),
                boolean.class
        );
    }

    public void delete(Long id) {
        if (id == null) {
            log.error("WHEN DELETE THE ID IS NULL");
            return;
        }
        jdbcTemplate.update(DELETE, new MapSqlParameterSource().addValue("id", id));
    }

    public void update(Migration migration) {
        if (migration == null) {
            log.error("WHEN UPDATE THE OBJECT IS NULL");
            return;
        }
        jdbcTemplate.update(UPDATE, createUpdatePreparedStatement(migration));
    }

    private SqlParameterSource createUpdatePreparedStatement(Migration migration) {
        return new MapSqlParameterSource()
                .addValue("id", migration.getId())
                .addValue("exported", migration.getExported())
                .addValue("status", migration.getStatus().name())
                .addValue("lastRequestUrl", migration.getLastRequestUrl());
    }

    public void create(Migration migration) {
        if (migration == null) {
            log.error("WHEN CREATE THE OBJECT IS NULL");
            return;
        }
        jdbcTemplate.update(INSERT, createPreparedStatement(migration));
    }

    private SqlParameterSource createPreparedStatement(Migration migration) {
        migration.setId(generateId());
        return new MapSqlParameterSource()
                .addValue("id", migration.getId())
                .addValue("entityName", migration.getEntityName())
                .addValue("total", migration.getTotal())
                .addValue("exported", migration.getExported())
                .addValue("lastRequestUrl", migration.getLastRequestUrl())
                .addValue("status", migration.getStatus().name())
                .addValue("createdDate", migration.getCreatedDate());
    }

    private Long generateId() {
        return jdbcTemplate.queryForObject(GENERATE_ID, EmptySqlParameterSource.INSTANCE, Long.class);
    }

    public List<Migration> getAllMigrations() {
        return jdbcTemplate.queryForList(SELECT_ALL, EmptySqlParameterSource.INSTANCE).stream()
                .map(this::mapToMigration)
                .collect(Collectors.toList());
    }

    private Migration mapToMigration(Map<String, Object> stringObjectMap) {
        Long id = (Long) stringObjectMap.get("id");
        String entityName = (String) stringObjectMap.get("entityName");
        Long total = (Long) stringObjectMap.get("total");
        int exported = (int) stringObjectMap.get("exported");
        String lastRequestUrl = (String) stringObjectMap.get("lastRequestUrl");
        LocalDateTime createdDate = ((Timestamp) stringObjectMap.get("createdDate")).toLocalDateTime();
        Migration.Status status = Migration.Status.valueOf((String) stringObjectMap.get("status"));
        return Migration.builder()
                .id(id)
                .entityName(entityName)
                .total(total)
                .exported(exported)
                .lastRequestUrl(lastRequestUrl)
                .createdDate(createdDate)
                .status(status)
                .build();
    }
}
