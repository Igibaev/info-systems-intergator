package kz.aday.repservice.controller;

import kz.aday.repservice.api.TaldayApi;
import kz.aday.repservice.talday.TaldayApiService;
import kz.aday.repservice.talday.model.StatPeriod;
import kz.aday.repservice.talday.model.Stats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Slf4j
@Controller
public class TaldayController {
    private final TaldayApiService taldayApiService;

    public TaldayController(TaldayApiService taldayApiService) {
        this.taldayApiService = taldayApiService;
    }

    @GetMapping("/test")
    public ResponseEntity test() {
//        taldayApiService.migrate();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/start-migration")
    public ResponseEntity startMigration() {
        try {
            taldayApiService.startMigration();
        } catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(e.getMessage());
        }

        return ResponseEntity.ok().build();
    }
}
