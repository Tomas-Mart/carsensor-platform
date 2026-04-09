package com.carsensor.platform.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Юнит-тесты для LoginRequest.
 *
 * <p><b>Проверяемые аспекты:</b>
 * <ul>
 *   <li>Сериализация/десериализация JSON</li>
 *   <li>Компактный конструктор (нормализация)</li>
 *   <li>Bean Validation</li>
 * </ul>
 *
 * @see LoginRequest
 * @since 1.0
 */
@DisplayName("Тесты LoginRequest")
class LoginRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
    private final Validator validator = validatorFactory.getValidator();

    // ============================================================
    // ТЕСТЫ СЕРИАЛИЗАЦИИ
    // ============================================================

    @Nested
    @DisplayName("Сериализация и десериализация JSON")
    class SerializationTests {

        @Test
        @DisplayName("✅ Сериализация LoginRequest в JSON и обратно")
        void should_SerializeAndDeserialize_Correctly() throws Exception {
            // given
            var original = new LoginRequest("admin", "admin123");

            // when
            var json = objectMapper.writeValueAsString(original);
            var deserialized = objectMapper.readValue(json, LoginRequest.class);

            // then
            assertThat(deserialized)
                    .isNotNull()
                    .satisfies(r -> {
                        assertThat(r.username()).isEqualTo("admin");
                        assertThat(r.password()).isEqualTo("admin123");
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
            var request = new LoginRequest("  ADMIN  ", "admin123");

            // then
            assertThat(request.username()).isEqualTo("admin");
        }

        @Test
        @DisplayName("✅ Password нормализуется (trim)")
        void should_NormalizePassword() {
            // when
            var request = new LoginRequest("admin", "  admin123  ");

            // then
            assertThat(request.password()).isEqualTo("admin123");
        }

        @Test
        @DisplayName("✅ Null username и password не вызывают исключение")
        void should_NotThrowException_WhenUsernameIsNull() {
            // when & then
            var request = new LoginRequest(null, "admin123");

            assertThat(request.username()).isNull();
            assertThat(request.password()).isEqualTo("admin123");
        }
    }

    // ============================================================
    // ТЕСТЫ ВАЛИДАЦИИ
    // ============================================================

    @Nested
    @DisplayName("Bean Validation")
    class ValidationTests {

        @Test
        @DisplayName("✅ Валидный LoginRequest проходит проверку")
        void should_PassValidation_WhenAllFieldsAreValid() {
            // given
            var request = new LoginRequest("admin", "admin123");

            // when
            var violations = validator.validate(request);

            // then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("❌ Пустой username вызывает ошибку валидации")
        void should_FailValidation_WhenUsernameIsEmpty() {
            // given
            var request = new LoginRequest("", "admin123");

            // when
            var violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
        }

        @Test
        @DisplayName("❌ Username короче 3 символов вызывает ошибку валидации")
        void should_FailValidation_WhenUsernameIsTooShort() {
            // given
            var request = new LoginRequest("ab", "admin123");

            // when
            var violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
        }

        @Test
        @DisplayName("❌ Пустой password вызывает ошибку валидации")
        void should_FailValidation_WhenPasswordIsEmpty() {
            // given
            var request = new LoginRequest("admin", "");

            // when
            var violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        }

        @Test
        @DisplayName("❌ Password короче 6 символов вызывает ошибку валидации")
        void should_FailValidation_WhenPasswordIsTooShort() {
            // given
            var request = new LoginRequest("admin", "12345");

            // when
            var violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        }
    }
}