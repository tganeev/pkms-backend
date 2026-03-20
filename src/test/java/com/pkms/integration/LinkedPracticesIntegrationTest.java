package com.pkms.integration;

import com.pkms.model.*;
import com.pkms.repository.*;
import com.pkms.service.CalendarService;
import com.pkms.dto.EntryDTO;
import com.pkms.dto.EntryStatusDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.format_sql=false",
        "logging.level.org.hibernate.SQL=OFF",
        "logging.level.org.hibernate.type.descriptor.sql=OFF",
        "logging.level.org.springframework.jdbc=OFF",
        "logging.level.org.testcontainers=OFF"
})
public class LinkedPracticesIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private CalendarService calendarService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PracticeRepository practiceRepository;

    @Autowired
    private StandardRepository standardRepository;

    @Autowired
    private StandardPracticeRepository standardPracticeRepository;

    @Autowired
    private PracticeLinkRepository practiceLinkRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User testUser;
    private Category coreCategory;
    private Category yogaCategory;
    private Practice coreMeditation;
    private Practice corePlank;
    private Practice coreSquat;
    private Practice yogaMeditation;
    private Standard coreStandard;
    private Standard yogaStandard;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        cleanupDatabase();
        createTestData();

        // Не выводим лишнего, только если тест падает
    }

    private void cleanupDatabase() {
        try {
            jdbcTemplate.execute("SET session_replication_role = 'replica';");
            jdbcTemplate.execute("TRUNCATE TABLE practice_value_logs CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE practice_links CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE entry_status CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE calendar CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE standard_practices CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE standards CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE practices CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE categories CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
            jdbcTemplate.execute("SET session_replication_role = 'origin';");
        } catch (Exception e) {
            // Игнорируем
        }
    }

    private void createTestData() {
        testUser = new User();
        testUser.setUsername("test");
        testUser.setEmail("test@test.com");
        testUser.setName("Test User");
        testUser = userRepository.save(testUser);

        coreCategory = new Category();
        coreCategory.setName("Core");
        coreCategory.setColor("#123456");
        coreCategory = categoryRepository.save(coreCategory);

        yogaCategory = new Category();
        yogaCategory.setName("Yoga");
        yogaCategory.setColor("#654321");
        yogaCategory = categoryRepository.save(yogaCategory);

        coreMeditation = createPractice("Meditation", coreCategory, "minutes");
        corePlank = createPractice("Plank", coreCategory, "minutes");
        coreSquat = createPractice("Squat", coreCategory, "times");
        yogaMeditation = createPractice("Meditation", yogaCategory, "minutes");

        // Стандарт для Yoga
        yogaStandard = new Standard();
        yogaStandard.setName("Yoga Standard");
        yogaStandard.setCategory(yogaCategory);
        yogaStandard.setStartDate(LocalDate.now().minusMonths(1));
        yogaStandard.setEndDate(null);
        yogaStandard = standardRepository.save(yogaStandard);
        addPracticeToStandard(yogaStandard, yogaMeditation, 31, "minutes");

        // Стандарт для Core
        coreStandard = new Standard();
        coreStandard.setName("Core Standard");
        coreStandard.setCategory(coreCategory);
        coreStandard.setStartDate(LocalDate.now().minusMonths(1));
        coreStandard.setEndDate(null);
        coreStandard = standardRepository.save(coreStandard);
        addPracticeToStandard(coreStandard, coreMeditation, 31, "minutes");
        addPracticeToStandard(coreStandard, corePlank, 10, "minutes");
        addPracticeToStandard(coreStandard, coreSquat, 2, "times");

        // Связь
        createPracticeLink(coreMeditation, yogaMeditation);

        testDate = LocalDate.now();
    }

    private Practice createPractice(String name, Category category, String unitType) {
        Practice practice = new Practice();
        practice.setName(name);
        practice.setCategory(category);
        practice.setUnitType(unitType);
        return practiceRepository.save(practice);
    }

    private void addPracticeToStandard(Standard standard, Practice practice, int targetValue, String unitType) {
        StandardPractice sp = new StandardPractice();
        sp.setStandard(standard);
        sp.setPractice(practice);
        sp.setTargetValue(targetValue);
        sp.setUnitType(unitType);
        sp.setIsActive(true);
        standardPracticeRepository.save(sp);
    }

    private void createPracticeLink(Practice source, Practice target) {
        PracticeLink link = new PracticeLink();
        link.setSourcePractice(source);
        link.setTargetPractice(target);
        practiceLinkRepository.save(link);
    }

    private EntryDTO createEntry(String category, String practice, LocalDate date) {
        EntryDTO entry = new EntryDTO();
        entry.setCategory(category);
        entry.setPractice(practice);
        entry.setPeriod("morning");
        entry.setEntryDate(date);
        entry.setRepeatInterval("Не повторять");
        return entry;
    }

    private Map<String, Integer> getAllCoreValues(LocalDate date) {
        Map<String, Integer> values = new HashMap<>();
        values.put("Meditation", getPracticeValue("Core", "Meditation", date));
        values.put("Plank", getPracticeValue("Core", "Plank", date));
        values.put("Squat", getPracticeValue("Core", "Squat", date));
        return values;
    }

    private Integer getPracticeValue(String categoryName, String practiceName, LocalDate date) {
        String tableName = categoryName.toLowerCase() + "_practices";
        String columnName = practiceName.toLowerCase();

        try {
            String query = String.format("SELECT %s FROM %s WHERE entry_date = ?", columnName, tableName);
            return jdbcTemplate.queryForObject(query, Integer.class, date);
        } catch (Exception e) {
            return null;
        }
    }

    @Test
    void completeWorkflowTest() throws Exception {
        System.out.println("\n=================================");
        System.out.println("🔍 ТЕСТ: Полный цикл работы со связанными практиками");
        System.out.println("=================================\n");

        testPassed = true;

        try {
            // ШАГ 1: Проверяем начальные значения
            System.out.println("📌 ШАГ 1: Проверяем начальные значения в БД:");
            Map<String, Integer> initialCore = getAllCoreValues(testDate);
            Integer initialYoga = getPracticeValue("Yoga", "Meditation", testDate);

            System.out.println("  Core.Meditation = " + (initialCore.get("Meditation") == null ? 0 : initialCore.get("Meditation")));
            System.out.println("  Core.Plank = " + (initialCore.get("Plank") == null ? 0 : initialCore.get("Plank")));
            System.out.println("  Core.Squat = " + (initialCore.get("Squat") == null ? 0 : initialCore.get("Squat")));
            System.out.println("  Yoga.Meditation = " + (initialYoga == null ? 0 : initialYoga));

            assertWithMessage("Начальные значения",
                    initialCore.get("Meditation"), initialCore.get("Plank"), initialCore.get("Squat"), initialYoga,
                    0, 0, 0, 0);

            Thread.sleep(500);

            // ШАГ 2: Создаем событие Yoga
            System.out.println("\n📌 ШАГ 2: Создаем событие: Yoga Standard");
            EntryDTO yogaEntry = createEntry("Yoga", "Yoga Standard", testDate);
            EntryDTO savedYogaEntry = calendarService.createEntry(yogaEntry, "test");

            EntryStatusDTO yogaStatus = new EntryStatusDTO();
            yogaStatus.setEntryId(savedYogaEntry.getId());
            yogaStatus.setStatus("completed");
            yogaStatus.setNotes("");

            calendarService.updateEntryStatus(savedYogaEntry.getId(), yogaStatus, "test");
            System.out.println("  ✅ Событие Yoga Standard создано и переведено в статус 'Выполнено полностью'");

            Thread.sleep(500);

            // Проверяем значения после Yoga
            System.out.println("\n📌 Проверяем значения после Yoga:");
            Map<String, Integer> afterYogaCore = getAllCoreValues(testDate);
            Integer afterYoga = getPracticeValue("Yoga", "Meditation", testDate);

            System.out.println("  Core.Meditation = " + (afterYogaCore.get("Meditation") == null ? 0 : afterYogaCore.get("Meditation")));
            System.out.println("  Core.Plank = " + (afterYogaCore.get("Plank") == null ? 0 : afterYogaCore.get("Plank")));
            System.out.println("  Core.Squat = " + (afterYogaCore.get("Squat") == null ? 0 : afterYogaCore.get("Squat")));
            System.out.println("  Yoga.Meditation = " + (afterYoga == null ? 0 : afterYoga));

            assertWithMessage("После Yoga",
                    afterYogaCore.get("Meditation"), afterYogaCore.get("Plank"), afterYogaCore.get("Squat"), afterYoga,
                    0, 0, 0, 31);

            Thread.sleep(500);

            // ШАГ 3: Создаем событие Core
            System.out.println("\n📌 ШАГ 3: Создаем событие: Core Standard");
            EntryDTO coreEntry = createEntry("Core", "Core Standard", testDate);
            EntryDTO savedCoreEntry = calendarService.createEntry(coreEntry, "test");

            EntryStatusDTO coreStatus = new EntryStatusDTO();
            coreStatus.setEntryId(savedCoreEntry.getId());
            coreStatus.setStatus("completed");
            coreStatus.setNotes("");

            calendarService.updateEntryStatus(savedCoreEntry.getId(), coreStatus, "test");
            System.out.println("  ✅ Событие Core Standard создано и переведено в статус 'Выполнено полностью'");

            Thread.sleep(500);

            // Проверяем значения после Core
            System.out.println("\n📌 Проверяем значения после Core:");
            Map<String, Integer> afterCoreCore = getAllCoreValues(testDate);
            Integer afterCoreYoga = getPracticeValue("Yoga", "Meditation", testDate);

            System.out.println("  Core.Meditation = " + (afterCoreCore.get("Meditation") == null ? 0 : afterCoreCore.get("Meditation")));
            System.out.println("  Core.Plank = " + (afterCoreCore.get("Plank") == null ? 0 : afterCoreCore.get("Plank")));
            System.out.println("  Core.Squat = " + (afterCoreCore.get("Squat") == null ? 0 : afterCoreCore.get("Squat")));
            System.out.println("  Yoga.Meditation = " + (afterCoreYoga == null ? 0 : afterCoreYoga));

            assertWithMessage("После Core",
                    afterCoreCore.get("Meditation"), afterCoreCore.get("Plank"), afterCoreCore.get("Squat"), afterCoreYoga,
                    31, 10, 2, 62);

            Thread.sleep(500);

            // ШАГ 4: Удаляем событие Core
            System.out.println("\n📌 ШАГ 4: Удаляем событие: Core Standard");
            calendarService.deleteEntry(savedCoreEntry.getId(), "test");
            System.out.println("  ✅ Событие Core Standard удалено");

            Thread.sleep(500);

            // Проверяем финальные значения
            System.out.println("\n📌 Проверяем значения после удаления Core:");
            Map<String, Integer> finalCore = getAllCoreValues(testDate);
            Integer finalYoga = getPracticeValue("Yoga", "Meditation", testDate);

            System.out.println("  Core.Meditation = " + (finalCore.get("Meditation") == null ? 0 : finalCore.get("Meditation")));
            System.out.println("  Core.Plank = " + (finalCore.get("Plank") == null ? 0 : finalCore.get("Plank")));
            System.out.println("  Core.Squat = " + (finalCore.get("Squat") == null ? 0 : finalCore.get("Squat")));
            System.out.println("  Yoga.Meditation = " + (finalYoga == null ? 0 : finalYoga));

            assertWithMessage("После удаления Core",
                    finalCore.get("Meditation"), finalCore.get("Plank"), finalCore.get("Squat"), finalYoga,
                    0, 0, 0, 31);



        } catch (Exception e) {
            testPassed = false;
            System.out.println("\n❌ ОШИБКА ВЫПОЛНЕНИЯ ТЕСТА: " + e.getMessage());
        }

        // В самом конце, после всех проверок
        if (!testPassed) {
            System.out.println("\n=================================");
            System.out.println("\n❌ ТЕСТ НЕ ПРОЙДЕН");
            System.out.println("=================================");
            System.exit(1); // Завершаем процесс с ошибкой
        } else {
            System.out.println("\n=================================");
            System.out.println("✅ ТЕСТ УСПЕШНО ЗАВЕРШЕН");
            System.out.println("=================================");
        }
    }

    private boolean testPassed = true;

    // Обновите метод assertWithMessage
    private void assertWithMessage(String step, Integer coreMed, Integer corePlank, Integer coreSquat, Integer yogaMed,
                                   int expCoreMed, int expCorePlank, int expCoreSquat, int expYogaMed) {

        List<String> errors = new ArrayList<>();

        int actualCoreMed = coreMed == null ? 0 : coreMed;
        int actualCorePlank = corePlank == null ? 0 : corePlank;
        int actualCoreSquat = coreSquat == null ? 0 : coreSquat;
        int actualYogaMed = yogaMed == null ? 0 : yogaMed;

        if (actualCoreMed != expCoreMed) {
            errors.add(String.format("  • Core.Meditation: ожидалось %d, получено %d", expCoreMed, actualCoreMed));
        }
        if (actualCorePlank != expCorePlank) {
            errors.add(String.format("  • Core.Plank: ожидалось %d, получено %d", expCorePlank, actualCorePlank));
        }
        if (actualCoreSquat != expCoreSquat) {
            errors.add(String.format("  • Core.Squat: ожидалось %d, получено %d", expCoreSquat, actualCoreSquat));
        }
        if (actualYogaMed != expYogaMed) {
            errors.add(String.format("  • Yoga.Meditation: ожидалось %d, получено %d", expYogaMed, actualYogaMed));
        }

        if (!errors.isEmpty()) {
            System.out.println("\n❌ ОШИБКА В ШАГЕ \"" + step + "\":");
            for (String error : errors) {
                System.out.println(error);
            }
            System.out.println();
            testPassed = false;
        } else {
            System.out.println("✅ Значения корректны");
        }
    }
}