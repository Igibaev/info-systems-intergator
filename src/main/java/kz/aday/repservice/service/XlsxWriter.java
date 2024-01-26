package kz.aday.repservice.service;

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

    public XlsxWriter(OutputStream outputStream) {
        super(new Workbook(outputStream, "gz", "1.0"));
        this.worksheet = workbook.newWorksheet("sheet 1");
        this.rowNumber = 0;
    }

    @Override
    public void writeHeaders(List<Map<String, String>> rows, String entityName) throws IOException {
        if (rows.isEmpty()) {
            return;
        }
        int c = 0;
        for (Map.Entry<String, String> row: rows.stream().findFirst().get().entrySet()) {
            worksheet.value(rowNumber, c++, getText(entityName, row.getKey()));
        }
        rowNumber++;
        worksheet.range(0,0,0, c-1)
                .style()
                .bold()
                .wrapText(true)
                .borderStyle(BorderStyle.MEDIUM)
                .set();
        worksheet.flush();
    }

    @Override
    public void writeRows(List<Map<String, String>> rows) throws IOException {
        if (rows.isEmpty()) {
            return;
        }
        for (Map<String, String> row: rows) {
            int c = 0;
            for (Map.Entry<String, String> entryRow: row.entrySet()) {
                worksheet.value(rowNumber, c++, entryRow.getValue());
            }
            rowNumber++;
        }

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
