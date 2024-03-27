package kz.aday.repservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import kz.aday.repservice.model.EntityMigration;
import lombok.extern.slf4j.Slf4j;
import org.dhatim.fastexcel.BorderStyle;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

@Slf4j
public class XlsxWriter extends ReportWriter {
    private int rowNumber;
    private Worksheet worksheet;

    public XlsxWriter(OutputStream outputStream, EntityMigration entityMigration) {
        super(new Workbook(outputStream, "gz", "1.0"), entityMigration);
        this.worksheet = workbook.newWorksheet("sheet 1");
        this.rowNumber = 0;
    }

    @Override
    public void writeHeaders(List<Map<String, JsonNode>> rows, String entityName) throws IOException {
        if (rows.isEmpty()) {
            return;
        }
        int c = 0;
        for (Map.Entry<String, JsonNode> row: rows.stream().findFirst().get().entrySet()) {
            worksheet.value(rowNumber, c++, getText(entityName, row.getKey()));
        }
        rowNumber++;
        worksheet.range(0,0,0, c-1)
                .style()
                .bold()
                .borderStyle(BorderStyle.MEDIUM)
                .wrapText(true)
                .set();
        worksheet.flush();
    }

    @Override
    public void writeRows(List<Map<String, JsonNode>> rows) throws IOException {
        if (rows.isEmpty()) {
            return;
        }
        for (Map<String, JsonNode> row: rows) {
            int c = 0;
            for (Map.Entry<String, JsonNode> entryRow: row.entrySet()) {
                worksheet.value(rowNumber, c++, convertToText(entryRow.getValue()));
            }
            rowNumber++;
        }
        rowNumber++;

        worksheet.flush();
    }

    @Override
    public void flush() throws IOException {
        worksheet.flush();
    }

    @Override
    public void finish() throws IOException {
        workbook.finish();
    }
}
