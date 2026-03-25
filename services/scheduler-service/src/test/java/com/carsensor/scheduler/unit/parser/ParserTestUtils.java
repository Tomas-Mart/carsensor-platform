package com.carsensor.scheduler.unit.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/**
 * Утилитный класс для тестов парсера.
 * Содержит вспомогательные методы для создания тестовых HTML элементов.
 * Не предназначен для использования в production коде.
 */
public final class ParserTestUtils {

    private ParserTestUtils() {
        // Приватный конструктор для предотвращения создания экземпляров
    }

    /**
     * Создание тестового HTML элемента с заданным текстом
     *
     * @param text текст для элемента
     * @return Element с указанным текстом
     */
    public static Element createHtmlElement(String text) {
        return Jsoup.parse("<div>" + text + "</div>").select("div").first();
    }

    /**
     * Создание тестового HTML документа
     *
     * @param html HTML строка
     * @return Document объект
     */
    public static org.jsoup.nodes.Document createHtmlDocument(String html) {
        return Jsoup.parse(html);
    }

    /**
     * Создание тестовой карточки автомобиля
     *
     * @param brand   марка
     * @param model   модель
     * @param year    год
     * @param mileage пробег
     * @param price   цена
     * @return Element карточки автомобиля
     */
    public static Element createCarCard(String brand, String model, String year, String mileage, String price) {
        String html = String.format("""
                <div class="cassetteItem">
                    <h2>%s %s</h2>
                    <div class="year">%s км</div>
                    <div class="mileage">%s км</div>
                    <div class="price">%s 円</div>
                </div>
                """, brand, model, year, mileage, price);
        return Jsoup.parse(html).select("div.cassetteItem").first();
    }
}