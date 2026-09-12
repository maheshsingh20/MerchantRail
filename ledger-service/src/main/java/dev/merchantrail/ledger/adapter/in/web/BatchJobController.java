package dev.merchantrail.ledger.adapter.in.web;

import dev.merchantrail.ledger.application.port.out.LedgerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API for orchestrating and monitoring Spring Batch clearing jobs.
 */
@RestController
@RequestMapping("/api/v1/ledger/batch")
public class BatchJobController {

    private static final Logger log = LoggerFactory.getLogger(BatchJobController.class);

    private final JobLauncher jobLauncher;
    private final Job dailyClearingJob;
    private final JobExplorer jobExplorer;
    private final LedgerRepository ledgerRepository;

    public BatchJobController(JobLauncher jobLauncher, 
                              Job dailyClearingJob, 
                              JobExplorer jobExplorer,
                              LedgerRepository ledgerRepository) {
        this.jobLauncher = jobLauncher;
        this.dailyClearingJob = dailyClearingJob;
        this.jobExplorer = jobExplorer;
        this.ledgerRepository = ledgerRepository;
    }

    /**
     * Triggers the daily clearing and settlement batch job.
     */
    @PostMapping("/clearing")
    public ResponseEntity<Map<String, Object>> runClearingBatch() {
        try {
            JobParameters params = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("triggerSource", "REST_API_OPERATOR")
                .toJobParameters();

            log.info("Triggering Spring Batch clearing job on demand...");
            JobExecution execution = jobLauncher.run(dailyClearingJob, params);

            Map<String, Object> response = new HashMap<>();
            response.put("jobId", execution.getId());
            response.put("status", execution.getStatus().name());
            response.put("exitCode", execution.getExitStatus().getExitCode());
            response.put("startTime", execution.getStartTime());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to execute clearing job", e);
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Batch execution failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
    }

    /**
     * Inspects status of a specific batch job execution.
     */
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<Map<String, Object>> getJobStatus(@PathVariable Long jobId) {
        JobExecution execution = jobExplorer.getJobExecution(jobId);
        if (execution == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> status = new HashMap<>();
        status.put("jobId", execution.getId());
        status.put("status", execution.getStatus().name());
        status.put("startTime", execution.getStartTime());
        status.put("endTime", execution.getEndTime());
        status.put("exitStatus", execution.getExitStatus().getExitCode());
        status.put("stepCount", execution.getStepExecutions().size());

        for (StepExecution step : execution.getStepExecutions()) {
            status.put("readCount", step.getReadCount());
            status.put("writeCount", step.getWriteCount());
            status.put("commitCount", step.getCommitCount());
            status.put("rollbackCount", step.getRollbackCount());
        }

        return ResponseEntity.ok(status);
    }

    /**
     * Returns count of unsettled transactions waiting for the next clearing batch.
     */
    @GetMapping("/unsettled-count")
    public ResponseEntity<Map<String, Object>> getUnsettledCount() {
        int count = ledgerRepository.findUnsettledEntries().size();
        return ResponseEntity.ok(Map.of("unsettledEntriesCount", count));
    }
}
