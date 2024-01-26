package kz.aday.repservice.service;

import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.opencsv.ICSVWriter.DEFAULT_ESCAPE_CHARACTER;
import static com.opencsv.ICSVWriter.DEFAULT_LINE_END;
import static com.opencsv.ICSVWriter.DEFAULT_QUOTE_CHARACTER;

@Slf4j
public class CsvWriter extends ReportWriter {

    public CsvWriter(Writer writer) {
        super(new CSVWriter(writer, ';', DEFAULT_QUOTE_CHARACTER, DEFAULT_ESCAPE_CHARACTER, DEFAULT_LINE_END));
    }

    @Override
    public void writeHeaders(List<Map<String, String>> rows, String entityName) throws IOException {
        if (csvWriter == null) {
            throw new IllegalArgumentException("CSWWRITER is null");
        }
        if (rows.isEmpty()) {
            return;
        }
        List<String> headers = new ArrayList<>();
        for (Map.Entry<String, String> row: rows.stream().findFirst().get().entrySet()) {
            headers.add(getText(entityName, row.getKey()));
        }
        csvWriter.writeNext(headers.toArray(new String[0]));
        csvWriter.flush();
    }

    public void writeRows(List<Map<String, String>> rows) throws IOException {
        if (csvWriter == null) {
            throw new IllegalArgumentException("CSWWRITER is null");
        }
        if (rows.isEmpty()) {
            return;
        }

        for (Map<String, String> row: rows) {
            List<String> rowsOrdered = new ArrayList<>();
            for (Map.Entry<String, String> rowMap: row.entrySet()) {
                rowsOrdered.add(rowMap.getValue());
            }
            csvWriter.writeNext(rowsOrdered.toArray(new String[0]));
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
