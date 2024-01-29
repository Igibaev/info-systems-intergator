package kz.aday.repservice.controller;//package kz.aday.reportservice.controller;



import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    protected String handleConflict(RuntimeException ex, Model model) {
        model.addAttribute("error", ex.getMessage());
        return "errorPage";
    }
}
