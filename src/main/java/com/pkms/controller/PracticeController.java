package com.pkms.controller;

import com.pkms.dto.PracticeDTO;
import com.pkms.model.Practice;
import com.pkms.repository.PracticeRepository;
import com.pkms.service.PracticeLinkService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;
import com.pkms.dto.*;

@RestController
@RequestMapping("/api/practices")
@RequiredArgsConstructor
public class PracticeController {

    private static final Logger log = LoggerFactory.getLogger(PracticeController.class);

    private final PracticeRepository practiceRepository;
    private final PracticeLinkService practiceLinkService;

    @GetMapping("/all")
    public ResponseEntity<List<PracticeDTO>> getAllPractices() {
        log.info("=== GET ALL PRACTICES ===");
        try {
            List<Practice> practices = practiceRepository.findAll();
            List<PracticeDTO> practiceDTOs = practices.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(practiceDTOs);
        } catch (Exception e) {
            log.error("Error getting all practices: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{practiceId}/links")
    public ResponseEntity<List<PracticeLinkDTO>> getPracticeLinks(@PathVariable Long practiceId) {
        log.info("=== GET PRACTICE LINKS for practice ID: {} ===", practiceId);
        try {
            List<PracticeLinkDTO> links = practiceLinkService.getLinksForPractice(practiceId);
            return ResponseEntity.ok(links);
        } catch (Exception e) {
            log.error("Error getting practice links: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/links")
    public ResponseEntity<PracticeLinkDTO> addPracticeLink(@RequestBody AddPracticeLinkRequest request) {
        log.info("=== ADD PRACTICE LINK ===");
        log.info("Source: {}, Target: {}", request.getSourcePracticeId(), request.getTargetPracticeId());
        try {
            PracticeLinkDTO link = practiceLinkService.addLink(
                    request.getSourcePracticeId(),
                    request.getTargetPracticeId()
            );
            return ResponseEntity.ok(link);
        } catch (Exception e) {
            log.error("Error adding practice link: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/links/{linkId}")
    public ResponseEntity<Void> deletePracticeLink(@PathVariable Long linkId) {
        log.info("=== DELETE PRACTICE LINK ID: {} ===", linkId);
        try {
            practiceLinkService.deleteLink(linkId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deleting practice link: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private PracticeDTO convertToDTO(Practice practice) {
        PracticeDTO dto = new PracticeDTO();
        dto.setId(practice.getId());
        dto.setName(practice.getName());
        dto.setDescription(practice.getDescription());
        dto.setUnitType(practice.getUnitType());
        dto.setDisplayOrder(practice.getDisplayOrder());
        dto.setCategoryId(practice.getCategory().getId());
        dto.setCategoryName(practice.getCategory().getName());
        return dto;
    }
}