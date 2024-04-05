package kz.aday.repservice.talday;

import com.fasterxml.jackson.databind.JsonNode;
import kz.aday.repservice.api.TaldayApi;
import kz.aday.repservice.service.SqlWriter;
import kz.aday.repservice.talday.model.Stat;
import kz.aday.repservice.talday.model.StatData;
import kz.aday.repservice.talday.model.StatDataForMigration;
import kz.aday.repservice.talday.model.StatDataRequest;
import kz.aday.repservice.talday.model.StatFilter;
import kz.aday.repservice.talday.model.StatFilterRequest;
import kz.aday.repservice.talday.model.StatInfo;
import kz.aday.repservice.talday.model.StatMeasure;
import kz.aday.repservice.talday.model.StatPeriod;
import kz.aday.repservice.talday.model.StatSegment;
import kz.aday.repservice.talday.model.Stats;
import kz.aday.repservice.talday.model.dto.TaldayRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class TaldayReportService {
    private final TaldayApi taldayApi;
    private final TaldaySaverRepository taldayApiRepository;

    public TaldayReportService(TaldayApi taldayApi, TaldaySaverRepository taldayApiRepository) {
        this.taldayApi = taldayApi;
        this.taldayApiRepository = taldayApiRepository;
    }

    public List<StatData> saveAndGetStatDataByRequest(TaldayRequest request) {
        Long statPeriodId = request.getStatPeriodId();
        Long statId = request.getStatId();
        StatInfo statInfo = request.getStatInfo();
        StatSegment statSegment = request.getStatSegment();
        List<StatData> statDataList;
        if (statPeriodId != null && statId != null && statInfo != null && statSegment != null) {
            if (request.getIsFiltered()) {
                StatFilter statFilter = request.getStatFilter();
                statDataList = getStatDataWithFilters(statPeriodId, statId, statSegment, statInfo.getMeasureId(), statFilter);

            } else {
                statDataList = getStatDataWithoutFilters(statPeriodId, statId, statSegment, statInfo.getMeasureId());
            }
        } else {
            throw new RuntimeException(
                    String.format(
                            "One of statPeriodId:%s, statId:%s, statInfo:%s, statSegment:%s is null",
                            statPeriodId, statId, statInfo, statSegment
                    )
            );
        }
        log.info("Found data:{}", statDataList.size());
        taldayApiRepository.saveAllStatData(statDataList);
        return statDataList;
    }

    public List<StatFilter> getStatFilters(StatSegment statSegment, Long periodId, Long statId) {
        log.info("Saving stat filter...");
        List<StatFilter> filterList = getFiltersByStatCombination(periodId, statId, statSegment.getDicIdsSeparatedBy(","), statSegment.getIdx());
        taldayApiRepository.saveAllStatFilters(filterList);
        log.info("Save all filters");
        log.info("Saved");
        return filterList;
    }

    public List<StatSegment> getStatSegments(Long periodId, Long statId) {
        log.info("Saving stat segments...");
        List<StatSegment> statSegments = taldayApi.getStatSegmentsByPeriodIdAndStatId(periodId, statId).block();
        statSegments.forEach(statSegment -> {
            statSegment.setStatId(statId);
            statSegment.setStatPeriodId(periodId);
        });
//        for (StatSegment statSegment : statSegments) {
//            if (taldayApiRepository.isStatSegmentNotExist(statSegment)) {
//                taldayApiRepository.saveStatSegment(statSegment);
//            }
//        }
//        log.info("Saved");
        return statSegments;
    }

    public List<StatMeasure> getStatMeasures(TaldayRequest request) {
        String statIndexHtmlPage = taldayApi.getStatIndexHtmlPage(request.getStatId()).block();
        if (!statIndexHtmlPage.contains("measure: {")) {
            List<StatMeasure> statMeasures = taldayApi.getStatMeasures(request.getStatInfo().getMeasureId()).block();
            if (statMeasures == null || statMeasures.isEmpty()) {
                StatMeasure statMeasure = new StatMeasure();
                statMeasure.setText(request.getStatInfo().getMeasureName());
                return List.of(statMeasure);
            }
            return statMeasures;
        }
        String startMeasureString = statIndexHtmlPage.substring(statIndexHtmlPage.indexOf("measure: {"));
        String measureString = startMeasureString.substring(0, startMeasureString.indexOf("},"));
        String startMeasureIdString = measureString.substring(measureString.indexOf("id: ") + "id: ".length());
        String extractedMeasureId = startMeasureIdString.substring(0, startMeasureIdString.indexOf(","));
        String startMeasureName = measureString.substring(measureString.indexOf("name: \"") + "name: \"".length());
        String extractedMeasureName = startMeasureName.substring(0, startMeasureName.indexOf("\""));
        List<StatMeasure> statMeasures = taldayApi.getStatMeasures(extractedMeasureId).block();
        if (statMeasures == null || statMeasures.isEmpty()) {
            StatMeasure statMeasure = new StatMeasure();
            statMeasure.setText(extractedMeasureName);
            return List.of(statMeasure);
        }
        return statMeasures;
    }

    public StatInfo getStatInfo(Long statId) {
        log.info("Saving stat info...");
        StatInfo statInfo = taldayApi.getStatInfoByStatId(statId).block();
        if (statInfo == null) {
            return null;
        }
//        if (statInfo.getPassport() == null) {
//            taldayApiRepository.saveStatInfo(statInfo);
//            log.info("Saved");
//            return statInfo;
//        }
        statInfo.getPassport().forEach(si -> si.setStatId(statId));
//        taldayApiRepository.saveStatInfo(statInfo);
//        taldayApiRepository.saveAllStatInfosPassports(new HashSet<>(statInfo.getPassport()));
        log.info("Saved");
        return statInfo;
    }

    public List<StatPeriod> getStatPeriods() {
        log.info("Saving stat periods...");
        List<StatPeriod> statPeriods = taldayApi.getPeriodsForStat().block();
//        taldayApiRepository.saveAllStatPeriods(statPeriods);
//        log.info("Saved");
        return statPeriods;
    }

    public List<Stat> getStatsByStatPeriodId(Long statPeriodId) {
        log.info("Saving stats by period:{}...", statPeriodId);
        Stats stats = taldayApi.getStatsByPeriodId(statPeriodId).block();
        stats.setStatPeriodId(statPeriodId);
//        taldayApiRepository.saveStatsBindedByPeriod(stats);
//        taldayApiRepository.saveAllStat(stats.getResults());
//        log.info("Saved");
        List<Stat> sorts = stats.getResults();
        return sorts;
    }

    private List<StatFilter> getFiltersByStatCombination(
            Long statPeriodId,
            Long statId,
            String dicIds,
            Integer idx
    ) {
        List<StatFilter> statFiltersAll = new ArrayList<>();
        StatFilterRequest statFilterRequest = StatFilterRequest.builder()
                .statId(statId)
                .statPeriodId(statPeriodId)
                .dicIds(dicIds)
                .idx(idx)
                .build();
        for (String dic : dicIds.split(",")) {
            statFilterRequest.setDic(dic);
            List<StatFilter> statFilters = taldayApi.getFilterListByStatFilterRequest(statFilterRequest).block();
            if (statFilters.isEmpty()) {
                log.info("StatFilters by dicIds:{} dicId:{} not found, skip", dicIds, dic);
                continue;
            }
            log.info("Filters found by dicIds:{} dicId:{}, count:{}", dicIds, dic, statFilters.size());
            for (StatFilter statFilter : statFilters) {
                statFilter.setRequestParamsToAll(statPeriodId, statId, statFilterRequest.getDicIds(), statFilterRequest.getDic());
            }
            statFiltersAll.addAll(statFilters);
        }
        return statFiltersAll;
    }

    private List<StatData> getStatDataWithFilters(Long statPeriodId, Long statId, StatSegment statSegment, String measureId, StatFilter statFilter) {
        StatDataRequest statDataRequest = StatDataRequest.builder()
                .statPeriodId(statPeriodId)
                .statId(statId)
                .statMeasureId(measureId)
                .firstTermId(statSegment.getFirstTermId())
                .filterTermIds(statSegment.getTermIds())
                .dicIds(statSegment.getDicIdsSeparatedBy(","))
                .segmentTermIds(statSegment.getTermIds())
                .parentId(null)
                .build();
        statDataRequest.setFilterId(statFilter.getId());
        statDataRequest.setFilterTermIds(statSegment.getTermIdsByOrder(statSegment.getOrder(), statFilter));
        List<StatData> statDataList = getAllStatData(taldayApi.getStatDataByStatDataRequest(statDataRequest).block(), statDataRequest);
        statDataList.forEach(statData -> {
            statData.setStatPeriodId(statPeriodId);
            statData.setStatId(statId);
            statData.setTermIds(statSegment.getTermIds());
            statData.setDicIds(statSegment.getDicIdsSeparatedBy(","));
            statData.setFilterTermIds(statDataRequest.getFilterTermIds());
            statData.setStatMeasureId(measureId);
        });
        return statDataList;
    }

    private List<StatData> getStatDataWithoutFilters(Long statPeriodId,
                                                     Long statId,
                                                     StatSegment statSegment,
                                                     String measureId
    ) {
        StatDataRequest statDataRequest = StatDataRequest.builder()
                .statPeriodId(statPeriodId)
                .statId(statId)
                .statMeasureId(measureId)
                .firstTermId(statSegment.getFirstTermId())
                .filterTermIds(statSegment.getTermIds())
                .dicIds(statSegment.getDicIdsSeparatedBy(","))
                .segmentTermIds(statSegment.getTermIds())
                .parentId(null)
                .build();
        List<StatData> statDataList = getAllStatData(taldayApi.getStatDataByStatDataRequest(statDataRequest).block(), statDataRequest);
        statDataList.forEach(statData -> {
            statData.setStatPeriodId(statPeriodId);
            statData.setStatId(statId);
            statData.setTermIds(statSegment.getTermIds());
            statData.setDicIds(statSegment.getDicIdsSeparatedBy(","));
            statData.setFilterTermIds(statDataRequest.getFilterTermIds());
            statData.setStatMeasureId(measureId);
        });
        return statDataList;
    }

    private List<StatData> getAllStatData(List<StatData> statDataList, StatDataRequest request) {
        List<StatData> allStatData = new ArrayList<>(statDataList);
        if (statDataList.isEmpty()) {
            return statDataList;
        } else {
            statDataList.forEach(statData -> statData.setParentId(request.getParentId()));
            for (StatData statData : statDataList) {
                request.setParentId(statData.getId());
                List<StatData> innerStatData = getAllStatData(
                        taldayApi.getStatDataByStatDataRequest(request).block(),
                        request
                );
                if (innerStatData != null && !innerStatData.isEmpty()) {
                    allStatData.addAll(innerStatData);
                }
            }
        }
        return allStatData;
    }

    public List<StatDataForMigration> saveToTable(List<StatData> statDataList, TaldayRequest request, String tableName) {
        List<StatDataForMigration> statDataForMigrationList = new ArrayList<>();
        StatPeriod statPeriod = request.getStatPeriod();
        Stat stat = request.getStat();
        StatInfo statInfo = request.getStatInfo();
        StatSegment statSegment = request.getStatSegment();
        StatMeasure statMeasure = request.getStatMeasure();
        StatFilter statFilter = request.getStatFilter();
        for (StatData statData : statDataList) {
            for (Map.Entry<String, String> statDataMap : statData.getDateDataMap().entrySet()) {
                if (statDataMap.getKey().startsWith("y")) {
                    StatDataForMigration statDataForMigration = StatDataForMigration.builder()
                            .statMeasureName(statMeasure.getText())
                            .statDataText(statData.getText())
                            .statName(stat.getName())
                            .statValue(convertValueByMeasure(statDataMap.getValue(), statMeasure, statInfo))
                            .statRawValue(statDataMap.getValue())
                            .statDate(convertToTextDate(statDataMap.getKey(), statPeriod))
                            .build();
                    if (request.getIsFiltered() && request.getStatFilter() != null) {
                        statDataForMigration.setFilterName(statFilter.getText());
                    }
                    statDataForMigrationList.add(statDataForMigration);
                }
            }
        }
        try (StringWriter stringWriter = new StringWriter()) {
            SqlWriter sqlWriter = new SqlWriter(stringWriter, true, false);
            List<Map<String, JsonNode>> rows = new ArrayList<>();
            statDataForMigrationList.forEach(st -> rows.add(st.row()));
            sqlWriter.writeHeaders(List.of(StatDataForMigration.headers), tableName);
            sqlWriter.writeRows(rows);
            sqlWriter.finish();
            taldayApiRepository.executeQuery(stringWriter.toString());
        } catch (IOException e) {
            throw new RuntimeException("Серверная ошибка при сохранение в базу \n" + e.getMessage());
        }
        return statDataForMigrationList;
    }

    private String convertValueByMeasure(String value, StatMeasure statMeasure, StatInfo statInfo) {
        String kfc = statMeasure.getKfc() != null ? statMeasure.getKfc().toString() : statInfo.getPreferredMeasureKfc();
        try {
            Double convertedValue = Double.valueOf(value);
            Double convertedKfc = Double.valueOf(kfc);
            BigDecimal numerator = new BigDecimal(convertedValue);
            BigDecimal denominator = new BigDecimal(convertedKfc);
            BigDecimal result = numerator.divide(denominator);
            return result.toPlainString();
        } catch (Exception e) {
            log.warn("IGNORE on stat:{} values:{}, kfc:{} return empty string", statInfo.getId(), value,kfc);
            return "";
        }
    }

    private String convertToTextDate(String key, StatPeriod statPeriod) {
        key = key.substring(1);
        String month = key.substring(0, 2);
        String year = key.substring(2);
        if (statPeriod.getText().equalsIgnoreCase("Год")) {
            return year + " год";
        } else {
            return Month.of(Integer.parseInt(month)).getDisplayName(TextStyle.FULL_STANDALONE, Locale.forLanguageTag("ru")) +
                    " " +
                    year + " год";
        }
    }
}
