package com.carsensor.auth.integration.repository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import com.carsensor.auth.domain.entity.Permission;
import com.carsensor.auth.domain.entity.Role;
import com.carsensor.auth.domain.entity.User;
import com.carsensor.auth.domain.repository.PermissionRepository;
import com.carsensor.auth.domain.repository.RoleRepository;
import com.carsensor.auth.domain.repository.UserRepository;
import com.carsensor.common.test.AbstractJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

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

    private Role userRole;
    private Role adminRole;
    private Permission viewCars;
    private Permission editCars;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        userRepository.deleteAll();
        roleRepository.deleteAll();
        permissionRepository.deleteAll();

        // 1. Создаем и сохраняем Permission
        viewCars = Permission.builder()
                .name("VIEW_CARS")
                .description("Просмотр автомобилей")
                .build();
        entityManager.persist(viewCars);

        editCars = Permission.builder()
                .name("EDIT_CARS")
                .description("Редактирование автомобилей")
                .build();
        entityManager.persist(editCars);

        // 2. Создаем роли
        userRole = Role.builder()
                .name("ROLE_USER")
                .description("Обычный пользователь")
                .permissions(Set.of(viewCars))
                .build();
        entityManager.persist(userRole);

        adminRole = Role.builder()
                .name("ROLE_ADMIN")
                .description("Администратор")
                .permissions(Set.of(viewCars, editCars))
                .build();
        entityManager.persist(adminRole);

        // 3. Создаем пользователей
        User user1 = User.builder()
                .username("john_doe")
                .email("john@example.com")
                .password("encodedPass123")
                .firstName("John")
                .lastName("Doe")
                .isActive(true)
                .roles(Set.of(userRole))
                .build();
        entityManager.persist(user1);

        User user2 = User.builder()
                .username("jane_smith")
                .email("jane@example.com")
                .password("encodedPass456")
                .firstName("Jane")
                .lastName("Smith")
                .isActive(true)
                .roles(Set.of(userRole, adminRole))
                .build();
        entityManager.persist(user2);

        User user3 = User.builder()
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

        // 4. Устанавливаем разные даты создания через нативный SQL
        entityManager.getEntityManager()
                .createNativeQuery("UPDATE users SET created_at = ? WHERE username = ?")
                .setParameter(1, now.minusDays(10))
                .setParameter(2, "john_doe")
                .executeUpdate();

        entityManager.getEntityManager()
                .createNativeQuery("UPDATE users SET created_at = ? WHERE username = ?")
                .setParameter(1, now.minusDays(5))
                .setParameter(2, "jane_smith")
                .executeUpdate();

        entityManager.getEntityManager()
                .createNativeQuery("UPDATE users SET created_at = ? WHERE username = ?")
                .setParameter(1, now.minusDays(1))
                .setParameter(2, "bob_wilson")
                .executeUpdate();

        entityManager.clear();
    }

    @Test
    @DisplayName("4. Подсчет пользователей созданных после даты")
    void testCountByCreatedAtAfter() {
        LocalDateTime threeDaysAgo = now.minusDays(3);
        long count = userRepository.countByCreatedAtAfter(threeDaysAgo);
        assertThat(count).isEqualTo(1); // Только bob_wilson
    }

    @Test
    @DisplayName("5. Поиск пользователей созданных в промежутке")
    void testFindUsersCreatedBetween() {
        LocalDateTime start = now.minusDays(6);
        LocalDateTime end = now.minusDays(4);
        List<User> users = userRepository.findUsersCreatedBetween(start, end);

        assertThat(users).hasSize(1);
        assertThat(users.getFirst().getUsername()).isEqualTo("jane_smith");
    }

    // Остальные тесты остаются без изменений...
    @Test
    @DisplayName("1. Поиск пользователя с ролями")
    void testFindByUsernameWithRoles() {
        Optional<User> found = userRepository.findByUsernameWithRoles("jane_smith");
        assertThat(found).isPresent();
        User user = found.get();
        assertThat(user.getRoles()).hasSize(2);
        assertThat(user.getRoles()).extracting(Role::getName)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("2. Поиск по несуществующему username")
    void testFindByUsernameNotFound() {
        Optional<User> found = userRepository.findByUsername("nonexistent");
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("3. Сохранение нового пользователя")
    void testSaveUser() {
        User newUser = User.builder()
                .username("alice")
                .email("alice@example.com")
                .password("password")
                .firstName("Alice")
                .lastName("Wonder")
                .isActive(true)
                .roles(Set.of(userRole))
                .build();

        User savedUser = userRepository.save(newUser);
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("alice");
    }

    @Test
    @DisplayName("6. Поиск всех активных пользователей с ролями")
    void testFindAllActiveUsersWithRoles() {
        List<User> activeUsers = userRepository.findAllActiveUsersWithRoles();
        assertThat(activeUsers).hasSize(2);
        assertThat(activeUsers).extracting(User::getUsername)
                .containsExactlyInAnyOrder("john_doe", "jane_smith");
    }

    @Test
    @DisplayName("7. Подсчет активных пользователей")
    void testCountByIsActiveTrue() {
        long activeCount = userRepository.countByIsActiveTrue();
        assertThat(activeCount).isEqualTo(2);
    }

    @Test
    @DisplayName("8. Поиск по несуществующему email")
    void testFindByEmailNotFound() {
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("9. Поиск пользователя по ID")
    void testFindById() {
        User user = userRepository.findByUsername("john_doe").orElseThrow();
        Optional<User> found = userRepository.findById(user.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("john_doe");
    }

    @Test
    @DisplayName("10. Поиск пользователя по username")
    void testFindByUsername() {
        Optional<User> found = userRepository.findByUsername("john_doe");
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("john_doe");
    }

    @Test
    @DisplayName("11. Проверка существования username")
    void testExistsByUsername() {
        assertThat(userRepository.existsByUsername("john_doe")).isTrue();
        assertThat(userRepository.existsByUsername("nonexistent")).isFalse();
    }

    @Test
    @DisplayName("12. Поиск по имени или фамилии")
    void testFindByFirstNameOrLastName() {
        List<User> found = userRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase("john", "smith");
        assertThat(found).hasSize(2);
    }

    @Test
    @DisplayName("13. Поиск пользователя по email")
    void testFindByEmail() {
        Optional<User> found = userRepository.findByEmail("jane@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("jane_smith");
    }

    @Test
    @DisplayName("14. Подсчет пользователей по статусу активности")
    void testCountByActiveStatus() {
        long activeCount = userRepository.countByActiveStatus(true);
        long inactiveCount = userRepository.countByActiveStatus(false);
        assertThat(activeCount).isEqualTo(2);
        assertThat(inactiveCount).isEqualTo(1);
    }

    @Test
    @DisplayName("15. Удаление пользователя")
    void testDeleteById() {
        User user = userRepository.findByUsername("john_doe").orElseThrow();
        userRepository.deleteById(user.getId());
        Optional<User> found = userRepository.findById(user.getId());
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("16. Проверка существования email")
    void testExistsByEmail() {
        assertThat(userRepository.existsByEmail("jane@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("nonexistent@example.com")).isFalse();
    }

    @Test
    @DisplayName("17. Поиск с частичным совпадением")
    void testSearchWithPartialMatch() {
        List<User> foundByDo = userRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase("do", "do");
        List<User> foundByWi = userRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase("wi", "wi");

        assertThat(foundByDo).hasSize(1);
        assertThat(foundByWi).hasSize(1);
        assertThat(foundByDo.getFirst().getUsername()).isEqualTo("john_doe");
        assertThat(foundByWi.getFirst().getUsername()).isEqualTo("bob_wilson");
    }
}