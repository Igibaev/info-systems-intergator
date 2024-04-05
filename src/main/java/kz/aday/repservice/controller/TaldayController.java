package kz.aday.repservice.controller;

import kz.aday.repservice.talday.TaldayApiMigrationService;
import kz.aday.repservice.talday.TaldayReportService;
import kz.aday.repservice.talday.model.Stat;
import kz.aday.repservice.talday.model.StatData;
import kz.aday.repservice.talday.model.StatDataForMigration;
import kz.aday.repservice.talday.model.StatFilter;
import kz.aday.repservice.talday.model.StatInfo;
import kz.aday.repservice.talday.model.StatMeasure;
import kz.aday.repservice.talday.model.StatSegment;
import kz.aday.repservice.talday.model.dto.TaldayRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Controller
public class TaldayController {
    private final TaldayReportService taldayReportService;
    private final TaldayApiMigrationService taldayApiMigrationService;

    public TaldayController(TaldayReportService taldayReportService,
                            TaldayApiMigrationService taldayApiMigrationService) {
        this.taldayReportService = taldayReportService;
        this.taldayApiMigrationService = taldayApiMigrationService;
    }

    @GetMapping("/talday-global-migration-status")
    public String globalMigrationStatus(Model model) {
        model.addAttribute("statsCount", taldayApiMigrationService.getNotMigratedStatsCount());
        return "talday-global-migrations-status";
    }

    @GetMapping("/talday-start-global-migration")
    public String startGlobalMigration(Model model) {
        taldayApiMigrationService.startMigration();
        model.addAttribute("statsCount", 0);
        return "talday-global-migrations-status";
    }

    @GetMapping("/talday")
    public String statPeriods(Model model) {
        model.addAttribute("statPeriods", taldayReportService.getStatPeriods());
        model.addAttribute("request", new TaldayRequest());
        model.addAttribute("stats", new ArrayList<>());
        return "talday";
    }

    @PostMapping("/talday-stats")
    public String getStats(@ModelAttribute TaldayRequest request, Model model) {
        List<Stat> statList = taldayReportService.getStatsByStatPeriodId(request.getStatPeriodId());
        model.addAttribute("statPeriods", List.of(request.getStatPeriod()));
        model.addAttribute("request", request);
        model.addAttribute("stats", statList);
        return "talday-stats";
    }

    @PostMapping("/talday-segments")
    public String getStatSegments(@ModelAttribute TaldayRequest request, Model model) {
        List<StatSegment> statSegments = taldayReportService.getStatSegments(request.getStatPeriodId(), request.getStatId());
        if (request.getStatInfoJson() == null) {
            StatInfo statInfo = taldayReportService.getStatInfo(request.getStatId());
            request.setStatInfoJson(statInfo.toJson());
        }
        model.addAttribute("request", request);
        model.addAttribute("statPeriods", List.of(request.getStatPeriod()));
        model.addAttribute("stats", request.getStat());
        model.addAttribute("statInfo", request.getStatInfo());
        model.addAttribute("statSegments", statSegments);

        return "talday-segments";
    }

    @PostMapping("talday-segments-filters")
    public String statsStatSegments(@ModelAttribute TaldayRequest request, Model model) {
        List<StatFilter> statFilters = taldayReportService.getStatFilters(
                request.getStatSegment(), request.getStatPeriodId(), request.getStatId());
        if (request.getStatInfoJson() == null) {
            StatInfo statInfo = taldayReportService.getStatInfo(request.getStatId());
            request.setStatInfoJson(statInfo.toJson());
        }
        model.addAttribute("request", request);
        model.addAttribute("statPeriods", List.of(request.getStatPeriod()));
        model.addAttribute("stats", request.getStat());
        model.addAttribute("statInfo", request.getStatInfo());
        model.addAttribute("statSegments", List.of(request.getStatSegment()));
        model.addAttribute("statFilters", statFilters);
        model.addAttribute("statData", List.of(new Stat()));
        List<StatMeasure> statMeasures = taldayReportService.getStatMeasures(request);
        model.addAttribute("statMeasures", statMeasures);
        return "talday-segments-filters";
    }

    @PostMapping("talday-stat-data")
    public String statDataMigrate(@ModelAttribute TaldayRequest request, Model model) {
        if (request.getStatInfoJson() == null) {
            StatInfo statInfo = taldayReportService.getStatInfo(request.getStatId());
            request.setStatInfoJson(statInfo.toJson());
        }
        List<StatData> statDataList = taldayReportService.saveAndGetStatDataByRequest(request);
        model.addAttribute("request", request);
        model.addAttribute("statPeriods", List.of(request.getStatPeriod()));
        model.addAttribute("stats", request.getStat());
        model.addAttribute("statInfo", request.getStatInfo());
        model.addAttribute("statSegments", List.of(request.getStatSegment()));
        if (request.getIsFiltered()) {
            model.addAttribute("statFilters", List.of(request.getStatFilter()));
        }
        model.addAttribute("statData", statDataList);
        if (statDataList.isEmpty()) {
            return "talday-segments-filters";
        }
        List<StatDataForMigration> migrations = taldayReportService.saveToTable(statDataList, request, request.getTableName());
        model.addAttribute("statDataCount", migrations.size());
        return "talday-stat-data-migrate-start";
    }
}
