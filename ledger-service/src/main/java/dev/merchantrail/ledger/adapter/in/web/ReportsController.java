package dev.merchantrail.ledger.adapter.in.web;

import dev.merchantrail.ledger.application.port.out.LedgerRepository;
import dev.merchantrail.ledger.application.service.ReconciliationReportService;
import dev.merchantrail.ledger.domain.LedgerEntry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * REST Controller exposing financial reporting, switching metrics,
 * and downloadable audit reconciliation files.
 */
@RestController
@RequestMapping("/api/v1/reports")
@CrossOrigin(origins = "*")
public class ReportsController {

    private final ReconciliationReportService reportService;
    private final LedgerRepository ledgerRepository;

    public ReportsController(ReconciliationReportService reportService, LedgerRepository ledgerRepository) {
        this.reportService = reportService;
        this.ledgerRepository = ledgerRepository;
    }

    @GetMapping("/settlement-summary")
    public ResponseEntity<ReconciliationReportService.SettlementSummary> getSettlementSummary() {
        return ResponseEntity.ok(reportService.getSettlementSummary());
    }

    @GetMapping("/switching-analytics")
    public ResponseEntity<ReconciliationReportService.SwitchingAnalytics> getSwitchingAnalytics() {
        return ResponseEntity.ok(reportService.getSwitchingAnalytics());
    }

    @GetMapping("/reconciliation/entries")
    public ResponseEntity<List<LedgerEntry>> getReconciliationEntries() {
        return ResponseEntity.ok(ledgerRepository.findAll());
    }

    @GetMapping(value = "/reconciliation/export.csv", produces = "text/csv")
    public ResponseEntity<String> exportReconciliationCsv() {
        String csvData = reportService.generateReconciliationCsv();
        String filename = String.format("reconciliation_%s.csv", 
            LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(csvData);
    }
}
