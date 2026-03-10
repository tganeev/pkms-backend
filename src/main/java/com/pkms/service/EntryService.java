package com.pkms.service;

import com.pkms.dto.EntryDTO;
import com.pkms.model.Entry;
import com.pkms.model.User;
import com.pkms.repository.EntryRepository;
import com.pkms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EntryService {

    private final EntryRepository entryRepository;
    private final UserRepository userRepository;

    @Transactional
    public EntryDTO createEntry(EntryDTO entryDTO, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Entry entry = new Entry();
        entry.setUser(user);
        entry.setCategory(entryDTO.getCategory());
        entry.setPractice(entryDTO.getPractice());
        entry.setDuration(entryDTO.getDuration());
        entry.setPeriod(entryDTO.getPeriod());
        entry.setRepeatInterval(entryDTO.getRepeatInterval());
        entry.setEntryDate(entryDTO.getEntryDate());

        Entry savedEntry = entryRepository.save(entry);
        return convertToDTO(savedEntry);
    }

    public List<EntryDTO> getWeekEntries(String username, LocalDate startDate, LocalDate endDate) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return entryRepository
                .findByUserAndEntryDateBetweenOrderByEntryDateAscPeriodAsc(user, startDate, endDate)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteEntry(Long entryId, String username) {
        entryRepository.deleteByUserIdAndId(
                userRepository.findByUsername(username).orElseThrow().getId(),
                entryId
        );
    }

    private EntryDTO convertToDTO(Entry entry) {
        EntryDTO dto = new EntryDTO();
        dto.setId(entry.getId());
        dto.setCategory(entry.getCategory());
        dto.setPractice(entry.getPractice());
        dto.setDuration(entry.getDuration());
        dto.setPeriod(entry.getPeriod());
        dto.setRepeatInterval(entry.getRepeatInterval());
        dto.setEntryDate(entry.getEntryDate());
        return dto;
    }
}