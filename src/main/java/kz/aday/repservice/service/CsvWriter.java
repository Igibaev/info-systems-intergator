package kz.aday.repservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.opencsv.CSVWriter;
import kz.aday.repservice.model.EntityMigration;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.opencsv.ICSVWriter.DEFAULT_ESCAPE_CHARACTER;
import static com.opencsv.ICSVWriter.DEFAULT_LINE_END;
import static com.opencsv.ICSVWriter.DEFAULT_QUOTE_CHARACTER;

@Slf4j
public class CsvWriter extends ReportWriter {

    public CsvWriter(Writer writer, EntityMigration entityMigration) {
        super(new CSVWriter(writer, ';', DEFAULT_QUOTE_CHARACTER, DEFAULT_ESCAPE_CHARACTER, DEFAULT_LINE_END), entityMigration);
    }

    @Override
    public void writeHeaders(List<Map<String, JsonNode>> rows, String entityName) throws IOException {
        if (csvWriter == null) {
            throw new IllegalArgumentException("Writer is null");
        }
        if (rows.isEmpty()) {
            return;
        }
        List<String> headers = new ArrayList<>();
        for (Map.Entry<String, JsonNode> row: rows.stream().findFirst().get().entrySet()) {
            headers.add(getText(entityName, row.getKey()));
        }

        csvWriter.writeNext(headers.toArray(new String[0]));
        csvWriter.flush();
    }

    public void writeRows(List<Map<String, JsonNode>> rows) throws IOException {
        if (csvWriter == null) {
            throw new IllegalArgumentException("CSWWRITER is null");
        }
        if (rows.isEmpty()) {
            return;
        }

        for (Map<String, JsonNode> row: rows) {
            List<String> values = new ArrayList<>();
            Iterator<Map.Entry<String, JsonNode>> rowIterator = row.entrySet().iterator();
            while (rowIterator.hasNext()) {
                Map.Entry<String, JsonNode> rowMap = rowIterator.next();
                values.add(convertToText(rowMap.getValue()));
            }
            csvWriter.writeNext(values.toArray(new String[0]));
        }
        csvWriter.flush();
    }

    @Override
    public void flush() throws IOException {
        csvWriter.flush();
    }

    @Override
    public void finish() throws IOException {
        csvWriter.close();
    }

}
