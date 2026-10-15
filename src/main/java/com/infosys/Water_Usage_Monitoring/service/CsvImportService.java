package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.CsvImportResult;
import com.infosys.Water_Usage_Monitoring.dto.RowError;
import com.infosys.Water_Usage_Monitoring.entity.Household;
import com.infosys.Water_Usage_Monitoring.enums.ReadingSource;
import com.infosys.Water_Usage_Monitoring.exception.BadRequestException;
import com.infosys.Water_Usage_Monitoring.exception.DuplicateResourceException;
import com.infosys.Water_Usage_Monitoring.exception.ResourceNotFoundException;
import com.infosys.Water_Usage_Monitoring.repository.HouseholdRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CsvImportService {

    private static final int MAX_ROWS = 10_000;

    private final UsageService usageService;
    private final HouseholdRepository householdRepo;

    private record ParsedRow(long rowNumber, String meterNumber, LocalDate date, BigDecimal reading) {}

    // Jaan bujh ke @Transactional nahi: har row apni transaction mein save hoti hai (UsageService.saveReading),
    // isliye ek row fail ho to baaki rows rollback nahi hoti.
    public CsvImportResult importUsage(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new BadRequestException("CSV file is empty");
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".csv"))
            throw new BadRequestException("Only .csv files are allowed");

        List<RowError> errors = new ArrayList<>();
        List<ParsedRow> rows = new ArrayList<>();
        long total = 0;

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .build();

        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            reader.mark(1);
            if (reader.read() != 0xFEFF) reader.reset();     // Excel ka BOM hata do

            try (CSVParser parser = format.parse(reader)) {
                Map<String, Integer> headerMap = parser.getHeaderMap();
                if (headerMap == null)
                    throw new BadRequestException("CSV has no header row");

                Set<String> headers = new HashSet<>();
                headerMap.keySet().forEach(h -> headers.add(h.trim().toLowerCase()));
                for (String required : List.of("meternumber", "readingdate", "meterreading")) {
                    if (!headers.contains(required))
                        throw new BadRequestException("Missing column '" + required
                                + "'. Expected header: meterNumber,readingDate,meterReading");
                }

                for (CSVRecord rec : parser) {
                    total++;
                    if (total > MAX_ROWS)
                        throw new BadRequestException("Too many rows (max " + MAX_ROWS + ")");
                    parseRow(rec, rows, errors);
                }
            }
        } catch (IOException e) {
            throw new BadRequestException("Could not read the CSV file");
        }

        // meter ke hisaab se, phir date ke hisaab se (stable sort)
        rows.sort(Comparator.comparing(ParsedRow::meterNumber).thenComparing(ParsedRow::date));

        Map<String, Household> cache = new HashMap<>();
        int imported = 0;

        for (ParsedRow r : rows) {
            try {
                Household h = cache.get(r.meterNumber());
                if (h == null) {
                    h = householdRepo.findByMeterNumber(r.meterNumber())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "No household found for meter " + r.meterNumber()));
                    cache.put(r.meterNumber(), h);
                }
                usageService.saveReading(h, r.date(), r.reading(), ReadingSource.CSV);
                imported++;
            } catch (ResourceNotFoundException | DuplicateResourceException | BadRequestException e) {
                errors.add(new RowError(r.rowNumber(), e.getMessage()));
            } catch (DataIntegrityViolationException e) {
                errors.add(new RowError(r.rowNumber(), "Duplicate or invalid data"));
            }
        }

        errors.sort(Comparator.comparingLong(RowError::rowNumber));
        return new CsvImportResult(total, imported, errors.size(), errors);
    }

    private void parseRow(CSVRecord rec, List<ParsedRow> rows, List<RowError> errors) {
        long n = rec.getRecordNumber();
        try {
            if (!rec.isConsistent())
                throw new IllegalArgumentException("Wrong number of columns");

            String meter = rec.get("meterNumber");
            if (meter == null || meter.isBlank())
                throw new IllegalArgumentException("meterNumber is empty");

            LocalDate date = LocalDate.parse(rec.get("readingDate"));
            if (date.isAfter(LocalDate.now()))
                throw new IllegalArgumentException("readingDate is in the future");

            BigDecimal reading = new BigDecimal(rec.get("meterReading"));
            if (reading.signum() < 0)
                throw new IllegalArgumentException("meterReading cannot be negative");

            rows.add(new ParsedRow(n, meter, date, reading));
        } catch (DateTimeParseException e) {
            errors.add(new RowError(n, "Invalid readingDate (use yyyy-MM-dd)"));
        } catch (NumberFormatException e) {
            errors.add(new RowError(n, "meterReading is not a valid number"));
        } catch (IllegalArgumentException e) {
            errors.add(new RowError(n, e.getMessage()));
        }
    }
}