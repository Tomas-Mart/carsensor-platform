package com.carsensor.auth.unit.dto;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.carsensor.auth.application.dto.UserDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Юнит-тесты для UserDto.
 *
 * <p><b>Проверяемые аспекты:</b>
 * <ul>
 *   <li>Сериализация/десериализация JSON</li>
 *   <li>Компактный конструктор (нормализация)</li>
 *   <li>Фабричные методы</li>
 * </ul>
 *
 * @see UserDto
 * @since 1.0
 */
@DisplayName("Тесты UserDto")
class UserDtoTest {

    private static final Long ID = 1L;
    private static final String USERNAME = "admin";
    private static final String EMAIL = "admin@example.com";
    private static final String PASSWORD = "admin123";
    private static final String FIRST_NAME = "Admin";
    private static final String LAST_NAME = "User";
    private static final boolean IS_ACTIVE = true;
    private static final boolean IS_LOCKED = false;
    private static final List<String> ROLES = List.of("ROLE_ADMIN", "ROLE_USER");
    private static final Long VERSION = 0L;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    // ============================================================
    // ТЕСТЫ СЕРИАЛИЗАЦИИ
    // ============================================================

    @Nested
    @DisplayName("Сериализация и десериализация JSON")
    class SerializationTests {

        @Test
        @DisplayName("✅ Сериализация UserDto в JSON и обратно")
        void should_SerializeAndDeserialize_Correctly() throws Exception {
            // given
            var original = new UserDto(
                    ID, USERNAME, EMAIL, PASSWORD, FIRST_NAME, LAST_NAME,
                    IS_ACTIVE, IS_LOCKED, ROLES, null, null, VERSION
            );

            // when
            var json = objectMapper.writeValueAsString(original);
            var deserialized = objectMapper.readValue(json, UserDto.class);

            // then
            assertThat(deserialized)
                    .isNotNull()
                    .satisfies(r -> {
                        assertThat(r.id()).isEqualTo(ID);
                        assertThat(r.username()).isEqualTo(USERNAME);
                        assertThat(r.email()).isEqualTo(EMAIL);
                        assertThat(r.firstName()).isEqualTo(FIRST_NAME);
                        assertThat(r.lastName()).isEqualTo(LAST_NAME);
                        assertThat(r.isActive()).isEqualTo(IS_ACTIVE);
                        assertThat(r.isLocked()).isEqualTo(IS_LOCKED);
                        assertThat(r.roles()).containsExactlyElementsOf(ROLES);
                        assertThat(r.version()).isEqualTo(VERSION);
                    });
        }

        @Test
        @DisplayName("✅ Password не сериализуется в JSON (write-only)")
        void should_NotSerializePassword() throws Exception {
            // given
            var original = new UserDto(
                    ID, USERNAME, EMAIL, PASSWORD, FIRST_NAME, LAST_NAME,
                    IS_ACTIVE, IS_LOCKED, ROLES, null, null, VERSION
            );

            // when
            var json = objectMapper.writeValueAsString(original);

            // then
            assertThat(json).doesNotContain("password");
            assertThat(json).doesNotContain(PASSWORD);
        }

        @Test
        @DisplayName("✅ Десериализация JSON с snake_case полями")
        void should_Deserialize_WithSnakeCaseFields() throws Exception {
            // given
            var json = """
                    {
                        "id": 1,
                        "username": "admin",
                        "email": "admin@example.com",
                        "first_name": "Admin",
                        "last_name": "User",
                        "is_active": true,
                        "is_locked": false,
                        "roles": ["ROLE_ADMIN"],
                        "version": 0
                    }
                    """;

            // when
            var user = objectMapper.readValue(json, UserDto.class);

            // then
            assertThat(user)
                    .isNotNull()
                    .satisfies(u -> {
                        assertThat(u.id()).isEqualTo(1L);
                        assertThat(u.username()).isEqualTo("admin");
                        assertThat(u.email()).isEqualTo("admin@example.com");
                        assertThat(u.firstName()).isEqualTo("Admin");
                        assertThat(u.lastName()).isEqualTo("User");
                        assertThat(u.isActive()).isTrue();
                        assertThat(u.isLocked()).isFalse();
                        assertThat(u.roles()).containsExactly("ROLE_ADMIN");
                        assertThat(u.version()).isEqualTo(0L);
                    });
        }
    }

    // ============================================================
    // ТЕСТЫ КОМПАКТНОГО КОНСТРУКТОРА
    // ============================================================

    @Nested
    @DisplayName("Компактный конструктор (нормализация)")
    class CompactConstructorTests {

        @Test
        @DisplayName("✅ Username нормализуется (trim и toLowerCase)")
        void should_NormalizeUsername() {
            // when
            var user = new UserDto(
                    null, "  ADMIN  ", "admin@example.com", null, null, null,
                    false, false, null, null, null, null
            );

            // then
            assertThat(user.username()).isEqualTo("admin");
        }

        @Test
        @DisplayName("✅ Email нормализуется (trim и toLowerCase)")
        void should_NormalizeEmail() {
            // when
            var user = new UserDto(
                    null, "admin", "  ADMIN@EXAMPLE.COM  ", null, null, null,
                    false, false, null, null, null, null
            );

            // then
            assertThat(user.email()).isEqualTo("admin@example.com");
        }
    }

    // ============================================================
    // ТЕСТЫ СОЗДАНИЯ DTO
    // ============================================================

    @Nested
    @DisplayName("Создание UserDto")
    class CreationTests {

        @Test
        @DisplayName("✅ Создание UserDto с минимальными полями")
        void should_CreateUserDto_WithMinimalFields() {
            // when
            var user = new UserDto(
                    null, "user", "user@example.com", "password123", null, null,
                    false, false, null, null, null, null
            );

            // then
            assertThat(user)
                    .isNotNull()
                    .satisfies(u -> {
                        assertThat(u.username()).isEqualTo("user");
                        assertThat(u.email()).isEqualTo("user@example.com");
                        assertThat(u.password()).isEqualTo("password123");
                        assertThat(u.id()).isNull();
                        assertThat(u.firstName()).isNull();
                        assertThat(u.lastName()).isNull();
                        assertThat(u.roles()).isNull();
                        assertThat(u.isActive()).isFalse();
                    });
        }

        @Test
        @DisplayName("✅ Создание UserDto с полными данными")
        void should_CreateUserDto_WithAllFields() {
            // when
            var user = new UserDto(
                    ID, USERNAME, EMAIL, PASSWORD, FIRST_NAME, LAST_NAME,
                    IS_ACTIVE, IS_LOCKED, ROLES, null, null, VERSION
            );

            // then
            assertThat(user)
                    .isNotNull()
                    .satisfies(u -> {
                        assertThat(u.id()).isEqualTo(ID);
                        assertThat(u.username()).isEqualTo(USERNAME);
                        assertThat(u.email()).isEqualTo(EMAIL);
                        assertThat(u.password()).isEqualTo(PASSWORD);
                        assertThat(u.firstName()).isEqualTo(FIRST_NAME);
                        assertThat(u.lastName()).isEqualTo(LAST_NAME);
                        assertThat(u.isActive()).isTrue();
                        assertThat(u.isLocked()).isFalse();
                        assertThat(u.roles()).containsExactlyElementsOf(ROLES);
                        assertThat(u.version()).isEqualTo(VERSION);
                    });
        }
    }
}