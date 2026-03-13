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
    private final DynamicTableService dynamicTableService;

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

        // Создаем динамическую таблицу для категории
        dynamicTableService.createCategoryTable(savedCategory.getName());

        // Если есть практики, создаем их и добавляем колонки
        if (categoryDTO.getPractices() != null) {
            for (PracticeDTO practiceDTO : categoryDTO.getPractices()) {
                Practice practice = new Practice();
                practice.setCategory(savedCategory);
                practice.setName(practiceDTO.getName());
                practice.setDescription(practiceDTO.getDescription());
                practice.setUnitType(practiceDTO.getUnitType());
                practice.setDisplayOrder(practiceDTO.getDisplayOrder());
                practiceRepository.save(practice);

                // Добавляем колонку в динамическую таблицу
                dynamicTableService.addPracticeColumn(
                        savedCategory.getName(),
                        practiceDTO.getName(),
                        practiceDTO.getUnitType()
                );
            }
        }

        return convertToDTO(savedCategory);
    }

    @Transactional
    public CategoryDTO updateCategory(Long id, CategoryDTO categoryDTO) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));

        String oldCategoryName = category.getName();
        String newCategoryName = categoryDTO.getName();

        category.setName(newCategoryName);
        category.setDescription(categoryDTO.getDescription());
        category.setColor(categoryDTO.getColor());
        category.setIcon(categoryDTO.getIcon());

        Category updatedCategory = categoryRepository.save(category);

        // Если имя категории изменилось, нужно переименовать таблицу
        if (!oldCategoryName.equals(newCategoryName)) {
            // TODO: реализовать переименование таблицы
            // Это сложнее, так как нужно переименовать саму таблицу
            // Пока просто создадим новую таблицу, если ее нет
            dynamicTableService.createCategoryTable(newCategoryName);
        }

        return convertToDTO(updatedCategory);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));

        String categoryName = category.getName();

        // Сначала удаляем все практики категории
        practiceRepository.deleteByCategoryId(id);

        // Удаляем динамическую таблицу
        try {
            dynamicTableService.dropCategoryTable(categoryName);
        } catch (Exception e) {
            System.err.println("❌ Ошибка при удалении таблицы " + categoryName + ": " + e.getMessage());
            // Продолжаем выполнение, даже если таблица не удалилась
        }

        // Затем удаляем категорию
        categoryRepository.deleteById(id);

        System.out.println("✅ Категория '" + categoryName + "' и связанные данные удалены");
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

        // Добавляем колонку в динамическую таблицу
        dynamicTableService.addPracticeColumn(
                category.getName(),
                practiceDTO.getName(),
                practiceDTO.getUnitType()
        );

        return convertToPracticeDTO(savedPractice);
    }

    @Transactional
    public PracticeDTO updatePractice(Long practiceId, PracticeDTO practiceDTO) {
        Practice practice = practiceRepository.findById(practiceId)
                .orElseThrow(() -> new RuntimeException("Practice not found with id: " + practiceId));

        String oldPracticeName = practice.getName();
        String newPracticeName = practiceDTO.getName();

        practice.setName(newPracticeName);
        practice.setDescription(practiceDTO.getDescription());
        practice.setUnitType(practiceDTO.getUnitType());
        practice.setDisplayOrder(practiceDTO.getDisplayOrder());

        Practice updatedPractice = practiceRepository.save(practice);

        // Если имя практики изменилось, переименовываем колонку
        if (!oldPracticeName.equals(newPracticeName)) {
            dynamicTableService.renamePracticeColumn(
                    practice.getCategory().getName(),
                    oldPracticeName,
                    newPracticeName
            );
        }

        return convertToPracticeDTO(updatedPractice);
    }



    @Transactional
    public void deletePractice(Long practiceId) {
        Practice practice = practiceRepository.findById(practiceId)
                .orElseThrow(() -> new RuntimeException("Practice not found with id: " + practiceId));

        // Удаляем колонку из динамической таблицы
        dynamicTableService.dropPracticeColumn(
                practice.getCategory().getName(),
                practice.getName()
        );

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