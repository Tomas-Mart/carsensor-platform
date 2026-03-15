package com.carsensor.scheduler.domain.parser;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import com.carsensor.platform.dto.CarDto;
import com.carsensor.scheduler.domain.dictionary.JapaneseCarDictionary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Парсер для сайта CarSensor.net с использованием Jsoup
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CarSensorParser {

    private final JapaneseCarDictionary dictionary;

    private static final String BASE_URL = "https://www.carsensor.net";
    private static final String SEARCH_URL = BASE_URL + "/usedcar/search.php";
    private static final int TIMEOUT = 30000;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    // Флаг для остановки парсинга
    private volatile boolean stopRequested = false;

    /**
     * Парсинг списка автомобилей с CarSensor
     */
    public List<CarDto> parseCars(int maxPages) {
        stopRequested = false;
        log.info("Starting parsing CarSensor.net, max pages: {}", maxPages);

        List<CarDto> allCars = new ArrayList<>();
        String currentUrl = SEARCH_URL;
        int pageCount = 0;

        while (currentUrl != null && pageCount < maxPages && !stopRequested) {
            try {
                log.debug("Parsing page {}: {}", pageCount + 1, currentUrl);

                Document doc = Jsoup.connect(currentUrl)
                        .userAgent(USER_AGENT)
                        .timeout(TIMEOUT)
                        .followRedirects(true)
                        .get();

                // Парсинг автомобилей на странице
                List<CarDto> pageCars = parseCarListPage(doc);
                log.debug("Found {} cars on page {}", pageCars.size(), pageCount + 1);
                allCars.addAll(pageCars);

                // Получение URL следующей страницы
                currentUrl = getNextPageUrl(doc);
                pageCount++;

                // Небольшая задержка между запросами
                if (currentUrl != null && !stopRequested) {
                    Thread.sleep(2000);
                }

            } catch (IOException e) {
                log.error("Error parsing page {}: {}", currentUrl, e.getMessage());
                break;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Parsing interrupted");
                break;
            }
        }

        if (stopRequested) {
            log.info("Parsing stopped by user. Total cars found: {}", allCars.size());
        } else {
            log.info("Parsing completed. Total cars found: {}", allCars.size());
        }

        return allCars;
    }

    /**
     * Остановка парсинга
     */
    public void stopParsing() {
        log.info("Stop requested for parsing");
        stopRequested = true;
    }

    /**
     * Парсинг одной машины по URL
     */
    public CarDto parseSingleCar(String url) {
        log.info("Parsing single car from URL: {}", url);
        // TODO: реализовать парсинг детальной страницы
        return null;
    }

    /**
     * Парсинг страницы со списком автомобилей
     */
    private List<CarDto> parseCarListPage(Document doc) {
        List<CarDto> cars = new ArrayList<>();

        // Поиск карточек автомобилей
        Elements carCards = doc.select("div.cassetteItem, article.carCassette, div[class*=carCassette]");

        if (carCards.isEmpty()) {
            // Альтернативный селектор
            carCards = doc.select("div[class*=cassette], div[class*=carItem]");
        }

        for (Element card : carCards) {
            if (stopRequested) {
                break;
            }
            try {
                CarDto car = parseCarCard(card);
                if (car != null) {
                    cars.add(car);
                }
            } catch (Exception e) {
                log.error("Error parsing car card: {}", e.getMessage());
            }
        }

        return cars;
    }

    /**
     * Парсинг отдельной карточки автомобиля
     */
    private CarDto parseCarCard(Element card) {
        try {
            // Получение ссылки на детальную страницу
            String detailUrl = null;
            Element link = card.select("a[href*=/usedcar/detail/]").first();
            if (link != null) {
                detailUrl = BASE_URL + link.attr("href");
            }

            // Марка и модель
            String title = extractText(card, "h2, h3, .title, .modelName");
            String brand = null;
            String model = null;

            if (title != null) {
                String[] parts = title.split("\\s+", 2);
                if (parts.length > 0) {
                    brand = dictionary.normalizeBrand(parts[0]);
                }
                if (parts.length > 1) {
                    model = dictionary.normalizeModel(parts[1]);
                }
            }

            // Если не удалось извлечь из заголовка, пробуем другие селекторы
            if (brand == null) {
                String brandText = extractText(card, ".maker, .brand, [class*=maker]");
                if (brandText != null) {
                    brand = dictionary.normalizeBrand(brandText);
                }
            }

            if (model == null) {
                String modelText = extractText(card, ".model, [class*=model]");
                if (modelText != null) {
                    model = dictionary.normalizeModel(modelText);
                }
            }

            if (brand == null || model == null) {
                log.debug("Skipping car - missing brand/model: {}", title);
                return null;
            }

            // Год выпуска
            Integer year = extractYear(card);

            // Пробег
            Integer mileage = extractMileage(card);

            // Цена
            BigDecimal price = extractPrice(card);

            if (year == null || mileage == null || price == null) {
                log.debug("Skipping car - missing required data: year={}, mileage={}, price={}",
                        year, mileage, price);
                return null;
            }

            // Цвет
            String color = extractColor(card);

            // Трансмиссия
            String transmission = extractTransmission(card);

            // Привод
            String driveType = extractDriveType(card);

            // Объем двигателя
            String engineCapacity = extractEngineCapacity(card);

            // Фото
            List<String> photoUrls = extractPhotos(card);

            // Описание
            String description = extractDescription(card);

            return CarDto.builder()
                    .brand(brand)
                    .model(model)
                    .year(year)
                    .mileage(mileage)
                    .price(price)
                    .description(description)
                    .originalBrand(title != null ? title : null)
                    .exteriorColor(color)
                    .transmission(transmission)
                    .driveType(driveType)
                    .engineCapacity(engineCapacity)
                    .photoUrls(photoUrls)
                    .mainPhotoUrl(photoUrls != null && !photoUrls.isEmpty() ? photoUrls.get(0) : null)
                    .parsedAt(LocalDateTime.now())
                    .sourceUrl(detailUrl)
                    .build();

        } catch (Exception e) {
            log.error("Error in parseCarCard: {}", e.getMessage());
            return null;
        }
    }

    private String extractText(Element element, String selector) {
        Element selected = element.select(selector).first();
        if (selected != null) {
            return selected.text().trim();
        }
        return null;
    }

    private Integer extractYear(Element card) {
        // Поиск года в различных форматах
        Pattern yearPattern = Pattern.compile("(\\d{4})年|(\\d{2})年|(平成|令和)(\\d+)年");

        String text = card.text();
        Matcher matcher = yearPattern.matcher(text);

        if (matcher.find()) {
            if (matcher.group(1) != null) {
                return Integer.parseInt(matcher.group(1));
            } else if (matcher.group(2) != null) {
                // Двузначный год (например, 22 -> 2022)
                int year = Integer.parseInt(matcher.group(2));
                return 2000 + year;
            } else if (matcher.group(4) != null) {
                // Японская эра
                return convertJapaneseYear(matcher.group(3), Integer.parseInt(matcher.group(4)));
            }
        }

        // Поиск в отдельных элементах
        String yearText = extractText(card, ".year, [class*=year], .modelYear");
        if (yearText != null) {
            matcher = yearPattern.matcher(yearText);
            if (matcher.find() && matcher.group(1) != null) {
                return Integer.parseInt(matcher.group(1));
            }
        }

        return null;
    }

    private Integer convertJapaneseYear(String era, int year) {
        // Преобразование японских эр в западный год
        return switch (era) {
            case "令和" -> 2019 + year; // Reiwa (2019 + year)
            case "平成" -> 1989 + (year - 1); // Heisei (1989 + year - 1)
            case "昭和" -> 1926 + (year - 1); // Showa (1926 + year - 1)
            default -> null;
        };
    }

    private Integer extractMileage(Element card) {
        Pattern mileagePattern = Pattern.compile("(\\d+(?:,\\d{3})*)\\s*km");

        String text = card.text();
        Matcher matcher = mileagePattern.matcher(text);

        if (matcher.find()) {
            String mileageStr = matcher.group(1).replace(",", "");
            return Integer.parseInt(mileageStr);
        }

        // Поиск в отдельных элементах
        String mileageText = extractText(card, ".mileage, [class*=mileage]");
        if (mileageText != null) {
            matcher = mileagePattern.matcher(mileageText);
            if (matcher.find()) {
                String mileageStr = matcher.group(1).replace(",", "");
                return Integer.parseInt(mileageStr);
            }
        }

        return null;
    }

    private BigDecimal extractPrice(Element card) {
        Pattern pricePattern = Pattern.compile("(\\d+(?:,\\d{3})*)\\s*万円");

        String text = card.text();
        Matcher matcher = pricePattern.matcher(text);

        if (matcher.find()) {
            String priceStr = matcher.group(1).replace(",", "");
            // Переводим из 万円 (10,000 йен) в йены
            double manYen = Double.parseDouble(priceStr);
            double yen = manYen * 10000;
            // Конвертация в рубли по примерному курсу (1 JPY ≈ 0.6 RUB для демо)
            double rub = yen * 0.6;
            return BigDecimal.valueOf(rub);
        }

        // Поиск в отдельных элементах
        String priceText = extractText(card, ".price, [class*=price]");
        if (priceText != null) {
            matcher = pricePattern.matcher(priceText);
            if (matcher.find()) {
                String priceStr = matcher.group(1).replace(",", "");
                double manYen = Double.parseDouble(priceStr);
                double yen = manYen * 10000;
                double rub = yen * 0.6;
                return BigDecimal.valueOf(rub);
            }
        }

        return null;
    }

    private String extractColor(Element card) {
        String colorText = extractText(card, ".color, [class*=color]");
        if (colorText != null) {
            return dictionary.normalizeColor(colorText);
        }

        // Поиск по тексту
        for (String jpColor : dictionary.getColorMapping().keySet()) {
            if (card.text().contains(jpColor)) {
                return dictionary.getColorMapping().get(jpColor);
            }
        }

        return null;
    }

    private String extractTransmission(Element card) {
        String transmissionText = extractText(card, ".transmission, [class*=transmission]");
        if (transmissionText != null) {
            return dictionary.normalizeTransmission(transmissionText);
        }

        // Поиск по тексту
        for (Map.Entry<String, String> entry : dictionary.getTransmissionMapping().entrySet()) {
            if (card.text().contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    private String extractDriveType(Element card) {
        String driveText = extractText(card, ".drive, [class*=drive]");
        if (driveText != null) {
            return dictionary.normalizeDriveType(driveText);
        }

        // Поиск по тексту
        for (Map.Entry<String, String> entry : dictionary.getDriveTypeMapping().entrySet()) {
            if (card.text().contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    private String extractEngineCapacity(Element card) {
        Pattern enginePattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*[Ll]");

        String text = card.text();
        Matcher matcher = enginePattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(1) + "L";
        }

        String engineText = extractText(card, ".engine, [class*=engine]");
        if (engineText != null) {
            matcher = enginePattern.matcher(engineText);
            if (matcher.find()) {
                return matcher.group(1) + "L";
            }
        }

        return null;
    }

    private List<String> extractPhotos(Element card) {
        List<String> photos = new ArrayList<>();

        // Поиск изображений
        Elements images = card.select("img[src*=/usedcar/], img[src*=/images/]");

        for (Element img : images) {
            String src = img.attr("src");
            if (src.startsWith("//")) {
                src = "https:" + src;
            } else if (src.startsWith("/")) {
                src = BASE_URL + src;
            }

            // Фильтруем только достаточно большие изображения (не иконки)
            if (!src.contains("icon") && !src.contains("logo")) {
                photos.add(src);
            }
        }

        return photos;
    }

    private String extractDescription(Element card) {
        String desc = extractText(card, ".description, [class*=desc]");
        if (desc != null) {
            return desc;
        }

        return null;
    }

    private String getNextPageUrl(Document doc) {
        // Поиск ссылки на следующую страницу
        Element nextLink = doc.select("a.next, a[rel=next], li.next a").first();

        if (nextLink != null) {
            String href = nextLink.attr("href");
            if (href.startsWith("http")) {
                return href;
            } else {
                return BASE_URL + href;
            }
        }

        return null;
    }
}