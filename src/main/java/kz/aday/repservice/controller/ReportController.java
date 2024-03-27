package kz.aday.repservice.controller;

import kz.aday.repservice.model.EntityMigration;
import kz.aday.repservice.model.EntityMigrationDto;
import kz.aday.repservice.model.Migration;
import kz.aday.repservice.model.RequestGZ;
import kz.aday.repservice.service.GZService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Slf4j
@Controller
public class ReportController {
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd_MM_yyyy");
    private final GZService reportService;

    public ReportController(GZService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/error")
    public String error(Model model) {
        return "errorPage";
    }

    @GetMapping("/")
    public String mainPage(Model model) {
        model.addAttribute("gzEntities", reportService.getAllEntityMigrations());
        model.addAttribute("migrations", reportService.getAllMigrations());
        return "index";
    }

    @GetMapping("/gos-zakup/manual-export")
    public String gosZakupManualExport(Model model) {
        RequestGZ requestGZ = new RequestGZ();
        EntityMigrationDto form = new EntityMigrationDto(new ArrayList<>(), new EntityMigration());
        model.addAttribute("form", form);
        model.addAttribute("request", requestGZ);
        return "goszakup-manual";
    }

    @GetMapping("/gos-zakup/export")
    public String gosZakupExport(@RequestParam Long entityId, Model model) {
        RequestGZ requestGZ = new RequestGZ();
        EntityMigration entityMigration = reportService.getEntityMigrationById(entityId);
        requestGZ.setUrl(entityMigration.getUrl());
        requestGZ.setToken(entityMigration.getToken());
        model.addAttribute("entity", entityId);
        model.addAttribute("request", requestGZ);
        return "goszakup";
    }

    @GetMapping("/gos-zakup/migrate") //
    public String gosZakupMigrate(@RequestParam Long entityId, Model model) {
        RequestGZ requestGZ = new RequestGZ();
        EntityMigration entityMigration = reportService.getEntityMigrationById(entityId);
        requestGZ.setUrl(entityMigration.getUrl());
        requestGZ.setToken(entityMigration.getToken());
        model.addAttribute("entity", entityId);
        model.addAttribute("request", requestGZ);
        return "goszakup-migrate";
    }

    @GetMapping("/gos-zakup/delete") //
    public String gosZakupDelete(@RequestParam Long entityId) {
        reportService.deleteEntityMigration(entityId);
        return "redirect:/";
    }

    @PostMapping("/gos-zakup/manual-export")
    public String gosZakupManualExport(@ModelAttribute EntityMigrationDto form, @ModelAttribute RequestGZ request, Model model) {
        form.setFields(reportService.getGozEntityFields(request));
        EntityMigration entityMigration = new EntityMigration();
        entityMigration.setToken(request.getToken());
        entityMigration.setUrl(request.getUrl());
        entityMigration.setName(request.getGzEntityName());
        form.setEntityMigration(entityMigration);
        model.addAttribute("form", form);
        return "goszakup-manual-start";
    }

    @PostMapping("/gos-zakup/manual-export-start")
    public String gosZakupManualExportStart(@ModelAttribute EntityMigrationDto form, Model model) {
        model.addAttribute("form", form);
        if (reportService.existEntityMigration(form.getEntityMigration())) {
            model.addAttribute(
                    "error",
                    "Сущность с таким наименованием таблицы уже существует"
            );
            return "errorPage";
        }
        reportService.saveEntityMigration(form);
        return "redirect:/";
    }

    @PostMapping("/gos-zakup/migrate")
    public String gosZakupMigrate(@RequestParam Long entityId, @ModelAttribute RequestGZ request, Model model) {
        EntityMigration entityMigration = reportService.getEntityMigrationById(entityId);
        request.setUrl(entityMigration.getUrl());
        request.setToken(entityMigration.getToken());
        request.setGzEntityName(entityMigration.getName());
        if (reportService.existMigration(request)) {
            model.addAttribute(
                    "error",
                    "Миграция на данную сщность уже существует со статусом IN_PROGRESS "
            );
            return "errorPage";
        }
        long countInProgress = reportService.getAllMigrations().stream()
                .filter(migration -> migration.getStatus() == Migration.Status.IN_PROGRESS)
                .count();
        if (countInProgress > 5) {
            model.addAttribute("error", "Превышено кол-во одноверменный миграции, повторите запрос позднее");
            return "errorPage";
        }
        executorService.submit(() -> reportService.startMigration(request));
        return "redirect:/";
    }

    @PostMapping("/gos-zakup/export")
    public ResponseEntity<StreamingResponseBody> gosZakupExport(@RequestParam Long entityId, @ModelAttribute RequestGZ request) {
        EntityMigration entityMigration = reportService.getEntityMigrationById(entityId);
        request.setUrl(entityMigration.getUrl());
        request.setToken(entityMigration.getToken());
        request.setGzEntityName(entityMigration.getName());
        log.info("Send request to goszakup [{}]", request);
        // Set the content type and file name headers
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=" + createFilename(request));
        headers.add("Content-Type", "application/octet-stream;");        // Return the response entity with the body and headers
        return ResponseEntity.ok()
                .headers(headers)
                .body(reportService.createReport(request));
    }

    private String createFilename(RequestGZ request) {
        if (request.getDateFrom() != null && request.getDateTo() != null) {
            return String.format("\"%s_%s-%s.%s\"",
                    request.getGzEntityName(),
                    simpleDateFormat.format(request.getDateFrom()),
                    simpleDateFormat.format(request.getDateTo()),
                    request.getReportType().getFileExtension()
            );
        } else {
            return String.format("\"%s_%s.%s\"",
                    request.getGzEntityName(),
                    request.getSize(),
                    request.getReportType().getFileExtension()
            );
        }

    }

    @GetMapping("/talday/export")
    public String taldayExport(@RequestParam String entityName, Model model) {
        model.addAttribute("entity", entityName);
        model.addAttribute("request", new RequestGZ());
        return "talday";
    }

    @GetMapping("/talday/migrate") //
    public String gtaldayMigrate(Model model) {
        model.addAttribute("request", new RequestGZ());
        return "talday-migrate";
    }

}
