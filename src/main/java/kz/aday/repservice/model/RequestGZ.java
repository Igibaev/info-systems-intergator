package kz.aday.repservice.model;

import lombok.Data;
import lombok.Getter;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
public class RequestGZ {
    String token;
    String url;
    Long searchAfter;
    String gzEntityName;

    @DateTimeFormat(pattern = "dd/MM/yyyy")
    Date dateFrom;
    @DateTimeFormat(pattern = "dd/MM/yyyy")
    Date dateTo;

    ReportType reportType;

    int offset;
    int size;

    String dbHost;
}
