package kz.aday.repservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.opencsv.CSVWriter;
import kz.aday.repservice.model.EntityMigration;
import kz.aday.repservice.util.Messages;
import lombok.extern.slf4j.Slf4j;
import org.dhatim.fastexcel.Workbook;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class ReportWriter {
    final Workbook workbook;
    final CSVWriter csvWriter;
    final EntityMigration entityMigration;

    public ReportWriter() {
        this.workbook = null;
        this.csvWriter = null;
        this.entityMigration = null;
    }

    public ReportWriter(Workbook workbook, EntityMigration entityMigration) {
        this.workbook = workbook;
        this.csvWriter = null;
        this.entityMigration = entityMigration;
    }

    public ReportWriter(CSVWriter csvWriter, EntityMigration entityMigration) {
        this.workbook = null;
        this.csvWriter = csvWriter;
        this.entityMigration = entityMigration;
    }

    public abstract void writeHeaders(List<Map<String, JsonNode>> rows, String entityName) throws IOException;
    public abstract void writeRows(List<Map<String, JsonNode>> rows) throws IOException;

    public abstract void flush() throws IOException;
    public abstract void finish() throws IOException;

    protected String getText(String gzEntityName, String key) {
        if (entityMigration != null && entityMigration.getLocalization().containsKey(key)) {
            return entityMigration.getLocalization().get(key);
        }
        return Messages.getText(gzEntityName + "." + key);
    }

    protected String convertToText(JsonNode node) {
        if (node.isValueNode()) {
            return node.asText();
        } else {
            return node.toString();
        }
    }

}

