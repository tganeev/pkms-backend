package com.pkms.service;

import com.pkms.dto.PracticeDTO;
import com.pkms.dto.PracticeLinkDTO;
import com.pkms.model.Practice;
import com.pkms.model.PracticeLink;
import com.pkms.repository.PracticeLinkRepository;
import com.pkms.repository.PracticeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PracticeLinkService {

    private static final Logger log = LoggerFactory.getLogger(PracticeLinkService.class);

    private final PracticeLinkRepository practiceLinkRepository;
    private final PracticeRepository practiceRepository;

    @Transactional
    public PracticeLinkDTO addLink(Long sourcePracticeId, Long targetPracticeId) {
        log.info("Adding link from practice {} to practice {}", sourcePracticeId, targetPracticeId);

        // Проверяем, не существует ли уже такая связь
        if (practiceLinkRepository.existsBySourcePracticeIdAndTargetPracticeId(sourcePracticeId, targetPracticeId)) {
            throw new RuntimeException("Link already exists");
        }

        Practice sourcePractice = practiceRepository.findById(sourcePracticeId)
                .orElseThrow(() -> new RuntimeException("Source practice not found"));

        Practice targetPractice = practiceRepository.findById(targetPracticeId)
                .orElseThrow(() -> new RuntimeException("Target practice not found"));

        PracticeLink link = new PracticeLink();
        link.setSourcePractice(sourcePractice);
        link.setTargetPractice(targetPractice);

        PracticeLink savedLink = practiceLinkRepository.save(link);
        log.info("Link saved with ID: {}", savedLink.getId());

        return convertToDTO(savedLink);
    }

    @Transactional
    public void deleteLink(Long linkId) {
        log.info("Deleting link with ID: {}", linkId);
        practiceLinkRepository.deleteById(linkId);
    }

    public List<PracticeLinkDTO> getLinksForPractice(Long practiceId) {
        log.info("Getting links for practice ID: {}", practiceId);

        // Ищем связи, где практика является целевой (получает данные)
        List<PracticeLink> links = practiceLinkRepository.findByTargetPracticeId(practiceId);

        return links.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private PracticeLinkDTO convertToDTO(PracticeLink link) {
        PracticeLinkDTO dto = new PracticeLinkDTO();
        dto.setId(link.getId());
        dto.setSourcePractice(convertToPracticeDTO(link.getSourcePractice()));
        dto.setTargetPractice(convertToPracticeDTO(link.getTargetPractice()));
        return dto;
    }

    private PracticeDTO convertToPracticeDTO(Practice practice) {
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