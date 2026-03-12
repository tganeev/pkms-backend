package com.pkms.service;

import com.pkms.dto.CategoryDTO;
import com.pkms.dto.PracticeDTO;
import com.pkms.model.Category;
import com.pkms.model.Practice;
import com.pkms.repository.CategoryRepository;
import com.pkms.repository.PracticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryManagementService {

    private final CategoryRepository categoryRepository;
    private final PracticeRepository practiceRepository;

    @Transactional
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        // Проверяем, существует ли уже категория с таким именем
        if (categoryRepository.findByName(categoryDTO.getName()).isPresent()) {
            throw new RuntimeException("Category with name '" + categoryDTO.getName() + "' already exists");
        }

        Category category = new Category();
        category.setName(categoryDTO.getName());
        category.setDescription(categoryDTO.getDescription());
        category.setColor(categoryDTO.getColor());
        category.setIcon(categoryDTO.getIcon());

        Category savedCategory = categoryRepository.save(category);

        // Если есть практики, создаем их
        if (categoryDTO.getPractices() != null) {
            for (PracticeDTO practiceDTO : categoryDTO.getPractices()) {
                Practice practice = new Practice();
                practice.setCategory(savedCategory);
                practice.setName(practiceDTO.getName());
                practice.setDescription(practiceDTO.getDescription());
                practice.setUnitType(practiceDTO.getUnitType());
                practice.setDisplayOrder(practiceDTO.getDisplayOrder());
                practiceRepository.save(practice);
            }
        }

        return convertToDTO(savedCategory);
    }

    @Transactional
    public CategoryDTO updateCategory(Long id, CategoryDTO categoryDTO) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));

        category.setName(categoryDTO.getName());
        category.setDescription(categoryDTO.getDescription());
        category.setColor(categoryDTO.getColor());
        category.setIcon(categoryDTO.getIcon());

        Category updatedCategory = categoryRepository.save(category);
        return convertToDTO(updatedCategory);
    }

    @Transactional
    public void deleteCategory(Long id) {
        // Сначала удаляем все практики категории
        practiceRepository.deleteByCategoryId(id);
        // Затем удаляем категорию
        categoryRepository.deleteById(id);
    }

    @Transactional
    public PracticeDTO addPractice(Long categoryId, PracticeDTO practiceDTO) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));

        Practice practice = new Practice();
        practice.setCategory(category);
        practice.setName(practiceDTO.getName());
        practice.setDescription(practiceDTO.getDescription());
        practice.setUnitType(practiceDTO.getUnitType());
        practice.setDisplayOrder(practiceDTO.getDisplayOrder());

        Practice savedPractice = practiceRepository.save(practice);
        return convertToPracticeDTO(savedPractice);
    }

    @Transactional
    public PracticeDTO updatePractice(Long practiceId, PracticeDTO practiceDTO) {
        Practice practice = practiceRepository.findById(practiceId)
                .orElseThrow(() -> new RuntimeException("Practice not found with id: " + practiceId));

        practice.setName(practiceDTO.getName());
        practice.setDescription(practiceDTO.getDescription());
        practice.setUnitType(practiceDTO.getUnitType());
        practice.setDisplayOrder(practiceDTO.getDisplayOrder());

        Practice updatedPractice = practiceRepository.save(practice);
        return convertToPracticeDTO(updatedPractice);
    }

    @Transactional
    public void deletePractice(Long practiceId) {
        practiceRepository.deleteById(practiceId);
    }

    public CategoryDTO convertToDTO(Category category) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setColor(category.getColor());
        dto.setIcon(category.getIcon());

        List<PracticeDTO> practices = practiceRepository.findByCategoryId(category.getId())
                .stream()
                .map(this::convertToPracticeDTO)
                .collect(Collectors.toList());
        dto.setPractices(practices);

        return dto;
    }

    private PracticeDTO convertToPracticeDTO(Practice practice) {
        PracticeDTO dto = new PracticeDTO();
        dto.setId(practice.getId());
        dto.setName(practice.getName());
        dto.setDescription(practice.getDescription());
        dto.setUnitType(practice.getUnitType());
        dto.setDisplayOrder(practice.getDisplayOrder());
        return dto;
    }
}