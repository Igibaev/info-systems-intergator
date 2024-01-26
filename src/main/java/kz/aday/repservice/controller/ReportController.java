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


@Slf4j
@Controller
public class ReportController {
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
        return "index";
    }

    @GetMapping("/gos-zakup/export")
    public String gosZakupExport(@RequestParam String entityName, Model model) {
        model.addAttribute("entity", entityName);
        model.addAttribute("request", new RequestGZ());
        return "goszakup";
    }

    @GetMapping("/gos-zakup/migrate") //
    public String gosZakupMigrate(Model model) {
        model.addAttribute("request", new RequestGZ());
        return "goszakup-migrate";
    }

    @PostMapping("/gos-zakup/export")
    public ResponseEntity<StreamingResponseBody> gosZakupExport(@RequestParam String entityName, @ModelAttribute RequestGZ request) {
        request.setGzEntityName(entityName);
        log.info("Send request to goszakup [{}]", request);
        // Set the content type and file name headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.add("Content-Disposition", "attachment; filename=" + createFilename(request));
        // Return the response entity with the body and headers
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

//    @PostMapping("/convertToJava")
//    public ResponseEntity<String> convertToJavaClass(
//            String javaClassName,
//            HttpEntity<String> httpEntity
//    ) {
//
//        log.info("Send request to goszakup [{},\n{}]", javaClassName, httpEntity.getBody());
//        String outputDir = "/Users/aigibaev/Desktop/bakhtiyar/rep-service/akimat/src/main/java/kz/aday/reportservice/model";
//        try {
//            convertJsonToJavaClass(
//                    httpEntity.getBody(),
//                    new File(outputDir),
//                    "generated",
//                    javaClassName
//            );
//            return ResponseEntity.ok("\"all\":\"good\"");
//        } catch (Exception e) {
//            log.error(e.getMessage());
//            e.printStackTrace();
//            return ResponseEntity.badRequest().body(e.getMessage());
//        }
//
//    }
//
//    public void convertJsonToJavaClass(String json, File outputJavaClassDirectory, String packageName, String javaClassName) throws IOException {
//        JCodeModel jcodeModel = new JCodeModel();
//
//        GenerationConfig config = new DefaultGenerationConfig() {
//            @Override
//            public boolean isGenerateBuilders() {
//                return true;
//            }
//
//            @Override
//            public SourceType getSourceType() {
//                return SourceType.JSON;
//            }
//        };
//
//        SchemaMapper mapper = new SchemaMapper(new RuleFactory(config, new Jackson2Annotator(config), new SchemaStore()), new SchemaGenerator());
//        mapper.generate(jcodeModel, javaClassName, packageName, json);
//
//        jcodeModel.build(outputJavaClassDirectory);
//    }
}
