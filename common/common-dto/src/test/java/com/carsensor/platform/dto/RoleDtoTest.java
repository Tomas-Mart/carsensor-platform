package com.carsensor.platform.dto;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Юнит-тесты для RoleDto.
 *
 * <p><b>Проверяемые аспекты:</b>
 * <ul>
 *   <li>Сериализация/десериализация JSON</li>
 *   <li>Компактный конструктор (нормализация)</li>
 *   <li>Фабричные методы {@code of()}</li>
 * </ul>
 *
 * @see RoleDto
 * @since 1.0
 */
@DisplayName("Тесты RoleDto")
class RoleDtoTest {

    private static final Long ID = 1L;
    private static final String NAME = "ROLE_ADMIN";
    private static final String DESCRIPTION = "Администратор с полными правами";
    private static final List<String> PERMISSIONS = List.of("CAR_VIEW", "CAR_EDIT", "USER_MANAGE");

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ============================================================
    // ТЕСТЫ СЕРИАЛИЗАЦИИ
    // ============================================================

    @Nested
    @DisplayName("Сериализация и десериализация JSON")
    class SerializationTests {

        @Test
        @DisplayName("✅ Сериализация RoleDto в JSON и обратно")
        void should_SerializeAndDeserialize_Correctly() throws Exception {
            // given
            var original = new RoleDto(ID, NAME, DESCRIPTION, PERMISSIONS, null, null, null);

            // when
            var json = objectMapper.writeValueAsString(original);
            var deserialized = objectMapper.readValue(json, RoleDto.class);

            // then
            assertThat(deserialized)
                    .isNotNull()
                    .satisfies(r -> {
                        assertThat(r.id()).isEqualTo(ID);
                        assertThat(r.name()).isEqualTo(NAME);
                        assertThat(r.description()).isEqualTo(DESCRIPTION);
                        assertThat(r.permissions()).containsExactlyElementsOf(PERMISSIONS);
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
        @DisplayName("✅ Name нормализуется (trim и toUpperCase)")
        void should_NormalizeName() {
            // when
            var role = new RoleDto(null, "  admin  ", null, null, null, null, null);

            // then
            assertThat(role.name()).isEqualTo("ROLE_ADMIN");
        }

        @Test
        @DisplayName("✅ Name без префикса ROLE_ добавляет его")
        void should_AddRolePrefix_WhenNameDoesNotStartWithRole() {
            // when
            var role = new RoleDto(null, "admin", null, null, null, null, null);

            // then
            assertThat(role.name()).isEqualTo("ROLE_ADMIN");
        }

        @Test
        @DisplayName("✅ Name с префиксом ROLE_ оставляет как есть")
        void should_KeepName_WhenNameAlreadyHasRolePrefix() {
            // when
            var role = new RoleDto(null, "ROLE_ADMIN", null, null, null, null, null);

            // then
            assertThat(role.name()).isEqualTo("ROLE_ADMIN");
        }

        @Test
        @DisplayName("✅ Description нормализуется (trim)")
        void should_NormalizeDescription() {
            // when
            var role = new RoleDto(null, "ROLE_ADMIN", "  Admin role  ", null, null, null, null);

            // then
            assertThat(role.description()).isEqualTo("Admin role");
        }

        @Test
        @DisplayName("✅ Null permissions заменяется на пустой список")
        void should_SetEmptyList_WhenPermissionsIsNull() {
            // when
            var role = new RoleDto(null, "ROLE_ADMIN", "Admin role", null, null, null, null);

            // then
            assertThat(role.permissions()).isNotNull().isEmpty();
        }
    }

    // ============================================================
    // ТЕСТЫ ФАБРИЧНЫХ МЕТОДОВ
    // ============================================================

    @Nested
    @DisplayName("Фабричные методы of()")
    class FactoryMethodTests {

        @Test
        @DisplayName("✅ of(name, description) создает роль без разрешений")
        void should_CreateRole_WithoutPermissions() {
            // when
            var role = RoleDto.of("ROLE_USER", "Обычный пользователь");

            // then
            assertThat(role.id()).isNull();
            assertThat(role.name()).isEqualTo("ROLE_USER");
            assertThat(role.description()).isEqualTo("Обычный пользователь");
            assertThat(role.permissions()).isEmpty();
        }

        @Test
        @DisplayName("✅ of(name, description, permissions) создает роль с разрешениями")
        void should_CreateRole_WithPermissions() {
            // when
            var role = RoleDto.of("ROLE_ADMIN", "Администратор", PERMISSIONS);

            // then
            assertThat(role.id()).isNull();
            assertThat(role.name()).isEqualTo("ROLE_ADMIN");
            assertThat(role.description()).isEqualTo("Администратор");
            assertThat(role.permissions()).containsExactlyElementsOf(PERMISSIONS);
        }
    }
}