package kz.aday.repservice.talday;

import kz.aday.repservice.api.TaldayApi;
import kz.aday.repservice.talday.model.Passport;
import kz.aday.repservice.talday.model.Stat;
import kz.aday.repservice.talday.model.StatCombination;
import kz.aday.repservice.talday.model.StatData;
import kz.aday.repservice.talday.model.StatDataRequest;
import kz.aday.repservice.talday.model.StatFilter;
import kz.aday.repservice.talday.model.StatFilterRequest;
import kz.aday.repservice.talday.model.StatInfo;
import kz.aday.repservice.talday.model.StatMeasure;
import kz.aday.repservice.talday.model.StatPeriod;
import kz.aday.repservice.talday.model.StatSegment;
import kz.aday.repservice.talday.model.Stats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TaldayApiService {
    private final TaldayApi taldayApi;
    private final TaldaySaverRepository taldayApiRepository;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final ConcurrentHashMap<Long, Long> queue = new ConcurrentHashMap<>();

    public TaldayApiService(TaldayApi taldayApi, TaldaySaverRepository taldayApiRepository) {
        this.taldayApi = taldayApi;
        this.taldayApiRepository = taldayApiRepository;
    }

    public void startMigration() {
        log.info("Start migration");
//        migrateStartPeriods();
//        migrateStats();
//        migrateStatInfos();
//        migrateStatMeasures();
//        migrateStatSegments();
//        migrateStatCombinations();
//        migrateStatFilters();
        Long periodId = 7L;
        boolean isStarted = false;
        int counter = 0;
        while (true) {
            if (queue.isEmpty()) {
                counter = 0;
            }
            if (!queue.isEmpty() && counter >= 10) {
                try {
                    log.info("sleep for 100s");
                    Thread.sleep(100000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                continue;
            }
            Set<Long> stats = taldayApiRepository.getAvaliableStatsToImport(periodId, 10);
            if (stats.isEmpty()) {
                log.info("ALL DATA MIGRATE STOP cycle EXECUTORS");
                break;
            }
            for (Long statId: stats) {
                taldayApiRepository.markAsBusy(periodId, statId);
                queue.put(statId, statId);
            }
            executorService.submit(() -> {
                migrateStatData(periodId, stats);
            });
            counter++;
        }
    }

    private void migrateStatData(Long periodId, Set<Long> stats) {
        try {
            log.info("Migrate statData" + Thread.currentThread().getName());
            log.info("Start migration for period:[{}]:statsCount:{}", periodId, stats.size());
            log.info("Start to load stats:[{}]", stats);
            for (Long statId : stats) {
                if (isAlreadyImportedStat(periodId, statId)) {
                    log.info("Skip periodId:{} statId:{}", periodId, statId);
                    continue;
                } else {
                    log.info("Save status for periodId:{} statId:{}", periodId, statId);
                    taldayApiRepository.saveStatMigrationStatus(periodId, statId);
                }
                log.info("Start migration for period:[{}]:statId:{}", periodId, statId);
                String measureId = Optional.ofNullable(taldayApi.getStatInfoByStatId(statId).block()).map(StatInfo::getMeasureId).orElse("");
                if (measureId.isEmpty()) {
                    log.error("MeasureId is null, skip statId:{}", statId);
                    continue;
                } else {
                    log.info("Found measureId:{}", measureId);
                }

                List<StatSegment> statSegments = taldayApi.getStatSegmentsByPeriodIdAndStatId(periodId, statId).block();
                if (statSegments == null || statSegments.isEmpty()) {
                    log.error("StatSegments is empty or null, skip statId:{}", statId);
                    continue;
                }
                log.info("Found statSegments:count={}", statSegments.size());
                for (StatSegment statSegment : statSegments) {
                    log.info("Segment:[{}],[{}]", statSegment.getTermIds(), statSegment.getNames());
                    List<StatData> filteredStatData = getStatDataWithFilters(periodId, statId, statSegment, measureId);
                    log.info("Found data with filters:{}", filteredStatData.size());
                    taldayApiRepository.saveAllStatData(filteredStatData);
                    log.info("Save all statData");
                }
                taldayApiRepository.saveStatMigrationStatus(periodId, statId);
                taldayApiRepository.markAsNotBusy(periodId, statId);
                log.info("Mark as not busy", periodId, statId);
                log.info("End migration for period:[{}]:statId:{}", periodId, statId);
            }
            log.info("End migration for period:[{}]", periodId);
            log.info("Migrate statData");
        } catch (Exception e) {
            log.error("FAIL TO IMPORT {}\n {}", stats, e);
        } finally {
            taldayApiRepository.markAsNotBusy(periodId, stats);
            stats.forEach(st -> queue.remove(st));
        }

    }

    private boolean isAvaliableToImport(Long periodId, Long statId) {
        return taldayApiRepository.isAvaliableToImport(periodId, statId);
    }

    private boolean isAlreadyImportedStat(Long periodId, Long statId) {
        Long total = taldayApiRepository.getStatMigrationTotal(periodId, statId);
        return total > 0;
    }

    private List<StatData> getStatDataWithFilters(Long statPeriodId, Long statId, StatSegment statSegment, String measureId) {
        List<StatData> filteredStatData = new ArrayList<>();
        StatDataRequest statDataRequest = StatDataRequest.builder()
                .statPeriodId(statPeriodId)
                .statId(statId)
                .statMeasureId(measureId)
                .firstTermId(statSegment.getFirstTermId())
                .dicIds(statSegment.getDicIdsSeparatedBy(","))
                .segmentTermIds(statSegment.getTermIds())
                .build();
        String[] dicIds = statDataRequest.getDicIds().split(",");
        for (int i = 1; i <= statSegment.getTermIdsCount(); i++) {
            String dic = i >= dicIds.length ? dicIds[i-1] : dicIds[i];
            List<StatFilter> statFilters = taldayApi.getFiltersByStatCombination(statPeriodId, statId, statDataRequest.getDicIds(), dic);
            if (statFilters.isEmpty()) {
                log.info("StatFilters by termIds:{} not found, skip", statSegment.getTermIds());
                continue;
            }
            for (StatFilter statFilter: statFilters) {
                statDataRequest.setFound(false);
                statDataRequest.setFilterId(statFilter.getId());
                statDataRequest.setParentId(null);
                statDataRequest.setFilterTermIds(statSegment.getTermIdsReplacedByOrderAndNewTermId(i, statFilter));
                List<StatData> statDataList = new ArrayList<>();
                try {
                    statDataList = getAllStatData(taldayApi.getStatDataByStatDataRequest(statDataRequest).block(), statDataRequest);
                } catch (Exception e) {
                    log.info(
                            "catch error:{} skip \n periodId:{} statId:{}:filterId:{}:{}",
                            e.getMessage(),
                            statPeriodId,
                            statId,
                            statFilter.getId(),
                            statFilter.getText()
                    );
                    continue;
                }
                if (statDataRequest.isFound()) {
                    log.info("Found in db data by filter:{} count:{}", statFilter.getText(), statDataList.size());
                    continue;
                }
                statDataList.forEach(statData -> {
                    statData.setStatPeriodId(statPeriodId);
                    statData.setStatId(statId);
                    statData.setTermIds(statSegment.getTermIds());
                    statData.setDicIds(statSegment.getDicIdsSeparatedBy(","));
                    statData.setStatFilterId(statFilter.getId());
                    statData.setFilterTermIds(statDataRequest.getFilterTermIds());
                    statData.setStatMeasureId(measureId);
                });
                if (statDataList == null) {
                    log.error("StatData is null return empty list");
                    return new ArrayList<>();
                }
                filteredStatData.addAll(statDataList);
                log.info("By filter:{} foundData:{} termIds:{}", statFilter.getText(), statDataList.size(), statDataRequest.getFilterTermIds());
            }
        }
        return filteredStatData;
    }

    private void migrateStatFilters() {
        log.info("Migrate statFilters");
        Map<Long, Set<Long>> statPeriodsStatIdMap = taldayApiRepository.getAllStatPeriods();
        long counter = 0;
        for (StatPeriod statPeriod : taldayApiRepository.getAllPeriods()) {
            log.info("Start migration statFilters for period:[{}]:statsCount:{}", statPeriod.getText(), statPeriodsStatIdMap.getOrDefault(statPeriod.getId(), new HashSet<>()).size());
            for (Long statId : statPeriodsStatIdMap.get(statPeriod.getId())) {
                List<StatSegment> statSegments = taldayApi.getStatSegmentsByPeriodIdAndStatId(statPeriod.getId(), statId).block();
                if (statSegments == null || statSegments.isEmpty()) {
                    log.error("StatSegments is empty or null, skip statId:{}", statId);
                    continue;
                }
                Map<String, Integer> dicIdsIdxMap = statSegments.stream()
                        .collect(Collectors.toMap(statSegment -> statSegment.getDicIdsSeparatedBy(","), statSegment -> statSegment.getIdx()));
                List<StatCombination> statCombinations = taldayApi.getStatCombinationsByPeriodIdAndStatId(statPeriod.getId(), statId).block();
                log.info("Found StatSegments:count={}", statSegments.size());
                log.info("Found statCombinations:count={}", statCombinations.size());
                for (StatCombination statCombination : statCombinations) {
                    log.info("statCombination:[{}],[{}]", statCombination.getId(), statCombination.getText());
                    List<StatFilter> filters = getFiltersByStatCombination(statPeriod.getId(), statId, statCombination, dicIdsIdxMap.get(statCombination.getId()));
                    log.info("Found filters {}", filters.size());
                    taldayApiRepository.saveAllStatFilters(filters);
                    log.info("Save all filters");
                    counter += filters.size();
                }
                log.info("countData:{}", counter);
            }
            log.info("End migration statFilters for period:[{}]:statsCount:{}", statPeriod.getText(), statPeriodsStatIdMap.get(statPeriod.getId()).size());
        }
    }

    private List<StatFilter> getFiltersByStatCombination(
            Long statPeriodId,
            Long statId,
            StatCombination statCombination,
            Integer idx
    ) {
        List<StatFilter> statFiltersAll = new ArrayList<>();
        StatFilterRequest statFilterRequest = StatFilterRequest.builder()
                .statId(statId)
                .statPeriodId(statPeriodId)
                .dicIds(statCombination.getId())
                .idx(idx)
                .build();
        for (String dic: statCombination.getId().split(",")) {
            statFilterRequest.setDic(dic);
            List<StatFilter> statFilters = taldayApi.getFilterListByStatFilterRequest(statFilterRequest).block();
            if (statFilters.isEmpty()) {
                log.info("StatFilters by dicIds:{} dicId:{} not found, skip", statCombination.getId(), dic);
                continue;
            }
            log.info("Filters found by dicIds:{} dicId:{}, count:{}", statCombination.getId(), dic, statFilters.size());
            for (StatFilter statFilter: statFilters) {
                statFilter.setRequestParamsToAll(statPeriodId, statId, statFilterRequest.getDicIds(), statFilterRequest.getDic());
            }
            statFiltersAll.addAll(statFilters);
        }
        return statFiltersAll;
    }

    private void migrateStatCombinations() {
        log.info("migrate statCombinations");
        for (Map.Entry<Long, Set<Long>> periodIdStatIdsMap : taldayApiRepository.getAllStatPeriods().entrySet()) {
            Long periodId = periodIdStatIdsMap.getKey();
            for (Long statId : periodIdStatIdsMap.getValue()) {
                log.info("Get statCombinations:pId:{} sId:{}", periodId, statId);
                List<StatCombination> statCombinations = taldayApi.getStatCombinationsByPeriodIdAndStatId(periodId, statId).block();
                statCombinations.forEach(statCombination -> {
                    statCombination.setStatId(statId);
                    statCombination.setStatPeriodId(periodId);
                });
                long savedCount = taldayApiRepository.saveAllStatCombinations(statCombinations);
                log.info("StatCombination periodId:{} statId:{} count:{} saved", periodId, statId, savedCount);
            }
        }
        log.info("StatCombination migrated");
    }

    private Set<StatSegment> migrateStatSegments() {
        log.info("migrate statSegment");
        Set<StatSegment> allStatSegments = new HashSet<>();
        for (Map.Entry<Long, Set<Long>> periodIdStatIdsMap : taldayApiRepository.getAllStatPeriods().entrySet()) {
            Long periodId = periodIdStatIdsMap.getKey();
            for (Long statId : periodIdStatIdsMap.getValue()) {
                log.info("Get statSegment:pId:{} sId:{}", periodId, statId);
                List<StatSegment> statSegments = taldayApi.getStatSegmentsByPeriodIdAndStatId(periodId, statId).block();
                statSegments.forEach(statSegment -> {
                    statSegment.setStatId(statId);
                    statSegment.setStatPeriodId(periodId);
                });
                allStatSegments.addAll(statSegments);
                for (StatSegment statSegment : statSegments) {
                    if (taldayApiRepository.isStatSegmentNotExist(statSegment)) {
                        taldayApiRepository.saveStatSegment(statSegment);
                        log.info("StatSegment:[pId:{} sId:{} dicId:{} termIds:{}] \nsaved",
                                statSegment.getStatPeriodId(), statSegment.getStatId(), statSegment.getDicId(), statSegment.getTermIds()
                        );
                    } else {
                        log.info("StatSegment:[pId:{} sId:{} dicId:{} termIds:{}] \nalready exist",
                                statSegment.getStatPeriodId(), statSegment.getStatId(), statSegment.getDicId(), statSegment.getTermIds()
                        );
                    }
                }
            }
        }
        log.info("StatSegment migrated");
        return allStatSegments;
    }

    private void migrateStatMeasures() {
        log.info("migrate statMeasures");
        for (StatPeriod statPeriod: taldayApiRepository.getAllPeriods()) {
            for (Stat stat: taldayApi.getStatsByPeriodId(statPeriod.getId()).block().getResults()) {
                StatInfo statInfo = taldayApi.getStatInfoByStatId(stat.getId()).block();
                List<StatMeasure> statMeasureList = taldayApi.getStatMeasures(statInfo.getMeasureId()).block();
                statMeasureList.forEach(statMeasure -> statMeasure.setStatMeasureId(statInfo.getMeasureId()));
                log.info("StatMeasure:{} to save", statMeasureList.size());
                int saved = taldayApiRepository.saveAllStatMeasures(statMeasureList);
                if (saved > 0) {
                    log.info("StatMeasure:{} saved", saved);
                }
            }
        }
        log.info("StatMeasure migrated");
    }

    private void migrateStatInfos() {
        log.info("migrate statInfos");
        for (StatPeriod statPeriod: taldayApiRepository.getAllPeriods()) {
            Stats stats = taldayApi.getStatsByPeriodId(statPeriod.getId()).block();
            for (Stat stat : stats.getResults()) {
                Set<Passport> passports = new HashSet<>();
                StatInfo statInfo = taldayApi.getStatInfoByStatId(stat.getId()).block();
                if (statInfo == null) {
                    log.info("StatInfo is null {}", stat.getId());
                    continue;
                }
                log.info("StatInfo:id:{} to save", statInfo.getId());
                boolean saved = taldayApiRepository.saveStatInfo(statInfo);
                if (saved) {
                    log.info("StatInfo:id:{} saved", statInfo.getId());
                    passports.addAll(statInfo.getPassport().stream()
                            .peek(passport -> passport.setStatId(statInfo.getId()))
                            .collect(Collectors.toList()));
                    log.info("Passport:{} to save", passports.size());
                    int savedRows = taldayApiRepository.saveAllStatInfosPassports(passports);
                    log.info("Passport:{} saved", savedRows);
                } else {
                    log.info("StatInfo:id:{} already saved", statInfo.getId());
                }
            }
        }
        log.info("StatInfos migrated");
    }

    private void migrateStats() {
        log.info("migrate Stats(binded periodId with StatId)");

        Set<Stat> statSet = new HashSet<>();
        for (StatPeriod statPeriod : taldayApiRepository.getAllPeriods()) {
            Stats stats = taldayApi.getStatsByPeriodId(statPeriod.getId()).block();
            stats.setStatPeriodId(statPeriod.getId());
            log.info("Stats:{} to save", stats);
            int saved = taldayApiRepository.saveStatsBindedByPeriod(stats);
            log.info("Stats:{} saved", saved);
            statSet.addAll(stats.getResults());
        }
        log.info("All stats binded to period was saved");

        log.info("migrate stat");
        log.info("Stat:{} to save", statSet.size());
        int saved = taldayApiRepository.saveAllStat(statSet);
        log.info("Stat:{} saved", saved);
    }

    private List<StatPeriod> migrateStartPeriods() {
        log.info("migrate StatPeriod");
        List<StatPeriod> statPeriods = taldayApi.getPeriodsForStat().block();
        log.info("StatPeriod:{} to save", statPeriods.size());
        int saved = taldayApiRepository.saveAllStatPeriods(statPeriods);
        log.info("StatPeriod:{} saved", saved);
        log.info("StatPeriod migrated");
        return statPeriods;
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

//    private List<StatFilter> getAllStatFilterRecursive(List<StatFilter> statFilterList, StatFilterRequest statFilterRequest) {
//        List<StatFilter> statFilters = new ArrayList<>(statFilterList);
//        if (statFilters.isEmpty()) {
//            return statFilters;
//        } else {
//            statFilterList.forEach(statFilter -> statFilter.setParentId(statFilterRequest.getNodeId()));
//            for (StatFilter statFilter : statFilterList) {
//                statFilterRequest.setNodeId(statFilter.getId());
//                if (statFilter.isLeaf()) {
//                    log.info("Filter:{} is leaf, skip inner filters", statFilter.getText());
//                    continue;
//                }
//                List<StatFilter> innerStatFilters = getAllStatFilterRecursive(
//                        taldayApi.getFilterListByStatFilterRequest(statFilterRequest).block(),
//                        statFilterRequest
//                );
//                if (innerStatFilters != null && !innerStatFilters.isEmpty()) {
//                    statFilters.addAll(innerStatFilters);
//                }
//            }
//        }
//        return statFilters;
//    }
}
