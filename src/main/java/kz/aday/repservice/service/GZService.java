package kz.aday.repservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import kz.aday.repservice.api.Fields;
import kz.aday.repservice.exceptons.ResponseGZBadRequest;
import kz.aday.repservice.model.Migration;
import kz.aday.repservice.model.RequestGZ;
import kz.aday.repservice.model.ResponseGZ;
import kz.aday.repservice.api.GosZakupApi;
import kz.aday.repservice.repository.MigrationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.awt.print.Pageable;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GZService {
    private final GosZakupApi gosZakupApi;
    private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final MigrationRepository migrationRepository;

    public GZService(GosZakupApi gzRepository, MigrationRepository migrationRepository) {
        this.migrationRepository = migrationRepository;
        this.gosZakupApi = gzRepository;

    }

    public List<Migration> getAllMigrations() {
        List<Migration> migrations = migrationRepository.getAllMigrations();
        if (migrations == null) {
            return new ArrayList<>();
        }
        return migrations;
    }

    public boolean existMigration(RequestGZ request) {
        return migrationRepository.exists(request.getGzEntityName());
    }

    public void startMigration(RequestGZ request) {
        if (migrationRepository.exists(request.getGzEntityName())) {
            log.error("миграция на данную сущность уже существует со статусом IN_PROGRESS ");
            return;
        }
        Migration migration = Migration.builder()
                .total(getTotal(request))
                .entityName(request.getGzEntityName())
                .status(Migration.Status.IN_PROGRESS)
                .exported(0)
                .createdDate(LocalDateTime.now())
                .build();
        migrationRepository.create(migration);
        request.setSize(defineRequestSize(request.getGzEntityName()));
        log.info("Prepare request to migration [{}], set batchSize:{}", request, request.getSize());
        boolean isDDLQueryNeed = true;
        try {
            int rowsMigrated = 0;
            while (true) {
                StringWriter stringWriter = new StringWriter();
                ReportWriter writer = new SqlWriter(stringWriter, isDDLQueryNeed, request.isTruncateTable());
                int exportedRows = export(request, writer);
                log.info("Rows ready to migration: {}", exportedRows);

                if (exportedRows == -1) {
                    log.error("migration failed request:{}", request);
                    migration.setStatus(Migration.Status.FAILED);
                    migrationRepository.update(migration);
                    break;
                }

                rowsMigrated+=exportedRows;
                migration.setExported(rowsMigrated);
                migration.setLastRequestUrl(request.getUrl());
                migrationRepository.executeQuery(stringWriter.toString());
                migrationRepository.update(migration);
                log.info("Rows migrated {}", rowsMigrated);
                isDDLQueryNeed = false;
                if (request.isDone()) {
                    log.info("All rows migrated, stop migration");
                    migration.setStatus(Migration.Status.DONE);
                    migrationRepository.update(migration);
                    break;
                }
            }
        } catch (Exception e) {
            log.error("SOME ERROR {}", e.getMessage());
            log.error("FLUSH MIGRATION {}", migration);
            migration.setStatus(Migration.Status.FAILED);
            migrationRepository.update(migration);
        }
    }

    private int defineRequestSize(String gzEntityName) {
        if (gzEntityName.contains("plans")) {
            return 500;
        }
        return 10000;
    }

    private Long getTotal(RequestGZ request) {
        ResponseGZ response = gosZakupApi.execute(request.getUrl(), request.getToken()).block();
        checkIfResponseIsNull(request.getUrl(), response);
        return response.getTotal();
    }

    public StreamingResponseBody createReport(RequestGZ request) {
        switch (request.getReportType()) {
            case CSV: return writeToCsv(request);
            case XLSX: return writeToXlsx(request);
            case SQl: return writeToSql(request);
            default: return writeToCsv(request);
        }
    }

    private StreamingResponseBody writeToCsv(RequestGZ request) {
        return outputStream -> {
            Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
            ReportWriter reportWriter = new CsvWriter(writer);
            export(request, reportWriter);
        };
    }

    private StreamingResponseBody writeToSql(RequestGZ request) {
        return outputStream -> {
            ReportWriter reportWriter = new SqlWriter(outputStream);
            export(request, reportWriter);
        };
    }

    private StreamingResponseBody writeToXlsx(RequestGZ request) {
        return outputStream -> {
            ReportWriter reportWriter = new XlsxWriter(outputStream);
            export(request, reportWriter);
        };
    }

    private int export(RequestGZ request, ReportWriter reportWriter) throws IOException {
        try {
            int rowsExported;
            if (request.getDateTo() != null && request.getDateFrom() != null) {
                rowsExported = exportByDate(request, reportWriter);
            } else {
                rowsExported = exportBySize(request, reportWriter);
            }
            log.info("Report is done. Rows exported:{}", rowsExported);
            return rowsExported;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            reportWriter.finish();
        }
        return -1;
    }

    private int exportByDate(RequestGZ request, ReportWriter reportWriter) throws IOException {
        ResponseGZ response;
        boolean wasHeaderWritten = false;
        int rowsExported = 0;
        while (true) {
            if (rowsExported >= request.getSize()) {
                log.info("Break report, limit exceeded");
                break;
            }
            List<Map<String, JsonNode>> rows = new ArrayList<>();
            response = gosZakupApi.execute(request).block();
            checkIfResponseIsNull(request.getUrl(), response);
            request.setUrl(response.getNextPage());

            Date firstRowDate = getRowDate(response.getRows().get(0));
            Date lastRowDate = getRowDate(response.getRows().get(response.getRows().size() - 1));

            if (!wasHeaderWritten) {
                reportWriter.writeHeaders(response.getRows(), request.getGzEntityName());
                wasHeaderWritten = true;
            }

            if (isDateInTimeRange(firstRowDate, request.getDateFrom(), request.getDateTo())
                    && isDateInTimeRange(lastRowDate, request.getDateFrom(), request.getDateTo())) {
                rows.addAll(response.getRows());
                rowsExported += rows.size();
            } else if (isDateInTimeRange(firstRowDate, request.getDateFrom(), request.getDateTo())) {
                for (Map<String, JsonNode> row : response.getRows()) {
                    Date rowDate = getRowDate(row);
                    if (isDateInTimeRange(rowDate, request.getDateFrom(), request.getDateTo())) {
                        rows.add(row);
                    }
                }
                rowsExported += rows.size();
            } else if (isDateInTimeRange(lastRowDate, request.getDateFrom(), request.getDateTo())) {
                for (Map<String, JsonNode> row : response.getRows()) {
                    Date rowDate = getRowDate(row);
                    if (isDateInTimeRange(rowDate, request.getDateFrom(), request.getDateTo())) {
                        rows.add(row);
                    }
                }
                rowsExported += rows.size();
            } else {
                if (isNeedToSkipRows(firstRowDate, lastRowDate, request.getDateFrom(), request.getDateTo())) {
                    log.info("Skip rows with dates {}:{}", firstRowDate, lastRowDate);
                    continue;
                } else {
                    break;
                }
            }

            reportWriter.writeRows(rows);
            log.info("exported:{}", rowsExported);
            if (response.getNextPage().isEmpty()) {
                log.info("next page doesn't exist, stoping report");
                request.setDone(true);
                break;
            }
        }
        return rowsExported;
    }

    private int exportBySize(RequestGZ request, ReportWriter reportWriter) throws IOException {
        ResponseGZ response;
        boolean wasHeaderWritten = false;
        int rowsExported = 0;
        while (true) {
            response = gosZakupApi.execute(request).block();
            checkIfResponseIsNull(request.getUrl(), response);
            request.setUrl(response.getNextPage());

            if (!wasHeaderWritten) {
                reportWriter.writeHeaders(response.getRows(), request.getGzEntityName());
                wasHeaderWritten = true;
            }

            reportWriter.writeRows(response.getRows());
            rowsExported += response.getRows().size();
            log.info("exported:{}", rowsExported);
            if (rowsExported >= request.getSize()) {
                break;
            }
            if (response.getNextPage().isEmpty()) {
                log.info("next page doesn't exist, stop report");
                request.setDone(true);
                break;
            }
        }
        return rowsExported;
    }

    private boolean isNeedToSkipRows(Date firstRowDate, Date lastRowDate, Date dateFrom, Date dateTo) {
        // учитывя что на каждый запрос в АПИ, возвращаются данные от новых к старым, если последняя дата из
        // запросы находится перед датами фильтрации, то завершить цикл, иначе находимся в цикле но данные не записываем, их пропускаем
        return !dateFrom.after(lastRowDate);
    }

    private Date getRowDate(Map<String, JsonNode> row) {
        String firstRowDate = row.get(Fields.crdate).asText();
        try {
            if (firstRowDate == null) {
                throw new ResponseGZBadRequest(
                        String.format("Выгрузка для по датам не возможна, поля сущности не содержат даты")
                );
            }
            return formatter.parse(firstRowDate);
        } catch (ParseException e) {
            throw new ResponseGZBadRequest("Неподдерживаемый формат данных " + firstRowDate);
        }
    }

    private boolean isDateInTimeRange(Date date, Date dateFrom, Date dateTo) {
        if (date == null) {
            return false;
        }
        return date.after(dateFrom) && date.before(dateTo);
    }

    private static void checkIfResponseIsNull(String url, ResponseGZ response) {
        if (response == null) {
            throw new ResponseGZBadRequest(
                    String.format("By url:%s response is null", url)
            );
        }
    }
}
