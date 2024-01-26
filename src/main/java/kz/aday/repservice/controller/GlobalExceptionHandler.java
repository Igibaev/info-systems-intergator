package kz.aday.repservice.controller;//package kz.aday.reportservice.controller;
//
//
//import org.springframework.boot.Banner;
//import org.springframework.http.ResponseEntity;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.ControllerAdvice;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.context.request.WebRequest;
//
//@ControllerAdvice
//public class GlobalExceptionHandler {
//
//    @ExceptionHandler(RuntimeException.class)
//    protected String handleConflict(RuntimeException ex, Model model) {
//        ex.printStackTrace();
//        model.addAttribute("error", ex.getMessage());
//        return "errorPage";
//    }
//}
