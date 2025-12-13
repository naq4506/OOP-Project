package com.example.server.controller;

import com.example.server.dto.AnalysisRequest;
import com.example.server.dto.AnalysisResponse;
import com.example.server.service.DisasterService;
import com.example.server.service.analyzer.AnalyzerFactory;
import com.example.server.stats.DamageStats;
import com.example.server.stats.ReliefStats;
import com.example.server.stats.SentimentStats;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/disaster")
public class DisasterController {

    private final DisasterService disasterService;

    public DisasterController(DisasterService disasterService) {
        this.disasterService = disasterService;
    }

    @PostMapping("/analyze/damage")
    public ResponseEntity<AnalysisResponse<DamageStats>> analyzeDamage(@RequestBody AnalysisRequest request) {
        return analyzeWithType(request, AnalyzerFactory.AnalyzerType.DAMAGE);
    }

    @PostMapping("/analyze/relief")
    public ResponseEntity<AnalysisResponse<Map<String, ReliefStats>>> analyzeRelief(@RequestBody AnalysisRequest request) {
        return analyzeWithType(request, AnalyzerFactory.AnalyzerType.RELIEF);
    }

    @PostMapping("/analyze/sentiment")
    public ResponseEntity<AnalysisResponse<Map<LocalDate, SentimentStats>>> analyzeSentiment(@RequestBody AnalysisRequest request) {
        return analyzeWithType(request, AnalyzerFactory.AnalyzerType.SENTIMENT);
    }

    @PostMapping("/analyze/relief-timeline")
    public ResponseEntity<AnalysisResponse<Map<String, Map<LocalDate, ReliefStats>>>> analyzeReliefTimeline(@RequestBody AnalysisRequest request) {
        return analyzeWithType(request, AnalyzerFactory.AnalyzerType.RELIEF_TIMELINE);
    }

    @PostMapping("/analyze/all")
    public ResponseEntity<AnalysisResponse<Map<String, Object>>> analyzeAll(@RequestBody AnalysisRequest request) {
        ValidationResult validation = validateRequest(request);
        if (!validation.isValid()) {
            return ResponseEntity.badRequest()
                    .body(AnalysisResponse.error(validation.getErrorResponse().getErrorMessage()));
        }

        try {
            return ResponseEntity.ok(disasterService.processAllAnalysis(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AnalysisResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalysisResponse.error("Server error: " + e.getMessage()));
        }
    }

    private <T> ResponseEntity<AnalysisResponse<T>> analyzeWithType(
            AnalysisRequest request,
            AnalyzerFactory.AnalyzerType type
    ) {
        ValidationResult validation = validateRequest(request);
        if (!validation.isValid()) {
            return ResponseEntity.badRequest()
                    .body(AnalysisResponse.error(validation.getErrorResponse().getErrorMessage()));
        }

        try {
            return ResponseEntity.ok(disasterService.processSingleAnalysis(request, type));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AnalysisResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalysisResponse.error("Server error: " + e.getMessage()));
        }
    }

    private ValidationResult validateRequest(AnalysisRequest request) {
        if (request.getDisasterName() == null || request.getDisasterName().isEmpty()) {
            return ValidationResult.invalid("Disaster name must not be empty");
        }
        if (request.getStartDate() == null || request.getEndDate() == null ||
                request.getStartDate().isEmpty() || request.getEndDate().isEmpty()) {
            return ValidationResult.invalid("Start date and end date must not be empty");
        }
        try {
            LocalDate s = LocalDate.parse(request.getStartDate());
            LocalDate e = LocalDate.parse(request.getEndDate());
            if (s.isAfter(e)) {
                return ValidationResult.invalid("Start date cannot be after end date");
            }
        } catch (DateTimeParseException e) {
            return ValidationResult.invalid("Invalid date format (YYYY-MM-DD)");
        }
        if (request.getPlatforms() == null || request.getPlatforms().isEmpty()) {
            return ValidationResult.invalid("At least one platform must be provided");
        }
        return ValidationResult.valid();
    }

    private static class ValidationResult {
        private final boolean valid;
        private final AnalysisResponse<?> errorResponse;

        private ValidationResult(boolean valid, AnalysisResponse<?> resp) {
            this.valid = valid;
            this.errorResponse = resp;
        }

        static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        static ValidationResult invalid(String msg) {
            return new ValidationResult(false, AnalysisResponse.error(msg));
        }

        public boolean isValid() { return valid; }
        public AnalysisResponse<?> getErrorResponse() { return errorResponse; }
    }
}