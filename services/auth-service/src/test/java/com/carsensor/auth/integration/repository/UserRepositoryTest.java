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

    // ============================================================
    // КОНСТАНТЫ ТЕСТОВЫХ ДАННЫХ
    // ============================================================

    private static final String PERMISSION_VIEW_CARS = "VIEW_CARS";
    private static final String PERMISSION_EDIT_CARS = "EDIT_CARS";
    private static final String PERMISSION_DELETE_CARS = "DELETE_CARS";

    private static final String ROLE_USER = "ROLE_USER";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private static final String USER_JOHN = "john_doe";
    private static final String USER_JANE = "jane_smith";
    private static final String USER_BOB = "bob_wilson";
    private static final String USER_ALICE = "alice_wonder";

    private static final String EMAIL_JOHN = "john@example.com";
    private static final String EMAIL_JANE = "jane@example.com";
    private static final String EMAIL_BOB = "bob@example.com";
    private static final String EMAIL_ALICE = "alice@example.com";

    // ============================================================
    // ПОЛЯ КЛАССА
    // ============================================================

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    private LocalDateTime now;

    // ============================================================
    // ИНИЦИАЛИЗАЦИЯ
    // ============================================================

    /**
     * Инициализация тестовых данных перед каждым тестом.
     */
    @BeforeEach
    void setUp() {
        now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

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

        em.createNativeQuery("TRUNCATE TABLE user_roles CASCADE").executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE role_permissions CASCADE").executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE users CASCADE").executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE roles CASCADE").executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE permissions CASCADE").executeUpdate();

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
                .name(PERMISSION_VIEW_CARS)
                .description("Просмотр автомобилей")
                .build();
        entityManager.persist(viewCars);

        var editCars = Permission.builder()
                .name(PERMISSION_EDIT_CARS)
                .description("Редактирование автомобилей")
                .build();
        entityManager.persist(editCars);

        var deleteCars = Permission.builder()
                .name(PERMISSION_DELETE_CARS)
                .description("Удаление автомобилей")
                .build();
        entityManager.persist(deleteCars);

        // Создание ролей
        var userRole = Role.builder()
                .name(ROLE_USER)
                .description("Обычный пользователь")
                .permissions(Set.of(viewCars))
                .build();
        entityManager.persist(userRole);

        var adminRole = Role.builder()
                .name(ROLE_ADMIN)
                .description("Администратор")
                .permissions(Set.of(viewCars, editCars, deleteCars))
                .build();
        entityManager.persist(adminRole);

        // Создание пользователей
        var john = User.builder()
                .username(USER_JOHN)
                .email(EMAIL_JOHN)
                .password("encodedPass123")
                .firstName("John")
                .lastName("Doe")
                .isActive(true)
                .roles(Set.of(userRole))
                .build();
        entityManager.persist(john);

        var jane = User.builder()
                .username(USER_JANE)
                .email(EMAIL_JANE)
                .password("encodedPass456")
                .firstName("Jane")
                .lastName("Smith")
                .isActive(true)
                .roles(Set.of(userRole, adminRole))
                .build();
        entityManager.persist(jane);

        var bob = User.builder()
                .username(USER_BOB)
                .email(EMAIL_BOB)
                .password("encodedPass789")
                .firstName("Bob")
                .lastName("Wilson")
                .isActive(false)
                .roles(Set.of(userRole))
                .build();
        entityManager.persist(bob);

        entityManager.flush();
    }

    /**
     * Устанавливает кастомные даты создания для тестовых пользователей.
     */
    private void setCreatedDates() {
        var em = entityManager.getEntityManager();

        var updateSql = """
                UPDATE users 
                SET created_at = ? 
                WHERE username = ?
                """;

        em.createNativeQuery(updateSql)
                .setParameter(1, now.minusDays(10))
                .setParameter(2, USER_JOHN)
                .executeUpdate();

        em.createNativeQuery(updateSql)
                .setParameter(1, now.minusDays(5))
                .setParameter(2, USER_JANE)
                .executeUpdate();

        em.createNativeQuery(updateSql)
                .setParameter(1, now.minusDays(1))
                .setParameter(2, USER_BOB)
                .executeUpdate();

        entityManager.clear();
    }

    // ============================================================
    // ТЕСТЫ МЕТОДОВ С EntityGraph И JOIN FETCH
    // ============================================================

    @Test
    @DisplayName("1. Поиск пользователя с ролями и разрешениями (EntityGraph)")
    void findByUsername_ShouldReturnUserWithRolesAndPermissions() {
        var found = userRepository.findByUsername(USER_JANE);

        assertThat(found).isPresent();

        var user = found.get();
        assertThat(user.getRoles())
                .as("Пользователь должен иметь 2 роли")
                .hasSize(2);

        assertThat(user.getRoles())
                .extracting(Role::getName)
                .containsExactlyInAnyOrder(ROLE_USER, ROLE_ADMIN);

        var adminRole = user.getRoles().stream()
                .filter(role -> role.getName().equals(ROLE_ADMIN))
                .findFirst()
                .orElseThrow();

        assertThat(adminRole.getPermissions())
                .as("Роль ADMIN должна иметь разрешения")
                .isNotEmpty()
                .extracting(Permission::getName)
                .contains(PERMISSION_VIEW_CARS, PERMISSION_EDIT_CARS, PERMISSION_DELETE_CARS);
    }

    @Test
    @DisplayName("2. Поиск пользователя с ролями (JOIN FETCH)")
    void findByUsernameWithRoles_ShouldReturnUserWithRoles() {
        var found = userRepository.findByUsernameWithRoles(USER_JANE);

        assertThat(found).isPresent();
        assertThat(found.get().getRoles()).hasSize(2);
        assertThat(found.get().getRoles())
                .extracting(Role::getName)
                .containsExactlyInAnyOrder(ROLE_USER, ROLE_ADMIN);
    }

    // ============================================================
    // ТЕСТЫ БАЗОВЫХ CRUD ОПЕРАЦИЙ
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
        var userRole = roleRepository.findByName(ROLE_USER).orElseThrow();

        var newUser = User.builder()
                .username(USER_ALICE)
                .email(EMAIL_ALICE)
                .password("password123")
                .firstName("Alice")
                .lastName("Wonder")
                .isActive(true)
                .roles(Set.of(userRole))
                .build();

        var savedUser = userRepository.save(newUser);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo(USER_ALICE);
        assertThat(savedUser.getEmail()).isEqualTo(EMAIL_ALICE);
        assertThat(savedUser.isActive()).isTrue();

        var found = userRepository.findById(savedUser.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo(USER_ALICE);
    }

    // ============================================================
    // ТЕСТЫ АГРЕГАЦИЙ И СТАТИСТИКИ
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
        assertThat(users.getFirst().getUsername()).isEqualTo(USER_JANE);
    }

    @Test
    @DisplayName("7. Поиск всех активных пользователей с ролями")
    void findAllActiveUsersWithRoles_ShouldReturnOnlyActiveUsers() {
        var activeUsers = userRepository.findAllActiveUsersWithRoles(100);

        assertThat(activeUsers).hasSize(2);
        assertThat(activeUsers)
                .extracting(User::getUsername)
                .containsExactlyInAnyOrder(USER_JOHN, USER_JANE);
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
    // ТЕСТЫ ПОИСКА И СУЩЕСТВОВАНИЯ
    // ============================================================

    @Test
    @DisplayName("10. Поиск пользователя по ID")
    void findById_WithExistingId_ShouldReturnUser() {
        var user = userRepository.findByUsername(USER_JOHN).orElseThrow();
        var found = userRepository.findById(user.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo(USER_JOHN);
    }

    @Test
    @DisplayName("11. Поиск пользователя по username")
    void findByUsername_WithExistingUsername_ShouldReturnUser() {
        var found = userRepository.findByUsername(USER_JOHN);

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo(USER_JOHN);
    }

    @Test
    @DisplayName("12. Поиск пользователя по email")
    void findByEmail_WithExistingEmail_ShouldReturnUser() {
        var found = userRepository.findByEmail(EMAIL_JANE);

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo(USER_JANE);
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
        assertThat(userRepository.existsByUsername(USER_JOHN)).isTrue();
        assertThat(userRepository.existsByUsername("nonexistent")).isFalse();
    }

    @Test
    @DisplayName("15. Проверка существования email")
    void existsByEmail_ShouldReturnTrueForExistingEmail() {
        assertThat(userRepository.existsByEmail(EMAIL_JANE)).isTrue();
        assertThat(userRepository.existsByEmail("nonexistent@example.com")).isFalse();
    }

    // ============================================================
    // ТЕСТЫ ТЕКСТОВОГО ПОИСКА
    // ============================================================

    @Test
    @DisplayName("16. Поиск по имени или фамилии (частичное совпадение)")
    void findByFirstNameOrLastName_ShouldFindUsersByPartialMatch() throws Exception {
        // Ищем по "j" - найдет John (в имени) и Jane (в имени)
        var found = userRepository.searchByFirstNameOrLastName("j");

        assertThat(found).hasSize(2);
        assertThat(found)
                .extracting(User::getUsername)
                .containsExactlyInAnyOrder(USER_JOHN, USER_JANE);
    }

    @Test
    @DisplayName("17. Поиск с частичным совпадением по разным критериям")
    void searchByPartialMatch_ShouldFindUsersByPartialName() {
        var foundByDo = userRepository
                .searchByFirstNameOrLastName("do");
        var foundByWi = userRepository
                .searchByFirstNameOrLastName("wi");

        assertThat(foundByDo).hasSize(1);
        assertThat(foundByWi).hasSize(1);
        assertThat(foundByDo.getFirst().getUsername()).isEqualTo(USER_JOHN);
        assertThat(foundByWi.getFirst().getUsername()).isEqualTo(USER_BOB);
    }

    // ============================================================
    // ТЕСТЫ УДАЛЕНИЯ
    // ============================================================

    @Test
    @DisplayName("18. Удаление пользователя")
    void deleteById_ShouldRemoveUser() {
        var user = userRepository.findByUsername(USER_JOHN).orElseThrow();
        userRepository.deleteById(user.getId());

        entityManager.flush();
        entityManager.clear();

        var found = userRepository.findById(user.getId());
        assertThat(found).isEmpty();

        var remainingUsers = userRepository.findAll();
        assertThat(remainingUsers).hasSize(2);
    }
}