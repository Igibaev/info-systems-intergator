package kz.aday.repservice.talday;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.aday.repservice.talday.model.Passport;
import kz.aday.repservice.talday.model.Stat;
import kz.aday.repservice.talday.model.StatCombination;
import kz.aday.repservice.talday.model.StatData;
import kz.aday.repservice.talday.model.StatFilter;
import kz.aday.repservice.talday.model.StatInfo;
import kz.aday.repservice.talday.model.StatMeasure;
import kz.aday.repservice.talday.model.StatPeriod;
import kz.aday.repservice.talday.model.StatSegment;
import kz.aday.repservice.talday.model.Stats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class TaldaySaverRepository {
    private String SAVE_STATE_PERIODS = "INSERT INTO stat_period (id, text) VALUES (:id, :text);";
    private String EXIST_STATE_PERIOD = "SELECT count(*) > 0 FROM stat_period WHERE id = :id;";

    private String SAVE_STATES_BY_PERIOD_ID =
            "INSERT INTO stats_periods (stat_period_id, stat_id) " +
                    "VALUES (:statPeriodId, :statId);";
    private String EXIST_STATES_BY_PERIOD_ID =
            "SELECT count(*) > 0 FROM stats_periods " +
                    "WHERE stat_period_id = :statPeriodId " +
                    "AND stat_id = :statId;";
    private String SELECT_STATES_BY_PERIOD = "SELECT stat_period_id, stat_id FROM stats_periods;";

    private String SAVE_STAT = "INSERT INTO stat (id, name, code, info) VALUES (:id, :name, :code, :info);";
    private String EXIST_STAT = "SELECT count(*) > 0 FROM stat WHERE id = :id;";

    private String SAVE_STAT_INFOS =
            "INSERT INTO stat_info (id, name_path, name, short_name, full_code, cg_params, term_names,\n" +
                    "                       measure_id, measure_kfc, measure_sign, measure_name, preferred_measure_id,\n" +
                    "                       preferred_measure_name, preferred_measure_kfc, preferred_measure_sign)\n" +
                    "VALUES (:id, :name_path, :name, :short_name, :full_code, :cg_params, :term_names,\n" +
                    "        :measure_id, :measure_kfc, :measure_sign, :measure_name, :preferred_measure_id,\n" +
                    "        :preferred_measure_name, :preferred_measure_kfc, :preferred_measure_sign)\n";
    private String EXIST_STAT_INFOS = "SELECT count(*) > 0 FROM stat_info WHERE id = :id;";

    private String SAVE_STAT_INFOS_PASSPORT = "INSERT INTO stat_info_passport (stat_id, title, value) VALUES (:stat_id, :title, :value);";
    private String EXIST_STAT_INFOS_PASSPORT = "SELECT count(*) > 0 FROM stat_info_passport WHERE stat_id = :id AND title = :title AND value = :value;";

    private String SAVE_STAT_MEASURES =
            "INSERT INTO stat_measure(id, text, kfc, sign, leaf, expand, stat_measure_id)\n" +
                    "    VALUES (:id, :text, :kfc, :sign, :leaf, :expand, :stat_measure_id)";
    private String EXIST_STAT_MEASURES = "SELECT count(*) > 0 FROM stat_measure WHERE id = :id;";

    private String SAVE_STAT_SEGMENTS =
            "INSERT INTO stat_segment (dic_id, dic_class_id, names, full_names, term_ids, term_names, dic_count, idx, id, segment_order,\n" +
                    "                          dec_format, keyword_dic, max_date, stat_period_id, stat_id)\n" +
                    "VALUES (:dicId, :dicClassId, :names, :fullNames, :termIds, :termNames, :dicCount, :idx, :id, :segment_order, :decFormat,\n" +
                    "        :keywordDic, :maxDate, :statPeriodId, :statId)\n";
    private String EXIST_STAT_SEGMENTS =
            "SELECT count(*) > 0 FROM stat_segment " +
                    "WHERE stat_period_id = :statPeriodId " +
                    "AND stat_id = :statId " +
                    "AND dic_id = :dicId " +
                    "AND term_ids = :termIds ";

    private String SAVE_STAT_COMBINATIONS =
            "INSERT INTO stat_combinations (dic_count, full_text, id, text, stat_period_id, stat_id)\n" +
            "VALUES (:dicCount, :fullText, :id, :text, :statPeriodId, :statId)\n";
    private String EXIST_STAT_COMBINATIONS =
            "SELECT count(*) > 0 FROM stat_combinations " +
                    "WHERE stat_period_id = :statPeriodId " +
                    "AND stat_id = :statId " +
                    "AND id = :id ";

    private String SAVE_STAT_FILTERS =
            "INSERT INTO stat_filters (id, parent_id, text, stat_period_id, stat_id, dic_ids, dic)\n" +
            "VALUES (:id, :parentId, :text, :statPeriodId, :statId, :dicIds, :dic) ON CONFLICT DO NOTHING;\n";
    private String EXIST_STAT_FILTERS =
            "SELECT count(*) > 0\n" +
                    "FROM stat_filters\n " +
                    "WHERE id = :id\n " +
                    "AND stat_period_id = :statPeriodId\n " +
                    "AND stat_id = :statId\n " +
                    "AND dic_ids = :dicIds\n" +
                    "AND dic = :dic";

    private String SAVE_STAT_DATA =
            "INSERT INTO stat_data (id, rownum, text, leaf, expanded, measure_name, date_data_map, parent_id,\n" +
            "                       stat_id, stat_period_id, stat_measure_id, stat_segment_term_ids, stat_segment_dic_ids,\n" +
            "                       stat_filter_id, stat_filter_term_ids)\n" +
            "VALUES (:id, :rownum, :text, :leaf, :expanded, :measure_name, :date_data_map, :parent_id,\n" +
            "        :stat_id, :stat_period_id, :stat_measure_id, :stat_segment_term_ids, :stat_segment_dic_ids,\n" +
            "        :stat_filter_id, :stat_filter_term_ids) ON CONFLICT DO NOTHING;";
    private String EXIST_STAT_DATA =
            "SELECT count(*) > 0 FROM stat_data\n" +
                    "WHERE id = :id\n" +
                    "AND stat_id = :statId\n" +
                    "AND stat_period_id = :statPeriodId\n" +
                    "AND stat_measure_id = :statMeasureId\n" +
                    "AND stat_segment_term_ids = :termIds\n" +
                    "AND stat_segment_dic_ids = :dicIds\n " +
                    "AND stat_filter_id = coalesce(:statFilterId, stat_filter_id)\n " +
                    "AND stat_filter_term_ids = coalesce(:statFilterTermIds, stat_filter_term_ids) ";


    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TaldaySaverRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveAllStatData(List<StatData> statDataList) {
        if (statDataList.isEmpty()) {
            return;
        }

        List<SqlParameterSource> parameterSourceList = new ArrayList<>();
        for (StatData statData : statDataList) {
            parameterSourceList.add(
                    new MapSqlParameterSource()
                            .addValue("id", statData.getId())
                            .addValue("rownum", statData.getRownum())
                            .addValue("text", statData.getText())
                            .addValue("leaf", statData.isLeaf())
                            .addValue("expanded", statData.isExpanded())
                            .addValue("measure_name", statData.getMeasureName())
                            .addValue("stat_id", statData.getStatId())
                            .addValue("stat_period_id", statData.getStatPeriodId())
                            .addValue("stat_measure_id", statData.getStatMeasureId())
                            .addValue("stat_segment_term_ids", statData.getTermIds())
                            .addValue("stat_segment_dic_ids", statData.getDicIds())
                            .addValue("stat_filter_id", statData.getStatFilterId())
                            .addValue("stat_filter_term_ids", statData.getFilterTermIds())
                            .addValue("parent_id", statData.getParentId())
                            .addValue("date_data_map", convertToJson(statData))
            );
        }
        jdbcTemplate.batchUpdate(SAVE_STAT_DATA, parameterSourceList.toArray(new SqlParameterSource[0]));
    }

    private String convertToJson(StatData statData) {
        try {
            return objectMapper.writeValueAsString(statData.getDateDataMap());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isStatDataNotExist(StatData statData) {
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("id", statData.getId())
                .addValue("statId", statData.getStatId())
                .addValue("statPeriodId", statData.getStatPeriodId())
                .addValue("statMeasureId", statData.getStatMeasureId())
                .addValue("statFilterTermIds", statData.getFilterTermIds())
                .addValue("statFilterId", statData.getStatFilterId())
                .addValue("termIds", statData.getTermIds())
                .addValue("dicIds", statData.getDicIds());
        return !Boolean.TRUE.equals(jdbcTemplate.queryForObject(EXIST_STAT_DATA, sqlParameterSource, boolean.class));
    }

    public void saveAllStatFilters(List<StatFilter> filters) {
        filters = filterAlreadyExistStatFilters(filters);

        if (filters.isEmpty()) {
            return;
        }
        List<SqlParameterSource> parameterSourceList = new ArrayList<>();

        parameterSourceList.addAll(getAllParametersSource(filters));

        jdbcTemplate.batchUpdate(SAVE_STAT_FILTERS, parameterSourceList.toArray(new SqlParameterSource[0]));
    }

    private Collection<? extends SqlParameterSource> getAllParametersSource(List<StatFilter> filters) {
        List<SqlParameterSource> parameterSourceList = new ArrayList<>();
        filters.forEach(statFilter -> {
            parameterSourceList.add(new MapSqlParameterSource()
                    .addValue("id", statFilter.getId())
                    .addValue("text", statFilter.getText())
                    .addValue("parentId", statFilter.getParentId())
                    .addValue("statPeriodId", statFilter.getStatPeriodId())
                    .addValue("statId", statFilter.getStatId())
                    .addValue("dicIds", statFilter.getDicIds())
                    .addValue("dic", statFilter.getDicId()));
            parameterSourceList.addAll(getAllParametersSource(statFilter.getChildren()));
        });
        return parameterSourceList;
    }

    private List<StatFilter> filterAlreadyExistStatFilters(List<StatFilter> filters) {
        List<StatFilter> notExistFilters = new ArrayList<>();
        for (StatFilter statFilter: Optional.ofNullable(filters).orElse(new ArrayList<>())) {
            if (isStatFilterNotExist(statFilter)) {
                notExistFilters.add(statFilter);
            }
            notExistFilters.addAll(filterAlreadyExistStatFilters(statFilter.getChildren()));
        }
        return notExistFilters;
    }


    public boolean isStatFilterNotExist(StatFilter statFilter) {
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("id", statFilter.getId())
                .addValue("statPeriodId", statFilter.getStatPeriodId())
                .addValue("statId", statFilter.getStatId())
                .addValue("dicIds", statFilter.getDicIds())
                .addValue("dic", statFilter.getDicId());
        return !Boolean.TRUE.equals(jdbcTemplate.queryForObject(EXIST_STAT_FILTERS, sqlParameterSource, boolean.class));
    }

    public long saveAllStatCombinations(List<StatCombination> statCombinations) {
        statCombinations = statCombinations.stream()
                .filter(statCombination -> isStatCombinationNotExist(statCombination))
                .collect(Collectors.toList());
        List<SqlParameterSource> parameterSourceList = new ArrayList<>();
        for (StatCombination statCombination : statCombinations) {
            parameterSourceList.add(
                    new MapSqlParameterSource()
                            .addValue("dicCount", statCombination.getDicCount())
                            .addValue("fullText", statCombination.getFullText())
                            .addValue("text", statCombination.getText())
                            .addValue("id", statCombination.getId())
                            .addValue("statPeriodId", statCombination.getStatPeriodId())
                            .addValue("statId", statCombination.getStatId())
            );
        }
        jdbcTemplate.batchUpdate(SAVE_STAT_COMBINATIONS, parameterSourceList.toArray(new SqlParameterSource[0]));
        return statCombinations.size();
    }

    private boolean isStatCombinationNotExist(StatCombination statCombination) {
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("id", statCombination.getId())
                .addValue("statPeriodId", statCombination.getStatPeriodId())
                .addValue("statId", statCombination.getStatId());
        return !Boolean.TRUE.equals(jdbcTemplate.queryForObject(EXIST_STAT_COMBINATIONS, sqlParameterSource, boolean.class));
    }

    public void saveAllStatSegments(List<StatSegment> statSegments) {
        statSegments = statSegments.stream()
                .filter(statSegment -> isStatSegmentNotExist(statSegment))
                .collect(Collectors.toList());
        List<SqlParameterSource> parameterSourceList = new ArrayList<>();
        for (StatSegment statSegment : statSegments) {
            parameterSourceList.add(
                    new MapSqlParameterSource()
                            .addValue("dicId", statSegment.getDicId())
                            .addValue("dicClassId", statSegment.getDicClassId())
                            .addValue("names", statSegment.getNames())
                            .addValue("fullNames", statSegment.getFullNames())
                            .addValue("termIds", statSegment.getTermIds())
                            .addValue("termNames", statSegment.getTermNames())
                            .addValue("dicCount", statSegment.getDicCount())
                            .addValue("idx", statSegment.getIdx())
                            .addValue("id", statSegment.getId())
                            .addValue("segment_order", statSegment.getOrder())
                            .addValue("decFormat", statSegment.getDecFormat())
                            .addValue("keywordDic", statSegment.getKeywordDic())
                            .addValue("maxDate", statSegment.getMaxDate())
                            .addValue("statPeriodId", statSegment.getStatPeriodId())
                            .addValue("statId", statSegment.getStatId())
            );
        }
        jdbcTemplate.batchUpdate(SAVE_STAT_SEGMENTS, parameterSourceList.toArray(new SqlParameterSource[0]));

    }

    public void saveStatSegment(StatSegment statSegment) {
        if (!isStatSegmentNotExist(statSegment)) {
            return;
        }
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("dicId", statSegment.getDicId())
                .addValue("dicClassId", statSegment.getDicClassId())
                .addValue("names", statSegment.getNames())
                .addValue("fullNames", statSegment.getFullNames())
                .addValue("termIds", statSegment.getTermIds())
                .addValue("termNames", statSegment.getTermNames())
                .addValue("dicCount", statSegment.getDicCount())
                .addValue("idx", statSegment.getIdx())
                .addValue("id", statSegment.getId())
                .addValue("segment_order", statSegment.getOrder())
                .addValue("decFormat", statSegment.getDecFormat())
                .addValue("keywordDic", statSegment.getKeywordDic())
                .addValue("maxDate", statSegment.getMaxDate())
                .addValue("statPeriodId", statSegment.getStatPeriodId())
                .addValue("statId", statSegment.getStatId());
        jdbcTemplate.update(SAVE_STAT_SEGMENTS, sqlParameterSource);
    }

    public boolean isStatSegmentNotExist(StatSegment statSegment) {
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("statPeriodId", statSegment.getStatPeriodId())
                .addValue("statId", statSegment.getStatId())
                .addValue("dicId", statSegment.getDicId())
                .addValue("termIds", statSegment.getTermIds());
        return !Boolean.TRUE.equals(jdbcTemplate.queryForObject(EXIST_STAT_SEGMENTS, sqlParameterSource, boolean.class));
    }

    public int saveAllStatMeasures(Collection<StatMeasure> statMeasures) {
        statMeasures = statMeasures.stream()
                .filter(this::isStatMeasuresNotExist)
                .collect(Collectors.toSet());
        List<SqlParameterSource> parameterSourceList = new ArrayList<>();
        for (StatMeasure statMeasure : statMeasures) {
            parameterSourceList.add(
                    new MapSqlParameterSource()
                            .addValue("stat_measure_id", statMeasure.getStatMeasureId())
                            .addValue("id", statMeasure.getId())
                            .addValue("text", statMeasure.getText())
                            .addValue("kfc", statMeasure.getKfc())
                            .addValue("sign", statMeasure.getSign())
                            .addValue("leaf", statMeasure.getLeaf())
                            .addValue("expand", statMeasure.getExpand())
            );
        }
        jdbcTemplate.batchUpdate(SAVE_STAT_MEASURES, parameterSourceList.toArray(new SqlParameterSource[0]));
        return statMeasures.size();
    }

    private boolean isStatMeasuresNotExist(StatMeasure statMeasure) {
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("id", statMeasure.getId());
        return !Boolean.TRUE.equals(jdbcTemplate.queryForObject(EXIST_STAT_MEASURES, sqlParameterSource, boolean.class));
    }

    public int saveAllStatInfosPassports(Set<Passport> passports) {
        passports = passports.stream()
                .filter(this::isStatInfoPassportNotExist)
                .collect(Collectors.toSet());

        List<SqlParameterSource> parameterSourceList = new ArrayList<>();
        for (Passport passport : passports) {
            parameterSourceList.add(
                    new MapSqlParameterSource()
                            .addValue("stat_id", passport.getStatId())
                            .addValue("value", passport.getValue())
                            .addValue("title", passport.getTitle())
            );
        }
        jdbcTemplate.batchUpdate(SAVE_STAT_INFOS_PASSPORT, parameterSourceList.toArray(new SqlParameterSource[0]));
        return passports.size();
    }

    private boolean isStatInfoPassportNotExist(Passport passport) {
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("id", passport.getStatId())
                .addValue("title", passport.getTitle())
                .addValue("value", passport.getValue());
        return !Boolean.TRUE.equals(jdbcTemplate.queryForObject(EXIST_STAT_INFOS_PASSPORT, sqlParameterSource, boolean.class));
    }

    private StatInfo mapToStatInfo(Map<String, Object> map) {
        Long id = (Long) map.get("id");
        String namePath = (String) map.get("name_path");
        String name = (String) map.get("name");
        String shortName = (String) map.get("short_name");
        String fullCode = (String) map.get("full_code");
        String cgParams = (String) map.get("cg_params");
        String termNames = (String) map.get("term_names");
        String measureId = (String) map.get("measure_id");
        String measureKfc = (String) map.get("measure_kfc");
        String measureSign = (String) map.get("measure_sign");
        String measureName = (String) map.get("measure_name");
        String preferredMeasureId = (String) map.get("preferred_measure_id");
        String preferredMeasureName = (String) map.get("preferred_measure_name");
        String preferredMeasureKfc = (String) map.get("preferred_measure_kfc");
        String preferredMeasureSign = (String) map.get("preferred_measure_sign");
        List<Passport> passport =
                jdbcTemplate.queryForList(
                                "SELECT stat_id, title, value FROM stat_info_passport WHERE stat_id = :statId;",
                                new MapSqlParameterSource().addValue("statId", id)
                        )
                        .stream()
                        .map(this::mapToPassport)
                        .collect(Collectors.toList());
        return StatInfo.builder()
                .id(id)
                .namePath(namePath)
                .name(name)
                .shortName(shortName)
                .fullCode(fullCode)
                .cgParams(cgParams)
                .termNames(termNames)
                .passport(passport)
                .measureId(measureId)
                .measureKfc(measureKfc)
                .measureSign(measureSign)
                .measureName(measureName)
                .preferredMeasureId(preferredMeasureId)
                .preferredMeasureName(preferredMeasureName)
                .preferredMeasureKfc(preferredMeasureKfc)
                .preferredMeasureSign(preferredMeasureSign)
                .build();
    }

    private Passport mapToPassport(Map<String, Object> map) {
        Long id = (Long) map.get("stat_id");
        String title = (String) map.get("title");
        String value = (String) map.get("value");
        return new Passport(id, title, value);
    }

    public boolean saveStatInfo(StatInfo statInfo) {
        if (!isStatInfoNotExist(statInfo.getId())) {
            return false;
        }
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("id", statInfo.getId())
                .addValue("name_path", statInfo.getNamePath())
                .addValue("name", statInfo.getName())
                .addValue("short_name", statInfo.getShortName())
                .addValue("full_code", statInfo.getFullCode())
                .addValue("cg_params", statInfo.getCgParams())
                .addValue("term_names", statInfo.getTermNames())
                .addValue("measure_id", statInfo.getMeasureId().toString())
                .addValue("measure_kfc", statInfo.getMeasureKfc().toString())
                .addValue("measure_sign", statInfo.getMeasureSign())
                .addValue("measure_name", statInfo.getMeasureName())
                .addValue("preferred_measure_id", statInfo.getPreferredMeasureId().toString())
                .addValue("preferred_measure_name", statInfo.getPreferredMeasureName())
                .addValue("preferred_measure_kfc", statInfo.getPreferredMeasureKfc().toString())
                .addValue("preferred_measure_sign", statInfo.getPreferredMeasureSign());
        jdbcTemplate.update(SAVE_STAT_INFOS, sqlParameterSource);
        return true;
    }

    public void saveAllStatInfos(List<StatInfo> statInfos) {
        statInfos = statInfos.stream()
                .filter(statInfo -> isStatInfoNotExist(statInfo.getId()))
                .collect(Collectors.toList());
        List<SqlParameterSource> parameterSourceList = new ArrayList<>();
        for (StatInfo statInfo : statInfos) {
            parameterSourceList.add(
                    new MapSqlParameterSource()
                            .addValue("id", statInfo.getId())
                            .addValue("name_path", statInfo.getNamePath())
                            .addValue("name", statInfo.getName())
                            .addValue("short_name", statInfo.getShortName())
                            .addValue("full_code", statInfo.getFullCode())
                            .addValue("cg_params", statInfo.getCgParams())
                            .addValue("term_names", statInfo.getTermNames())
                            .addValue("measure_id", statInfo.getMeasureId().toString())
                            .addValue("measure_kfc", statInfo.getMeasureKfc().toString())
                            .addValue("measure_sign", statInfo.getMeasureSign())
                            .addValue("measure_name", statInfo.getMeasureName())
                            .addValue("preferred_measure_id", statInfo.getPreferredMeasureId().toString())
                            .addValue("preferred_measure_name", statInfo.getPreferredMeasureName())
                            .addValue("preferred_measure_kfc", statInfo.getPreferredMeasureKfc().toString())
                            .addValue("preferred_measure_sign", statInfo.getPreferredMeasureSign())
            );
        }
        jdbcTemplate.batchUpdate(SAVE_STAT_INFOS, parameterSourceList.toArray(new SqlParameterSource[0]));
    }

    public boolean isStatInfoNotExist(Long id) {
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource().addValue("id", id);
        return !Boolean.TRUE.equals(jdbcTemplate.queryForObject(EXIST_STAT_INFOS, sqlParameterSource, boolean.class));
    }

    public int saveAllStat(Collection<Stat> collection) {
        collection = collection.stream()
                .filter(this::isStatNotExist)
                .collect(Collectors.toList());

        List<SqlParameterSource> parameterSourceList = new ArrayList<>();
        for (Stat stat : collection) {
            parameterSourceList.add(
                    new MapSqlParameterSource()
                            .addValue("id", stat.getId())
                            .addValue("name", stat.getName())
                            .addValue("code", stat.getCode())
                            .addValue("info", stat.getInfo())
            );
        }
        jdbcTemplate.batchUpdate(SAVE_STAT, parameterSourceList.toArray(new SqlParameterSource[0]));
        return collection.size();
    }

    private boolean isStatNotExist(Stat stat) {
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource().addValue("id", stat.getId());
        return !Boolean.TRUE.equals(jdbcTemplate.queryForObject(EXIST_STAT, sqlParameterSource, boolean.class));
    }

    public int saveAllStatPeriods(List<StatPeriod> statPeriods) {
        statPeriods = statPeriods.stream()
                .filter(this::isStatPeriodNotExist)
                .collect(Collectors.toList());

        List<SqlParameterSource> parameterSourceList = new ArrayList<>();
        for (StatPeriod statPeriod : statPeriods) {
            parameterSourceList.add(
                    new MapSqlParameterSource()
                            .addValue("id", statPeriod.getId())
                            .addValue("text", statPeriod.getText())
            );
        }
        jdbcTemplate.batchUpdate(SAVE_STATE_PERIODS, parameterSourceList.toArray(new SqlParameterSource[0]));
        return statPeriods.size();
    }

    private boolean isStatPeriodNotExist(StatPeriod statPeriod) {
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource().addValue("id", statPeriod.getId());
        return !Boolean.TRUE.equals(jdbcTemplate.queryForObject(EXIST_STATE_PERIOD, sqlParameterSource, boolean.class));
    }

    public int saveStatsBindedByPeriod(Stats stats) {
        List<Stat> statsThatDoesNotExist = stats.getResults().stream()
                .filter(stat -> isStatBindedByPeriodNotExist(stat, stats.getStatPeriodId()))
                .collect(Collectors.toList());
        List<SqlParameterSource> parameterSourceList = new ArrayList<>();
        for (Stat stat : statsThatDoesNotExist) {
            parameterSourceList.add(
                    new MapSqlParameterSource()
                            .addValue("statPeriodId", stats.getStatPeriodId())
                            .addValue("statId", stat.getId())
            );
        }
        jdbcTemplate.batchUpdate(SAVE_STATES_BY_PERIOD_ID, parameterSourceList.toArray(new SqlParameterSource[0]));
        return statsThatDoesNotExist.size();
    }

    private boolean isStatBindedByPeriodNotExist(Stat stat, Long statPeriodId) {
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("statPeriodId", statPeriodId)
                .addValue("statId", stat.getId());
        return !Boolean.TRUE.equals(jdbcTemplate.queryForObject(EXIST_STATES_BY_PERIOD_ID, sqlParameterSource, boolean.class));
    }

    public void saveStatMigrationStatus(Long statPeriodId, Long statId) {
        String query;
        SqlParameterSource parameterSource;
        if (isExistStatMigrationStatus(statPeriodId, statId)) {
            query = "UPDATE stat_migration_status s\n" +
                    "SET total = (SELECT count(*) FROM stat_data sd where (sd.stat_period_id, sd.stat_id) = (:periodId, :statId))\n" +
                    "WHERE (s.period_id, s.stat_id) = (:periodId, :statId);";
            parameterSource = new MapSqlParameterSource()
                    .addValue("periodId", statPeriodId)
                    .addValue("statId", statId);
        } else {
            query = "INSERT INTO stat_migration_status (period_id, stat_id, total) " +
                    "VALUES (:periodId, :statId, (SELECT count(*) FROM stat_data sd where (sd.stat_period_id, sd.stat_id) = (:periodId, :statId)));";
            parameterSource = new MapSqlParameterSource()
                    .addValue("periodId", statPeriodId)
                    .addValue("statId", statId);
        }
        jdbcTemplate.update(query, parameterSource);
    }

    private boolean isExistStatMigrationStatus(Long periodId, Long statId) {
        String query = "SELECT count(*) > 0\n" +
                "FROM stat_migration_status s\n" +
                "WHERE (s.period_id, s.stat_id) = (:periodId, :statId)";
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("periodId", periodId)
                .addValue("statId", statId);
        return jdbcTemplate.queryForObject(query, parameterSource, Boolean.class);
    }

    public Long getStatMigrationTotal(Long periodId, Long statId) {
        String query = "SELECT total\n" +
                "FROM stat_migration_status s\n" +
                "WHERE (s.period_id, s.stat_id) = (:periodId, :statId)\n" +
                "LIMIT 1";
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("periodId", periodId)
                .addValue("statId", statId);
        for (Map<String, Object> map: jdbcTemplate.queryForList(query, parameterSource)) {
            Long total = (Long) map.get("total");
            return total;
        }
        return 0L;
    }

    public Map<Long, Set<Long>> getAllStatPeriods() {
        Map<Long, Set<Long>> periodStatIdSetMap = new HashMap<>();

        for (Map<String, Object> map : jdbcTemplate.queryForList(SELECT_STATES_BY_PERIOD, EmptySqlParameterSource.INSTANCE)) {
            Long periodId = (Long) map.get("stat_period_id");
            Long statId = (Long) map.get("stat_id");
            Set<Long> statIds = periodStatIdSetMap.getOrDefault(periodId, new HashSet<>());
            statIds.add(statId);
            periodStatIdSetMap.put(periodId, statIds);
        }

        return periodStatIdSetMap;
    }

    public List<StatPeriod> getAllPeriods() {
        String query = "SELECT id, text FROM stat_period";
        return jdbcTemplate.queryForList(query, EmptySqlParameterSource.INSTANCE).stream()
                .map(this::mapToStatPeriod)
                .collect(Collectors.toList());
    }

    private StatPeriod mapToStatPeriod(Map<String, Object> map) {
        Long id = (Long) map.get("id");
        String text = (String) map.get("text");
        return new StatPeriod(id, text);
    }

    public ConcurrentMap<Long, String> getAllStatInfos() {
        String query = "SELECT id, measure_id FROM stat_info";
        ConcurrentMap<Long, String> conMap = new ConcurrentHashMap<>();
        for (Map<String, Object> map: jdbcTemplate.queryForList(query, EmptySqlParameterSource.INSTANCE)) {
            Long statId = (Long) map.get("id");
            String measureId = (String) map.get("measure_id");
            conMap.put(statId, measureId);
        }
        return conMap;
    }

    public ConcurrentMap<String, List<StatSegment>> getAllStatSegmentsMap() {
        String query = "SELECT s.dic_id, dic_class_id, names, full_names, term_ids, term_names, dic_count, idx, id, " +
                "segment_order, dec_format, keyword_dic, max_date, stat_period_id, stat_id FROM stat_segment s ";
        ConcurrentMap<String, List<StatSegment>> conMap = new ConcurrentHashMap<>();
        for (Map<String, Object> map: jdbcTemplate.queryForList(query, EmptySqlParameterSource.INSTANCE)) {
            StatSegment statSegment = mapToStatSegment(map);
            String key = String.format("%s-%s", statSegment.getStatPeriodId(), statSegment.getStatId());
            List<StatSegment> statSegments = conMap.getOrDefault(key, new ArrayList<>());
            statSegments.add(statSegment);
            conMap.put(key, statSegments);
        }
        return conMap;
    }

    private StatSegment mapToStatSegment(Map<String, Object> map) {
        String dic_id = (String) map.get("dic_id");
        String dic_class_id = (String) map.get("dic_class_id");
        String names = (String) map.get("names");
        String full_names = (String) map.get("full_names");
        String term_ids = (String) map.get("term_ids");
        String term_names = (String) map.get("term_names");
        Integer dic_count = (Integer) map.get("dic_count");
        Integer idx = (Integer) map.get("idx");
        Integer id = (Integer) map.get("id");
        Integer segment_order = (Integer) map.get("segment_order");
        Integer dec_format = (Integer) map.get("dec_format");
        String keyword_dic = (String) map.get("keyword_dic");
        String max_date = (String) map.get("max_date");
        Long stat_period_id = (Long) map.get("stat_period_id");
        Long stat_id = (Long) map.get("stat_id");
        return StatSegment.builder()
                .dicId(dic_id)
                .dicClassId(dic_class_id)
                .names(names)
                .fullNames(full_names)
                .termIds(term_ids)
                .termNames(term_names)
                .dicCount(dic_count)
                .id(id)
                .idx(idx)
                .order(segment_order)
                .decFormat(dec_format)
                .keywordDic(keyword_dic)
                .maxDate(max_date)
                .statPeriodId(stat_period_id)
                .statId(stat_id)
                .build();
    }

    public boolean isAvaliableToImport(Long periodId, Long statId) {
        String query = "SELECT count(*) = 0\n" +
                "FROM stat_migration_status s\n" +
                "WHERE (s.period_id, s.stat_id) = (:periodId, :statId)\n";
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("periodId", periodId)
                .addValue("statId", statId);
        return jdbcTemplate.queryForObject(query, parameterSource, Boolean.class);
    }

    public void markAsBusy(Long periodId, Long statId) {
        String query = "INSERT INTO stat_migration_busy (period_id, stat_id) VALUES (:periodId, :statId)";
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("periodId", periodId)
                .addValue("statId", statId);
        jdbcTemplate.update(query, parameterSource);
    }

    public void markAsNotBusy(Long periodId, Set<Long> statIds) {
        String query = "DELETE FROM stat_migration_busy s WHERE s.period_id = :periodId AND s.stat_id IN (:statIds)";
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("periodId", periodId)
                .addValue("statIds", statIds);
        jdbcTemplate.update(query, parameterSource);
    }

    public void markAsNotBusy(Long periodId, Long statId) {
        String query = "DELETE FROM stat_migration_busy s WHERE (period_id, stat_id) = (:periodId, :statId)";
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("periodId", periodId)
                .addValue("statId", statId);
        jdbcTemplate.update(query, parameterSource);
    }

    public Set<Long> getAvaliableStatsToImport(Long periodId, int limit) {
        String query = "SELECT stat_id FROM stats_periods\n" +
                "WHERE stat_period_id = :periodId\n" +
                "AND stat_id NOT IN (SELECT sm.stat_id FROM stat_migration_status sm WHERE sm.total > 0 AND sm.period_id = :periodId)\n" +
                "AND stat_id NOT IN (SELECT sm.stat_id FROM stat_migration_busy sm WHERE sm.period_id = :periodId)\n" +
                "LIMIT 5\n";
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("periodId", periodId);
        Set<Long> stats  = new HashSet<>();
        for (Map<String, Object> map: jdbcTemplate.queryForList(query, parameterSource)) {
            Long statId = (Long) map.get("stat_id");
            if (statId != null) {
                stats.add(statId);
            }
        }
        return stats;
    }
}
