package kz.aday.repservice.talday;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.aday.repservice.talday.model.Passport;
import kz.aday.repservice.talday.model.Stat;
import kz.aday.repservice.talday.model.StatCombination;
import kz.aday.repservice.talday.model.StatData;
import kz.aday.repservice.talday.model.StatDataRequest;
import kz.aday.repservice.talday.model.StatFilter;
import kz.aday.repservice.talday.model.StatInfo;
import kz.aday.repservice.talday.model.StatMeasure;
import kz.aday.repservice.talday.model.StatPeriod;
import kz.aday.repservice.talday.model.StatSegment;
import kz.aday.repservice.talday.model.Stats;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class TaldayGetterRepository {
    private String SELECT_STATES_BY_PERIOD = "SELECT stat_period_id, stat_id FROM stats_periods;";
    private String SELECT_STAT_PERIOD = "SELECT id, text FROM stat_period;";
    private String SELECT_STATS =
            "SELECT s.*, sp.stat_period_id FROM stats_periods sp\n" +
                    "JOIN stat s ON sp.stat_id = s.id\n" +
                    "WHERE sp.stat_period_id = :periodId";
    private String SELECT_STAT_INFO =
            "SELECT id,\n" +
                    "       name_path,\n" +
                    "       name,\n" +
                    "       short_name,\n" +
                    "       full_code,\n" +
                    "       cg_params,\n" +
                    "       term_names,\n" +
                    "       measure_id,\n" +
                    "       measure_kfc,\n" +
                    "       measure_sign,\n" +
                    "       measure_name,\n" +
                    "       preferred_measure_id,\n" +
                    "       preferred_measure_name,\n" +
                    "       preferred_measure_kfc,\n" +
                    "       preferred_measure_sign\n" +
                    "FROM stat_info s\n" +
                    "WHERE s.id = :id";
    private String SELECT_STAT_SEGMENTS =
            "SELECT dic_id,\n" +
                    "       dic_class_id,\n" +
                    "       names,\n" +
                    "       full_names,\n" +
                    "       term_ids,\n" +
                    "       term_names,\n" +
                    "       dic_count,\n" +
                    "       idx,\n" +
                    "       id,\n" +
                    "       segment_order,\n" +
                    "       dec_format,\n" +
                    "       keyword_dic,\n" +
                    "       max_date,\n" +
                    "       stat_period_id,\n" +
                    "       stat_id\n" +
                    "FROM stat_segment \n" +
                    "WHERE stat_period_id = :statPeriodId \n" +
                    "AND stat_id = :statId";

    private String SELECT_STAT_MEASURES =
            "SELECT stat_measure_id, id, text, kfc, sign, leaf, expand\n" +
                    "FROM stat_measure\n" +
                    "WHERE stat_measure_id = :measureId;";

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TaldayGetterRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<StatPeriod> getAllStatPeriods() {
        List<StatPeriod> statPeriods = new ArrayList<>();

        for (Map<String, Object> map : jdbcTemplate.queryForList(SELECT_STAT_PERIOD, EmptySqlParameterSource.INSTANCE)) {
            Long id = (Long) map.get("id");
            String text = (String) map.get("text");
            statPeriods.add(new StatPeriod(id, text));
        }

        return statPeriods;
    }

    public Map<Long, Set<Long>> getAllBindedStatPeriods() {
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

    public Stats getStatsByPeriod(Long periodId) {
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("periodId", periodId);
        return mapToStats(jdbcTemplate.queryForList(SELECT_STATS, parameterSource));
    }

    private Stats mapToStats(List<Map<String, Object>> maps) {
        List<Stat> stats = new ArrayList<>();
        Long periodId = -1L;
        for (Map<String, Object> map : maps) {
            periodId = (Long) map.get("stat_period_id");
            Long statId = (Long) map.get("id");
            String name = (String) map.get("name");
            String code = (String) map.get("code");
            String info = (String) map.get("info");
            stats.add(new Stat(statId, name, code, info));
        }
        return new Stats(periodId, stats.size() + "", stats);
    }

    public StatInfo getStatInfoByStatId(Long statId) {
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("id", statId);
        List<StatInfo> statInfos = jdbcTemplate.queryForList(SELECT_STAT_INFO, parameterSource).stream()
                .map(this::mapToStatInfo)
                .collect(Collectors.toList());
        return statInfos.isEmpty() ? null : statInfos.get(0);
    }

    public List<StatSegment> getAllStatSegmentsByPeriodIdAndStatId(Long periodId, Long statId) {
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("statId", statId)
                .addValue("statPeriodId", periodId);
        return jdbcTemplate.queryForList(SELECT_STAT_SEGMENTS, parameterSource).stream()
                .map(this::mapToStatSegment)
                .collect(Collectors.toList());
    }

    public List<StatMeasure> getAllMeasuresByMeasureId(String measureId) {
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("measureId", measureId);
        return jdbcTemplate.queryForList(SELECT_STAT_MEASURES, parameterSource).stream()
                .map(this::mapToStatMeasure)
                .collect(Collectors.toList());
    }

    private StatMeasure mapToStatMeasure(Map<String, Object> map) {
        String statMeasureId = (String) map.get("stat_measure_id");
        String id = (String) map.get("id");
        String text = (String) map.get("text");
        Long kfc = (Long) map.get("kfc");
        String sign = (String) map.get("sign");
        Boolean leaf = (Boolean) map.get("leaf");
        Boolean expand = (Boolean) map.get("expand");
        return StatMeasure.builder()
                .statMeasureId(statMeasureId)
                .id(id)
                .text(text)
                .kfc(kfc)
                .sign(sign)
                .leaf(leaf)
                .expand(expand)
                .build();
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

    public List<StatFilter> getAllStatFiltersBy(Long statPeriodId, Long statId, String dicIds, String dic) {
        String query = "SELECT * FROM stat_filters s\n" +
                "WHERE s.stat_period_id = :statPeriodId\n" +
                "  AND s.stat_id = :statId\n" +
                "  AND s.dic_ids = :dicIds\n" +
                "  AND s.dic = :dic\n";
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("statPeriodId", statPeriodId)
                .addValue("statId", statId)
                .addValue("dicIds", dicIds)
                .addValue("dic", dic);
        return jdbcTemplate.queryForList(query, parameterSource).stream()
                .map(this::mapToStatFilter)
                .collect(Collectors.toList());
    }

    private StatFilter mapToStatFilter(Map<String, Object> map) {
        Long id = (Long) map.get("id");
        Long parentId = (Long) map.get("parent_id");
        String text = (String) map.get("text");
        String dicIds = (String) map.get("dic_ids");
        String dicId = (String) map.get("dic");
        Long statPeriodId = (Long) map.get("stat_period_id");
        Long statId = (Long) map.get("stat_id");
        return new StatFilter(id, parentId, text, statPeriodId, statId, dicIds, dicId);
    }

    public List<StatFilter> getAllStatFiltersByStatCombination(Long statPeriodId, Long statId, String dicIds, String dic) {
        String query = "SELECT * FROM stat_filters s\n " +
                "WHERE s.stat_period_id = :statPeriodId\n" +
                "AND s.stat_id = :statId\n" +
                "AND s.dic_ids = :dicIds\n" +
                "AND s.dic = :dic\n";
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("statPeriodId", statPeriodId)
                .addValue("statId", statId)
                .addValue("dicIds", dicIds)
                .addValue("dic", dic);
        return jdbcTemplate.queryForList(query, parameterSource).stream()
                .map(this::mapToStatFilter)
                .collect(Collectors.toList());
    }

    public List<StatCombination> getAllStatCombinationsByPeriodIdAndStatId(Long periodId, Long statId) {
        String query = "SELECT dic_count, full_text, id, text, stat_period_id, stat_id FROM stat_combinations " +
                "WHERE stat_period_id = :statPeriodId AND stat_id = :statId";
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("statPeriodId", periodId)
                .addValue("statId", statId);
        return jdbcTemplate.queryForList(query, parameterSource).stream()
                .map(this::mapToStatCombinations)
                .collect(Collectors.toList());
    }

    private StatCombination mapToStatCombinations(Map<String, Object> map) {
        String id = (String) map.get("id");
        String fullText = (String) map.get("full_text");
        String text = (String) map.get("text");
        Long statPeriodId = (Long) map.get("stat_period_id");
        Long statId = (Long) map.get("stat_id");
        int dicCount = (int) map.get("dic_count");
        return new StatCombination(dicCount, fullText, id, text, statPeriodId, statId);
    }

    public List<StatData> getAllStatDataByStatDataRequest(StatDataRequest request) {
        String query = "SELECT id, rownum, text, leaf, expanded, measure_name, stat_id, \n" +
                "       stat_period_id, stat_measure_id, stat_segment_term_ids, \n" +
                "       stat_segment_dic_ids, stat_filter_id, stat_filter_term_ids, \n" +
                "       parent_id, date_data_map FROM stat_data s\n" +
                "WHERE s.stat_id = :statId\n" +
                "AND s.stat_period_id = :statPeriodId\n" +
                "AND s.stat_measure_id = :statMeasureId\n" +
                "AND s.stat_filter_term_ids = :statFilterTermIds\n" +
                "AND s.stat_filter_id = :statFilterId\n" +
                "AND s.stat_segment_term_ids = :termIds\n" +
                "AND s.stat_segment_dic_ids = :dicIds\n" +
                "AND s.parent_id = coalesce(:parentId, parent_id)";
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("statId", request.getStatId())
                .addValue("statPeriodId", request.getStatPeriodId())
                .addValue("statMeasureId", request.getStatMeasureId())
                .addValue("statFilterTermIds", request.getFilterTermIds())
                .addValue("statFilterId", request.getFilterId())
                .addValue("termIds", request.getSegmentTermIds())
                .addValue("parentId", request.getParentId())
                .addValue("dicIds", request.getDicIds());
        return jdbcTemplate.queryForList(query, parameterSource).stream()
                .map(this::mapToStatData)
                .collect(Collectors.toList());
    }

    private StatData mapToStatData(Map<String, Object> map) {
        String id = (String) map.get("id");
        String rownum = (String) map.get("rownum");
        String text = (String) map.get("text");
        boolean leaf = (boolean) map.get("leaf");
        boolean expanded = (boolean) map.get("expanded");
        String measureName = (String) map.get("measure_Name");
        Map<String, String> dateDataMap = convertToDataMap((String) map.get("date_Data_Map"));
        String parentId = (String) map.get("parent_id");
        Long statId = (Long) map.get("stat_id");
        Long statPeriodId = (Long) map.get("stat_period_id");
        Long statFilterId = (Long) map.get("stat_filter_id");
        String termIds = (String) map.get("stat_segment_term_ids");
        String dicIds = (String) map.get("stat_segment_dic_ids");
        String filterTermIds = (String) map.get("stat_filter_term_ids");
        String statMeasureId = (String) map.get("stat_measure_id");
        return new StatData(id, rownum, text,leaf,expanded,measureName,dateDataMap, parentId, statId, statPeriodId,
                statFilterId, termIds, dicIds, filterTermIds, statMeasureId);
    }

    private Map<String, String> convertToDataMap(String dateDataMap) {
        try {
            return objectMapper.readValue(dateDataMap, HashMap.class);
        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
