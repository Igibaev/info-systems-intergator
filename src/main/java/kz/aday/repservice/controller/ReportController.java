package kz.aday.repservice.controller;

import kz.aday.repservice.model.EntityNameGZ;
import kz.aday.repservice.model.RequestGZ;
import kz.aday.repservice.service.GZService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
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
        model.addAttribute("gzEntities", EntityNameGZ.values());
        return "errorPage";
    }

    @GetMapping("/")
    public String mainPage(Model model) {
        model.addAttribute("gzEntities", EntityNameGZ.values());
        model.addAttribute("migrations", reportService.getAllMigrations());
        return "index";
    }

    @GetMapping("/gos-zakup/export")
    public String gosZakupExport(@RequestParam String entityName, Model model) {
        RequestGZ requestGZ = new RequestGZ();
        requestGZ.setUrl("v3/" + entityName);
        model.addAttribute("entity", entityName);
        model.addAttribute("request", requestGZ);
        return "goszakup";
    }

    @GetMapping("/gos-zakup/migrate") //
    public String gosZakupMigrate(@RequestParam String entityName, Model model) {
        RequestGZ requestGZ = new RequestGZ();
        requestGZ.setUrl("v3/" + entityName);
        model.addAttribute("entity", entityName);
        model.addAttribute("request", requestGZ);
        return "goszakup-migrate";
    }

    @PostMapping("/gos-zakup/migrate")
    public String gosZakupMigrate(@RequestParam String entityName, @ModelAttribute RequestGZ request) {
        request.setGzEntityName(entityName);
        reportService.startMigration(request);
//        executorService.submit(() -> reportService.startMigration(request));
        return "redirect:/";
    }

    @PostMapping("/gos-zakup/export")
    public ResponseEntity<StreamingResponseBody> gosZakupExport(@RequestParam String entityName, @ModelAttribute RequestGZ request) {
        request.setGzEntityName(entityName);
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
