package com.pkms.service;

import com.pkms.dto.StandardDTO;
import com.pkms.dto.StandardPracticeDTO;
import com.pkms.model.*;
import com.pkms.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StandardService {

    private static final Logger log = LoggerFactory.getLogger(StandardService.class);

    private final StandardRepository standardRepository;
    private final StandardPracticeRepository standardPracticeRepository;
    private final CategoryRepository categoryRepository;
    private final PracticeRepository practiceRepository;

    @Transactional
    public StandardDTO createStandard(StandardDTO standardDTO) {
        log.info("Creating standard: {}", standardDTO);

        Category category = categoryRepository.findById(standardDTO.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Standard standard = new Standard();
        standard.setName(standardDTO.getName());
        standard.setDescription(standardDTO.getDescription());
        standard.setCategory(category);

        Standard savedStandard = standardRepository.save(standard);
        log.info("Standard saved with ID: {}", savedStandard.getId());

        // Добавляем практики в стандарт
        if (standardDTO.getPractices() != null && !standardDTO.getPractices().isEmpty()) {
            for (StandardPracticeDTO practiceDTO : standardDTO.getPractices()) {
                if (Boolean.TRUE.equals(practiceDTO.getIsActive())) {
                    Practice practice = practiceRepository.findById(practiceDTO.getPracticeId())
                            .orElseThrow(() -> new RuntimeException("Practice not found"));

                    StandardPractice standardPractice = new StandardPractice();
                    standardPractice.setStandard(savedStandard);
                    standardPractice.setPractice(practice);
                    standardPractice.setTargetValue(practiceDTO.getTargetValue());
                    standardPractice.setUnitType(practiceDTO.getUnitType());
                    standardPractice.setIsActive(true);

                    standardPracticeRepository.save(standardPractice);
                    log.info("Added practice {} to standard with target: {} {}",
                            practice.getName(), practiceDTO.getTargetValue(), practiceDTO.getUnitType());
                }
            }
        }

        return convertToDTO(savedStandard);
    }

    @Transactional
    public StandardDTO updateStandard(Long id, StandardDTO standardDTO) {
        log.info("Updating standard ID: {}", id);

        Standard standard = standardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Standard not found"));

        standard.setName(standardDTO.getName());
        standard.setDescription(standardDTO.getDescription());

        // Удаляем старые практики
        standardPracticeRepository.deleteByStandardId(id);

        // Добавляем новые
        if (standardDTO.getPractices() != null && !standardDTO.getPractices().isEmpty()) {
            for (StandardPracticeDTO practiceDTO : standardDTO.getPractices()) {
                if (Boolean.TRUE.equals(practiceDTO.getIsActive())) {
                    Practice practice = practiceRepository.findById(practiceDTO.getPracticeId())
                            .orElseThrow(() -> new RuntimeException("Practice not found"));

                    StandardPractice standardPractice = new StandardPractice();
                    standardPractice.setStandard(standard);
                    standardPractice.setPractice(practice);
                    standardPractice.setTargetValue(practiceDTO.getTargetValue());
                    standardPractice.setUnitType(practiceDTO.getUnitType());
                    standardPractice.setIsActive(true);

                    standardPracticeRepository.save(standardPractice);
                }
            }
        }

        return convertToDTO(standard);
    }

    @Transactional
    public void deleteStandard(Long id) {
        log.info("Deleting standard ID: {}", id);
        standardPracticeRepository.deleteByStandardId(id);
        standardRepository.deleteById(id);
    }

    public List<StandardDTO> getStandardsByCategory(Long categoryId) {
        log.info("Getting standards for category ID: {}", categoryId);

        List<Standard> standards = standardRepository.findByCategoryIdOrderByCreatedAtDesc(categoryId);
        return standards.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }



    private StandardDTO convertToDTO(Standard standard) {
        StandardDTO dto = new StandardDTO();
        dto.setId(standard.getId());
        dto.setName(standard.getName());
        dto.setDescription(standard.getDescription());
        dto.setCategoryId(standard.getCategory().getId());
        dto.setCategoryName(standard.getCategory().getName());

        List<StandardPracticeDTO> practices = standardPracticeRepository.findByStandardId(standard.getId())
                .stream()
                .map(sp -> {
                    StandardPracticeDTO spDto = new StandardPracticeDTO();
                    spDto.setId(sp.getId());
                    spDto.setPracticeId(sp.getPractice().getId());
                    spDto.setPracticeName(sp.getPractice().getName());
                    spDto.setTargetValue(sp.getTargetValue());
                    spDto.setUnitType(sp.getUnitType());
                    spDto.setIsActive(sp.getIsActive());
                    return spDto;
                })
                .collect(Collectors.toList());

        dto.setPractices(practices);
        return dto;
    }
}