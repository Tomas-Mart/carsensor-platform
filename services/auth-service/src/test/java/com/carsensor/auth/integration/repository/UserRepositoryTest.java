package com.carsensor.auth.integration.repository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import com.carsensor.auth.domain.entity.Permission;
import com.carsensor.auth.domain.entity.Role;
import com.carsensor.auth.domain.entity.User;
import com.carsensor.auth.domain.repository.PermissionRepository;
import com.carsensor.auth.domain.repository.RoleRepository;
import com.carsensor.auth.domain.repository.UserRepository;
import com.carsensor.common.test.AbstractJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционные тесты для UserRepository.
 *
 * <p>Проверяют работу репозитория пользователей с использованием Embedded PostgreSQL.
 * Каждый тест выполняется в транзакции с автоматическим откатом.
 *
 * <p><b>Оптимизации:</b>
 * <ul>
 *   <li>Использует {@link AbstractJpaTest} для настройки Embedded PostgreSQL</li>
 *   <li>Автоматический запуск PostgreSQL при загрузке класса</li>
 *   <li>Транзакционная поддержка с автоматическим откатом</li>
 *   <li>Java 21 features: {@code var} и текстовые блоки</li>
 * </ul>
 *
 * @see AbstractJpaTest
 * @see UserRepository
 * @since 1.0
 */
@DataJpaTest
@ActiveProfiles("test")
@ComponentScan(basePackages = "com.carsensor.auth.domain.repository")
@DisplayName("Интеграционные тесты UserRepository")
class UserRepositoryTest extends AbstractJpaTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    private LocalDateTime now;

    /**
     * Инициализация тестовых данных перед каждым тестом.
     */
    @BeforeEach
    void setUp() {
        now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        // Очистка таблиц в правильном порядке
        clearAllTables();
        createTestData();
        setCreatedDates();
    }

    /**
     * Очищает все таблицы в правильном порядке.
     * Использует TRUNCATE для PostgreSQL (быстрее, чем DELETE).
     */
    private void clearAllTables() {
        var em = entityManager.getEntityManager();

        // Используем TRUNCATE с CASCADE для PostgreSQL
        em.createNativeQuery("TRUNCATE TABLE user_roles CASCADE").executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE role_permissions CASCADE").executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE users CASCADE").executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE roles CASCADE").executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE permissions CASCADE").executeUpdate();

        // Сброс последовательностей для PostgreSQL
        em.createNativeQuery("ALTER SEQUENCE users_id_seq RESTART WITH 1").executeUpdate();
        em.createNativeQuery("ALTER SEQUENCE roles_id_seq RESTART WITH 1").executeUpdate();
        em.createNativeQuery("ALTER SEQUENCE permissions_id_seq RESTART WITH 1").executeUpdate();

        entityManager.flush();
        entityManager.clear();
    }

    /**
     * Создает тестовые данные: разрешения, роли и пользователей.
     */
    private void createTestData() {
        // Создание разрешений
        var viewCars = Permission.builder()
                .name("VIEW_CARS")
                .description("Просмотр автомобилей")
                .build();
        entityManager.persist(viewCars);

        var editCars = Permission.builder()
                .name("EDIT_CARS")
                .description("Редактирование автомобилей")
                .build();
        entityManager.persist(editCars);

        var deleteCars = Permission.builder()
                .name("DELETE_CARS")
                .description("Удаление автомобилей")
                .build();
        entityManager.persist(deleteCars);

        // Создание ролей
        var userRole = Role.builder()
                .name("ROLE_USER")
                .description("Обычный пользователь")
                .permissions(Set.of(viewCars))
                .build();
        entityManager.persist(userRole);

        var adminRole = Role.builder()
                .name("ROLE_ADMIN")
                .description("Администратор")
                .permissions(Set.of(viewCars, editCars, deleteCars))
                .build();
        entityManager.persist(adminRole);

        // Создание пользователей
        var user1 = User.builder()
                .username("john_doe")
                .email("john@example.com")
                .password("encodedPass123")
                .firstName("John")
                .lastName("Doe")
                .isActive(true)
                .roles(Set.of(userRole))
                .build();
        entityManager.persist(user1);

        var user2 = User.builder()
                .username("jane_smith")
                .email("jane@example.com")
                .password("encodedPass456")
                .firstName("Jane")
                .lastName("Smith")
                .isActive(true)
                .roles(Set.of(userRole, adminRole))
                .build();
        entityManager.persist(user2);

        var user3 = User.builder()
                .username("bob_wilson")
                .email("bob@example.com")
                .password("encodedPass789")
                .firstName("Bob")
                .lastName("Wilson")
                .isActive(false)
                .roles(Set.of(userRole))
                .build();
        entityManager.persist(user3);

        entityManager.flush();
    }

    /**
     * Устанавливает кастомные даты создания для тестовых пользователей.
     * Использует positional parameters (?) для native query.
     */
    private void setCreatedDates() {
        var em = entityManager.getEntityManager();

        // Positional parameters - первый параметр это date, второй username
        var updateSql = """
                UPDATE users 
                SET created_at = ? 
                WHERE username = ?
                """;

        em.createNativeQuery(updateSql)
                .setParameter(1, now.minusDays(10))
                .setParameter(2, "john_doe")
                .executeUpdate();

        em.createNativeQuery(updateSql)
                .setParameter(1, now.minusDays(5))
                .setParameter(2, "jane_smith")
                .executeUpdate();

        em.createNativeQuery(updateSql)
                .setParameter(1, now.minusDays(1))
                .setParameter(2, "bob_wilson")
                .executeUpdate();

        entityManager.clear();
    }

