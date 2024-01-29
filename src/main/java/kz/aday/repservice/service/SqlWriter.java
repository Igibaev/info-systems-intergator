package kz.aday.repservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
public class SqlWriter extends ReportWriter {
    private Writer writer;
    private boolean isDDLQueryNeed = true;
    private boolean truncateTable = true;

    public SqlWriter(OutputStream outputStream) {
        this.writer = new OutputStreamWriter(outputStream);
    }

    public SqlWriter(StringWriter stringWriter, boolean isDDLQueryNeed, boolean truncateTable) {
        this.writer = stringWriter;
        this.isDDLQueryNeed = isDDLQueryNeed;
        this.truncateTable = truncateTable;
    }

    private String INSERT_QUERY;
    private String TRUNCATE_TABLE = "TRUNCATE TABLE %s;\n\n";

    private String TABLE_MASK_OPEN = "CREATE TABLE IF NOT EXISTS %s (\n";
    private String FIELD_MASK = "    %s VARCHAR" ;
    private String TABLE_MASK_CLOSE = "\n);";

    private String COMMENT_MASK = "COMMENT ON COLUMN %s.%s IS '%s';\n";

    private String INSERT_MASK_OPEN = "INSERT INTO %s (";
    private String INSERT_MASK_CLOSE = ")\n";

    private String VALUES_MASK_OPEN = "VALUES (";
    private String VALUES_MASK_CLOSE = ");\n";

    @Override
    public void writeHeaders(List<Map<String, JsonNode>> rows, String entityName) throws IOException {
        if (rows.isEmpty()) {
            return;
        }
        entityName = entityName.replaceAll("-", "_");
        StringBuilder queryInsert = new StringBuilder();
        queryInsert.append(String.format(INSERT_MASK_OPEN, entityName));
        StringBuilder queryDDLComments = new StringBuilder();

        StringBuilder queryDDL = new StringBuilder();
        if (truncateTable) {
            queryDDL.append(String.format(TRUNCATE_TABLE, entityName));
        }
        queryDDL.append(String.format(TABLE_MASK_OPEN, entityName));

        Iterator<Map.Entry<String, JsonNode>> rowIterator = rows.stream().findFirst().get().entrySet().iterator();
        while (rowIterator.hasNext()) {
            String header = rowIterator.next().getKey();
            queryInsert.append(header);
            queryDDLComments.append(String.format(COMMENT_MASK, entityName, header, getText(entityName, header)));
            queryDDL.append(String.format(FIELD_MASK, header));
            if (rowIterator.hasNext()) {
                queryDDL.append(",\n");
                queryInsert.append(", ");
            }
        }

        queryDDL.append(String.format(TABLE_MASK_CLOSE, entityName));
        queryDDL.append("\n");
        queryDDL.append(queryDDLComments);

        if (isDDLQueryNeed) {
            writer.write(queryDDL.toString());
            writer.flush();
        }

        queryInsert.append(INSERT_MASK_CLOSE);
        this.INSERT_QUERY = queryInsert.toString();
    }

    @Override
    public void writeRows(List<Map<String, JsonNode>> rows) throws IOException {
        if (rows.isEmpty()) {
            return;
        }

        for (Map<String, JsonNode> row: rows) {
            StringBuilder queryInsertValues = new StringBuilder();
            queryInsertValues.append(INSERT_QUERY);
            queryInsertValues.append(VALUES_MASK_OPEN);
            Iterator<Map.Entry<String, JsonNode>> rowIterator = row.entrySet().iterator();
            while (rowIterator.hasNext()) {
                String value = convertToText(rowIterator.next().getValue()).replaceAll("'","");
                queryInsertValues.append("'");
                queryInsertValues.append(value);
                queryInsertValues.append("'");
                if (rowIterator.hasNext()) {
                    queryInsertValues.append(", ");
                }
            }
            queryInsertValues.append(VALUES_MASK_CLOSE);
            writer.write(queryInsertValues.toString());

        }
        writer.flush();
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    @Override
    public void finish() throws IOException {
        writer.close();
    }
}
