package com.infosys.Water_Usage_Monitoring.dto;

import java.util.List;

public record CsvImportResult(long totalRows, int imported, int failed, List<RowError> errors) {}