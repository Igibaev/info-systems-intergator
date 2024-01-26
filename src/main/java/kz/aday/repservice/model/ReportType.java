package kz.aday.repservice.model;

import lombok.Getter;

@Getter
public enum ReportType {
    SQl("sql"), XLSX("xlsx"), CSV("csv");

    private String fileExtension;

    ReportType(String fileExtension) {
        this.fileExtension = fileExtension;
    }
}
