package com.carsensor.common.test.util;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Утилитный класс для тестирования
 * Содержит методы для работы с приватными методами/полями и вспомогательные функции
 */
public final class TestUtils {

    private TestUtils() {
        // Приватный конструктор для утилитного класса
    }

    /**
     * Безопасный вызов приватного метода
     *
     * @param target     объект, на котором вызывается метод
     * @param methodName имя метода
     * @param args       аргументы метода
     * @param <T>        тип возвращаемого значения
     * @return результат выполнения метода
     */
    @SuppressWarnings("unchecked")
    public static <T> T invokePrivateMethod(Object target, String methodName, Object... args) {
        return (T) ReflectionTestUtils.invokeMethod(target, methodName, args);
    }

    /**
     * Установка значения приватного поля
     *
     * @param target    объект, на котором устанавливается поле
     * @param fieldName имя поля
     * @param value     значение для установки
     */
    public static void setPrivateField(Object target, String fieldName, Object value) {
        ReflectionTestUtils.setField(target, fieldName, value);
    }

    /**
     * Получение значения приватного поля
     *
     * @param target    объект, с которого получается поле
     * @param fieldName имя поля
     * @param <T>       тип возвращаемого значения
     * @return значение поля
     */
    @SuppressWarnings("unchecked")
    public static <T> T getPrivateField(Object target, String fieldName) {
        return (T) ReflectionTestUtils.getField(target, fieldName);
    }

    /**
     * Выполнение действия с игнорированием исключений
     * Полезно для тестов, где исключения не критичны
     *
     * @param action действие для выполнения
     */
    public static void safeExecute(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            // Логируем и игнорируем
            System.err.println("Ignored exception in safeExecute: " + e.getMessage());
        }
    }

    /**
     * Создание тестового HTML элемента с заданным текстом
     *
     * @param text текст внутри элемента
     * @return элемент div с заданным текстом
     */
    public static org.jsoup.nodes.Element createHtmlElement(String text) {
        return org.jsoup.Jsoup.parse("<div>" + text + "</div>").select("div").first();
    }

    /**
     * Форматирование даты для тестов
     *
     * @param date дата для форматирования
     * @return строка в формате ISO_LOCAL_DATE_TIME
     */
    public static String formatDate(LocalDateTime date) {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    /**
     * Создание BigDecimal из строки с обработкой исключений
     *
     * @param value строковое представление числа
     * @return объект BigDecimal или null при ошибке
     */
    public static BigDecimal createBigDecimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Проверка, что два BigDecimal числа равны с учетом точности
     *
     * @param expected ожидаемое значение
     * @param actual   фактическое значение
     * @return true если числа равны
     */
    public static boolean bigDecimalsEqual(BigDecimal expected, BigDecimal actual) {
        if (expected == null && actual == null) return true;
        if (expected == null || actual == null) return false;
        return expected.compareTo(actual) == 0;
    }

    /**
     * Создание строки с повторяющимся символом
     *
     * @param ch    символ
     * @param count количество повторений
     * @return строка из повторяющихся символов
     */
    public static String repeatChar(char ch, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }

    /**
     * Ожидание с игнорированием InterruptedException
     * Полезно для тестов, где нужно подождать асинхронные операции
     *
     * @param millis время ожидания в миллисекундах
     */
    public static void sleepUninterruptibly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Ожидание с игнорированием InterruptedException
     *
     * @param timeout время ожидания
     * @param unit    единица измерения времени
     */
    public static void sleepUninterruptibly(long timeout, TimeUnit unit) {
        sleepUninterruptibly(unit.toMillis(timeout));
    }

    /**
     * Проверка, что строка не null и не пуста
     *
     * @param str проверяемая строка
     * @return true если строка не null и не пуста
     */
    public static boolean isNotBlank(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * Проверка, что строка null или пуста
     *
     * @param str проверяемая строка
     * @return true если строка null или пуста
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}