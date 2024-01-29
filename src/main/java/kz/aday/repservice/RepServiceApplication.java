package kz.aday.repservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
public class RepServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RepServiceApplication.class, args);
    }


}
