package kz.aday.repservice.service;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
public class SqlWriter extends ReportWriter {
    private Writer writer;

    public SqlWriter(OutputStream outputStream) {
        this.writer = new OutputStreamWriter(outputStream);
    }

    private String INSERT_QUERY;

    private String FIELD_MASK = "    %s VARCHAR" ;
    private String TABLE_MASK_OPEN = "CREATE TABLE %s (\n";
    private String TABLE_MASK_CLOSE = "\n);";

    private String COMMENT_MASK = "COMMENT ON COLUMN %s.%s IS '%s';\n";

    private String INSERT_MASK_OPEN = "INSERT INTO %s (";
    private String INSERT_MASK_CLOSE = ")\n";

    private String VALUES_MASK_OPEN = "VALUES (";
    private String VALUES_MASK_CLOSE = ");\n";

    @Override
    public void writeHeaders(List<Map<String, String>> rows, String entityName) throws IOException {
        if (rows.isEmpty()) {
            return;
        }
        StringBuilder queryInsert = new StringBuilder();
        queryInsert.append(String.format(INSERT_MASK_OPEN, entityName));
        StringBuilder queryDDLComments = new StringBuilder();

        StringBuilder queryDDL = new StringBuilder();
        queryDDL.append(String.format(TABLE_MASK_OPEN, entityName));

        Iterator<Map.Entry<String, String>> rowIterator = rows.stream().findFirst().get().entrySet().iterator();
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

        writer.write(queryDDL.toString());
        writer.flush();

        queryInsert.append(String.format(INSERT_MASK_CLOSE, entityName));
        this.INSERT_QUERY = queryInsert.toString();
    }

    @Override
    public void writeRows(List<Map<String, String>> rows) throws IOException {
        if (rows.isEmpty()) {
            return;
        }
        StringBuilder queryInsertValues = new StringBuilder();
        queryInsertValues.append(INSERT_QUERY);
        queryInsertValues.append(VALUES_MASK_OPEN);

        Iterator<Map.Entry<String, String>> rowIterator = rows.stream().findFirst().get().entrySet().iterator();
        while (rowIterator.hasNext()) {
            String header = rowIterator.next().getValue();
            queryInsertValues.append("'");
            queryInsertValues.append(header);
            queryInsertValues.append("'");
            if (rowIterator.hasNext()) {
                queryInsertValues.append(", ");
            }
        }

        queryInsertValues.append(VALUES_MASK_CLOSE);
        writer.write(queryInsertValues.toString());
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
