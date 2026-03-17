package com.carsensor.platform.util;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateUtilsTest {

    @Test
    void parseIso_ValidDate_ReturnsLocalDateTime() {
        // Given
        String dateStr = "2026-03-17T10:30:00";

        // When
        LocalDateTime result = DateUtils.parseIso(dateStr);

        // Then
        assertNotNull(result);
        assertEquals(2026, result.getYear());
        assertEquals(3, result.getMonthValue());
        assertEquals(17, result.getDayOfMonth());
        assertEquals(10, result.getHour());
        assertEquals(30, result.getMinute());
    }

    @Test
    void parseIso_InvalidDate_ReturnsNull() {
        // Given
        String dateStr = "invalid-date";

        // When
        LocalDateTime result = DateUtils.parseIso(dateStr);

        // Then
        assertNull(result);
    }

    @Test
    void parseIso_NullDate_ReturnsNull() {
        assertNull(DateUtils.parseIso(null));
    }

    @Test
    void parseIso_BlankDate_ReturnsNull() {
        assertNull(DateUtils.parseIso("   "));
    }

    @Test
    void formatRu_ValidDate_ReturnsFormattedString() {
        // Given
        LocalDateTime dateTime = LocalDateTime.of(2026, 3, 17, 10, 30);

        // When
        String result = DateUtils.formatRu(dateTime);

        // Then
        assertEquals("17.03.2026 10:30", result);
    }

    @Test
    void formatRu_NullDate_ReturnsEmptyString() {
        assertEquals("", DateUtils.formatRu(null));
    }

    @Test
    void isNotOlderThan_DateWithinDays_ReturnsTrue() {
        // Given
        LocalDateTime dateTime = LocalDateTime.now().minusDays(2);

        // When
        boolean result = DateUtils.isNotOlderThan(dateTime, 5);

        // Then
        assertTrue(result);
    }

    @Test
    void isNotOlderThan_DateOlderThanDays_ReturnsFalse() {
        // Given
        LocalDateTime dateTime = LocalDateTime.now().minusDays(10);

        // When
        boolean result = DateUtils.isNotOlderThan(dateTime, 5);

        // Then
        assertFalse(result);
    }

    @Test
    void isNotOlderThan_NullDate_ReturnsFalse() {
        assertFalse(DateUtils.isNotOlderThan(null, 5));
    }
}