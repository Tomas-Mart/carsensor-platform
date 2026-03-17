package com.carsensor.platform.util;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilsTest {

    @Test
    void isValidEmail_ValidEmail_ReturnsTrue() {
        assertTrue(ValidationUtils.isValidEmail("user@example.com"));
        assertTrue(ValidationUtils.isValidEmail("user.name+tag@example.co.uk"));
    }

    @Test
    void isValidEmail_InvalidEmail_ReturnsFalse() {
        assertFalse(ValidationUtils.isValidEmail("invalid-email"));
        assertFalse(ValidationUtils.isValidEmail("user@.com"));
        assertFalse(ValidationUtils.isValidEmail("@example.com"));
    }

    @Test
    void isValidEmail_NullOrBlank_ReturnsFalse() {
        assertFalse(ValidationUtils.isValidEmail(null));
        assertFalse(ValidationUtils.isValidEmail("   "));
    }

    @Test
    void isValidPhone_ValidPhone_ReturnsTrue() {
        assertTrue(ValidationUtils.isValidPhone("+79161234567"));
        assertTrue(ValidationUtils.isValidPhone("89161234567"));
        assertTrue(ValidationUtils.isValidPhone("+7 (916) 123-45-67"));
    }

    @Test
    void isValidPhone_InvalidPhone_ReturnsFalse() {
        assertFalse(ValidationUtils.isValidPhone("123"));
        assertFalse(ValidationUtils.isValidPhone("phone"));
    }

    @Test
    void isValidPhone_NullOrBlank_ReturnsFalse() {
        assertFalse(ValidationUtils.isValidPhone(null));
        assertFalse(ValidationUtils.isValidPhone("   "));
    }

    @Test
    void isPositivePrice_ValidPrice_ReturnsTrue() {
        assertTrue(ValidationUtils.isPositivePrice(BigDecimal.valueOf(100)));
        assertTrue(ValidationUtils.isPositivePrice(BigDecimal.valueOf(0.01)));
    }

    @Test
    void isPositivePrice_InvalidPrice_ReturnsFalse() {
        assertFalse(ValidationUtils.isPositivePrice(BigDecimal.ZERO));
        assertFalse(ValidationUtils.isPositivePrice(BigDecimal.valueOf(-10)));
        assertFalse(ValidationUtils.isPositivePrice(null));
    }

    @Test
    void isValidYear_ValidYear_ReturnsTrue() {
        int currentYear = java.time.Year.now().getValue();
        assertTrue(ValidationUtils.isValidYear(2020));
        assertTrue(ValidationUtils.isValidYear(2000));
        assertTrue(ValidationUtils.isValidYear(currentYear));
    }

    @Test
    void isValidYear_InvalidYear_ReturnsFalse() {
        int currentYear = java.time.Year.now().getValue();
        assertFalse(ValidationUtils.isValidYear(1899));
        assertFalse(ValidationUtils.isValidYear(currentYear + 2));
        assertFalse(ValidationUtils.isValidYear(null));
    }
}