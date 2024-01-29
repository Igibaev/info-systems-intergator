package kz.aday.repservice.controller;

import kz.aday.repservice.repository.MigrationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
public class MigrationController {
    private final MigrationRepository repository;

    public MigrationController(MigrationRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/migration/delete")
    public String mainPage(@RequestParam Long id, Model model) {
        repository.delete(id);
        return "redirect:/";
    }

    @GetMapping("/migration/refresh")
    public String refresh() {
        return "redirect:/";
    }
}