// ============================================================
// Тесты методов с EntityGraph и JOIN FETCH
// ============================================================

    @Test
    @DisplayName("1. Поиск пользователя с ролями и разрешениями (EntityGraph)")
    void findByUsername_ShouldReturnUserWithRolesAndPermissions() {
        var found = userRepository.findByUsername("jane_smith");

        assertThat(found).isPresent();

        var user = found.get();
        assertThat(user.getRoles())
                .as("Пользователь должен иметь 2 роли")
                .hasSize(2);

        assertThat(user.getRoles())
                .extracting(Role::getName)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");

        // Проверяем, что разрешения загружены
        var adminRole = user.getRoles().stream()
                .filter(r -> r.getName().equals("ROLE_ADMIN"))
                .findFirst()
                .orElseThrow();

        assertThat(adminRole.getPermissions())
                .as("Роль ADMIN должна иметь разрешения")
                .isNotEmpty();
    }

    @Test
    @DisplayName("2. Поиск пользователя с ролями (JOIN FETCH)")
    void findByUsernameWithRoles_ShouldReturnUserWithRoles() {
        var found = userRepository.findByUsernameWithRoles("jane_smith");

        assertThat(found).isPresent();
        assertThat(found.get().getRoles()).hasSize(2);
        assertThat(found.get().getRoles())
                .extracting(Role::getName)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

// ============================================================
// Тесты базовых CRUD операций
// ============================================================

    @Test
    @DisplayName("3. Поиск по несуществующему username")
    void findByUsername_WithNonExistentUsername_ShouldReturnEmpty() {
        var found = userRepository.findByUsername("nonexistent");
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("4. Сохранение нового пользователя")
    void save_WithValidUser_ShouldPersistUser() {
        var userRole = roleRepository.findByName("ROLE_USER").orElseThrow();

        var newUser = User.builder()
                .username("alice_wonder")
                .email("alice@example.com")
                .password("password123")
                .firstName("Alice")
                .lastName("Wonder")
                .isActive(true)
                .roles(Set.of(userRole))
                .build();

        var savedUser = userRepository.save(newUser);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("alice_wonder");
        assertThat(savedUser.getEmail()).isEqualTo("alice@example.com");
        assertThat(savedUser.isActive()).isTrue();

        // Проверяем, что пользователь действительно сохранен
        var found = userRepository.findById(savedUser.getId());
        assertThat(found).isPresent();
    }

// ============================================================
// Тесты агрегаций и статистики
// ============================================================

    @Test
    @DisplayName("5. Подсчет пользователей созданных после даты")
    void countByCreatedAtAfter_ShouldCountUsersCreatedAfterDate() {
        var threeDaysAgo = now.minusDays(3);
        var count = userRepository.countByCreatedAtAfter(threeDaysAgo);

        assertThat(count)
                .as("Должен быть только bob_wilson (создан 1 день назад)")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("6. Поиск пользователей созданных в промежутке")
    void findUsersCreatedBetween_ShouldReturnUsersInDateRange() {
        var start = now.minusDays(6);
        var end = now.minusDays(4);
        var users = userRepository.findUsersCreatedBetween(start, end, 100);

        assertThat(users).hasSize(1);
        assertThat(users.getFirst().getUsername()).isEqualTo("jane_smith");
    }

    @Test
    @DisplayName("7. Поиск всех активных пользователей с ролями")
    void findAllActiveUsersWithRoles_ShouldReturnOnlyActiveUsers() {
        var activeUsers = userRepository.findAllActiveUsersWithRoles(100);

        assertThat(activeUsers).hasSize(2);
        assertThat(activeUsers)
                .extracting(User::getUsername)
                .containsExactlyInAnyOrder("john_doe", "jane_smith");
    }

    @Test
    @DisplayName("8. Подсчет активных пользователей")
    void countByIsActiveTrue_ShouldReturnCountOfActiveUsers() {
        var activeCount = userRepository.countByIsActiveTrue();

        assertThat(activeCount)
                .as("Должно быть 2 активных пользователя: john_doe и jane_smith")
                .isEqualTo(2);
    }

    @Test
    @DisplayName("9. Подсчет пользователей по статусу активности")
    void countByActiveStatus_ShouldCountUsersByStatus() {
        var activeCount = userRepository.countByActiveStatus(true);
        var inactiveCount = userRepository.countByActiveStatus(false);

        assertThat(activeCount).isEqualTo(2);
        assertThat(inactiveCount).isEqualTo(1);
    }

// ============================================================
// Тесты поиска и существования
// ============================================================

    @Test
    @DisplayName("10. Поиск пользователя по ID")
    void findById_WithExistingId_ShouldReturnUser() {
        var user = userRepository.findByUsername("john_doe").orElseThrow();
        var found = userRepository.findById(user.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("john_doe");
    }

    @Test
    @DisplayName("11. Поиск пользователя по username")
    void findByUsername_WithExistingUsername_ShouldReturnUser() {
        var found = userRepository.findByUsername("john_doe");

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("john_doe");
    }

    @Test
    @DisplayName("12. Поиск пользователя по email")
    void findByEmail_WithExistingEmail_ShouldReturnUser() {
        var found = userRepository.findByEmail("jane@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("jane_smith");
    }

    @Test
    @DisplayName("13. Поиск по несуществующему email")
    void findByEmail_WithNonExistentEmail_ShouldReturnEmpty() {
        var found = userRepository.findByEmail("nonexistent@example.com");
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("14. Проверка существования username")
    void existsByUsername_ShouldReturnTrueForExistingUser() {
        assertThat(userRepository.existsByUsername("john_doe")).isTrue();
        assertThat(userRepository.existsByUsername("nonexistent")).isFalse();
    }

    @Test
    @DisplayName("15. Проверка существования email")
    void existsByEmail_ShouldReturnTrueForExistingEmail() {
        assertThat(userRepository.existsByEmail("jane@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("nonexistent@example.com")).isFalse();
    }

// ============================================================
// Тесты текстового поиска
// ============================================================

    @Test
    @DisplayName("16. Поиск по имени или фамилии (частичное совпадение)")
    void findByFirstNameOrLastName_ShouldFindUsersByPartialMatch() {
        var found = userRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase("john", "smith");

        assertThat(found).hasSize(2);
        assertThat(found)
                .extracting(User::getUsername)
                .containsExactlyInAnyOrder("john_doe", "jane_smith");
    }

    @Test
    @DisplayName("17. Поиск с частичным совпадением по разным критериям")
    void searchByPartialMatch_ShouldFindUsersByPartialName() {
        var foundByDo = userRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase("do", "do");
        var foundByWi = userRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase("wi", "wi");

        assertThat(foundByDo).hasSize(1);
        assertThat(foundByWi).hasSize(1);
        assertThat(foundByDo.getFirst().getUsername()).isEqualTo("john_doe");
        assertThat(foundByWi.getFirst().getUsername()).isEqualTo("bob_wilson");
    }

// ============================================================
// Тесты удаления
// ============================================================

    @Test
    @DisplayName("18. Удаление пользователя")
    void deleteById_ShouldRemoveUser() {
        var user = userRepository.findByUsername("john_doe").orElseThrow();
        userRepository.deleteById(user.getId());

        entityManager.flush();
        entityManager.clear();

        var found = userRepository.findById(user.getId());
        assertThat(found).isEmpty();

        // Проверяем, что другие пользователи остались
        var remainingUsers = userRepository.findAll();
        assertThat(remainingUsers).hasSize(2);
    }
}