package com.carsensor.scheduler.unit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/**
 * Утилитный класс для тестов парсера
 */
public final class ParserTestUtils {

    private ParserTestUtils() {
    }

    /**
     * Создание тестового HTML элемента с заданным текстом
     */
    public static Element createHtmlElement(String text) {
        return Jsoup.parse("<div>" + text + "</div>").select("div").first();
    }

    /**
     * Создание тестового HTML документа
     */
    public static org.jsoup.nodes.Document createHtmlDocument(String html) {
        return Jsoup.parse(html);
    }

    /**
     * Создание тестовой карточки автомобиля
     */
    public static Element createCarCard(String brand, String model, String year, String mileage, String price) {
        String html = String.format("""
                <div class="cassetteItem">
                    <h2>%s %s</h2>
                    <div class="year">%s</div>
                    <div class="mileage">%s</div>
                    <div class="price">%s</div>
                </div>
                """, brand, model, year, mileage, price);
        return Jsoup.parse(html).select("div.cassetteItem").first();
    }
}