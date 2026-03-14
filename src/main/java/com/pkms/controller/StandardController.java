package com.pkms.controller;

import com.pkms.dto.StandardDTO;
import com.pkms.dto.StandardStatsDTO;
import com.pkms.service.StandardService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/standards")
@RequiredArgsConstructor
public class StandardController {

    private static final Logger log = LoggerFactory.getLogger(StandardController.class);
    private final StandardService standardService;

    @GetMapping("/stats")
    public ResponseEntity<List<StandardStatsDTO>> getAllStandardsStats() {
        log.info("=== GET ALL STANDARDS STATS ===");
        try {
            List<StandardStatsDTO> stats = standardService.getAllStandardsStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting standards stats: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<StandardDTO>> getStandardsByCategory(@PathVariable Long categoryId) {
        log.info("=== GET STANDARDS BY CATEGORY: {} ===", categoryId);
        try {
            List<StandardDTO> standards = standardService.getStandardsByCategory(categoryId);
            return ResponseEntity.ok(standards);
        } catch (Exception e) {
            log.error("Error getting standards: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<StandardDTO> createStandard(@RequestBody StandardDTO standardDTO) {
        log.info("=== CREATE STANDARD ===");
        try {
            StandardDTO created = standardService.createStandard(standardDTO);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            log.error("Error creating standard: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<StandardDTO> updateStandard(@PathVariable Long id, @RequestBody StandardDTO standardDTO) {
        log.info("=== UPDATE STANDARD: {} ===", id);
        try {
            StandardDTO updated = standardService.updateStandard(id, standardDTO);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error updating standard: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStandard(@PathVariable Long id) {
        log.info("=== DELETE STANDARD: {} ===", id);
        try {
            standardService.deleteStandard(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deleting standard: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}