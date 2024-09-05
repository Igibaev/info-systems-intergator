package kz.aday.repservice.controller;



import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    protected String handleConflict(RuntimeException ex, Model model) {
        log.error("Catched exception: ", ex);
        model.addAttribute("error", ex.getMessage());
        return "errorPage";
    }
}
