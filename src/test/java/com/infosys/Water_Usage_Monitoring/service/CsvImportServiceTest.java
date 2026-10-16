package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.CsvImportResult;
import com.infosys.Water_Usage_Monitoring.entity.Household;
import com.infosys.Water_Usage_Monitoring.enums.ReadingSource;
import com.infosys.Water_Usage_Monitoring.exception.BadRequestException;
import com.infosys.Water_Usage_Monitoring.exception.DuplicateResourceException;
import com.infosys.Water_Usage_Monitoring.repository.HouseholdRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CsvImportServiceTest {

    @Mock UsageService usageService;
    @Mock HouseholdRepository householdRepo;
    @InjectMocks CsvImportService service;

    private Household household;

    @BeforeEach
    void setUp() {
        household = new Household();
        household.setId(1L);
    }

    private MockMultipartFile csv(String name, String content) {
        return new MockMultipartFile("file", name, "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void mixedFile_importsValidRowsAndReportsBadOnes() {
        when(householdRepo.findByMeterNumber("MTR-001")).thenReturn(Optional.of(household));
        when(householdRepo.findByMeterNumber("MTR-XXX")).thenReturn(Optional.empty());

        String content = """
                meterNumber,readingDate,meterReading
                MTR-001,2026-10-01,1000
                MTR-001,2026-10-02,1015
                MTR-XXX,2026-10-02,500
                MTR-001,2026-13-45,1100
                MTR-001,2026-10-05,abc
                """;

        CsvImportResult r = service.importUsage(csv("usage.csv", content));

        assertEquals(5, r.totalRows());
        assertEquals(2, r.imported());
        assertEquals(3, r.failed());
        assertTrue(r.errors().stream().anyMatch(e -> e.message().contains("No household")));
        assertTrue(r.errors().stream().anyMatch(e -> e.message().contains("Invalid readingDate")));
        assertTrue(r.errors().stream().anyMatch(e -> e.message().contains("not a valid number")));
        verify(usageService, times(2)).saveReading(eq(household), any(LocalDate.class),
                any(BigDecimal.class), eq(ReadingSource.CSV));
    }

    @Test
    void duplicateRowFromService_isReportedWithoutStoppingImport() {
        when(householdRepo.findByMeterNumber("MTR-001")).thenReturn(Optional.of(household));
        when(usageService.saveReading(any(Household.class), any(LocalDate.class),
                any(BigDecimal.class), any(ReadingSource.class)))
                .thenAnswer(inv -> {
                    LocalDate date = inv.getArgument(1);
                    if (date.equals(LocalDate.of(2026, 10, 2)))
                        throw new DuplicateResourceException("Reading already exists for " + date);
                    return null;
                });

        String content = "meterNumber,readingDate,meterReading\n"
                + "MTR-001,2026-10-01,1000\n"
                + "MTR-001,2026-10-02,1015\n";

        CsvImportResult r = service.importUsage(csv("usage.csv", content));

        assertEquals(1, r.imported());
        assertEquals(1, r.failed());
        assertTrue(r.errors().get(0).message().contains("already exists"));
    }

    @Test
    void emptyFile_isRejected() {
        assertThrows(BadRequestException.class, () -> service.importUsage(csv("usage.csv", "")));
    }

    @Test
    void nonCsvFile_isRejected() {
        assertThrows(BadRequestException.class,
                () -> service.importUsage(csv("usage.txt", "meterNumber,readingDate,meterReading\n")));
    }

    @Test
    void missingRequiredColumn_isRejected() {
        assertThrows(BadRequestException.class,
                () -> service.importUsage(csv("usage.csv", "a,b,c\n1,2,3\n")));
    }
}