package com.pkms.controller;

import com.pkms.dto.CategoryDTO;
import com.pkms.dto.PracticeDTO;
import com.pkms.dto.StandardDTO;
import com.pkms.service.StandardService;
import com.pkms.dto.CategoryTableDTO;
import com.pkms.model.Category;
import com.pkms.repository.CategoryRepository;
import com.pkms.service.CategoryTableService;
import com.pkms.service.CategoryManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final CategoryTableService categoryTableService;
    private final CategoryManagementService categoryManagementService;
    private final StandardService standardService;

    @GetMapping("/{categoryId}/standards")
    public List<StandardDTO> getCategoryStandards(@PathVariable Long categoryId) {
        return standardService.getStandardsByCategory(categoryId);
    }

    @PostMapping("/{categoryId}/standards")
    public StandardDTO createStandard(@PathVariable Long categoryId, @RequestBody StandardDTO standardDTO) {
        standardDTO.setCategoryId(categoryId);
        return standardService.createStandard(standardDTO);
    }

    @PutMapping("/standards/{standardId}")
    public StandardDTO updateStandard(@PathVariable Long standardId, @RequestBody StandardDTO standardDTO) {
        return standardService.updateStandard(standardId, standardDTO);
    }

    @DeleteMapping("/standards/{standardId}")
    public void deleteStandard(@PathVariable Long standardId) {
        standardService.deleteStandard(standardId);
    }

    @GetMapping
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @GetMapping("/{id}")
    public CategoryDTO getCategory(@PathVariable Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        return categoryManagementService.convertToDTO(category);
    }

    @PostMapping
    public CategoryDTO createCategory(@RequestBody CategoryDTO categoryDTO) {
        return categoryManagementService.createCategory(categoryDTO);
    }

    @PutMapping("/{id}")
    public CategoryDTO updateCategory(@PathVariable Long id, @RequestBody CategoryDTO categoryDTO) {
        return categoryManagementService.updateCategory(id, categoryDTO);
    }

    @DeleteMapping("/{id}")
    public void deleteCategory(@PathVariable Long id) {
        categoryManagementService.deleteCategory(id);
    }

    @GetMapping("/{name}/table")
    public CategoryTableDTO getCategoryTable(
            @PathVariable String name,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return categoryTableService.getCategoryTable(name, startDate, endDate);
    }

    // Управление практиками
    @PostMapping("/{categoryId}/practices")
    public PracticeDTO addPractice(@PathVariable Long categoryId, @RequestBody PracticeDTO practiceDTO) {
        return categoryManagementService.addPractice(categoryId, practiceDTO);
    }

    @PutMapping("/practices/{practiceId}")
    public PracticeDTO updatePractice(@PathVariable Long practiceId, @RequestBody PracticeDTO practiceDTO) {
        return categoryManagementService.updatePractice(practiceId, practiceDTO);
    }

    @DeleteMapping("/practices/{practiceId}")
    public void deletePractice(@PathVariable Long practiceId) {
        categoryManagementService.deletePractice(practiceId);
    }
}