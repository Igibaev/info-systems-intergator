package kz.aday.repservice.model;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class Migration {
    private Long id;
    private String entityName;
    private Long total;
    private int exported;
    private String lastRequestUrl;
    private LocalDateTime createdDate;
    private Status status;

    public enum Status {
        IN_PROGRESS, FAILED, DONE;
    }
}
