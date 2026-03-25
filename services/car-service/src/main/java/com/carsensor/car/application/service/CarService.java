package com.carsensor.car.application.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import com.carsensor.platform.dto.CarDto;
import com.carsensor.platform.dto.PageResponse;
import com.carsensor.platform.exception.PlatformException;

/**
 * Интерфейс сервиса для работы с автомобилями
 *
 * <p>Предоставляет методы для CRUD операций, поиска, фильтрации
 * и экспорта/импорта данных об автомобилях.
 */
public interface CarService {

    /**
     * Получение автомобиля по ID.
     *
     * @param id идентификатор автомобиля
     * @return DTO автомобиля
     * @throws PlatformException.CarNotFoundException если автомобиль не найден
     */
    CarDto getCarById(Long id);

    /**
     * Получение автомобиля по марке и модели.
     *
     * @param brand марка автомобиля
     * @param model модель автомобиля
     * @return DTO автомобиля
     * @throws PlatformException.CarNotFoundException если автомобиль не найден
     */
    CarDto getCarByBrandAndModel(String brand, String model);

    /**
     * Получение списка автомобилей с фильтрацией и пагинацией.
     *
     * @param brand        марка (опционально)
     * @param model        модель (опционально)
     * @param yearFrom     год от (опционально)
     * @param yearTo       год до (опционально)
     * @param mileageFrom  пробег от (опционально)
     * @param mileageTo    пробег до (опционально)
     * @param priceFrom    цена от (опционально)
     * @param priceTo      цена до (опционально)
     * @param transmission тип трансмиссии (опционально)
     * @param driveType    тип привода (опционально)
     * @param searchQuery  поисковый запрос (опционально)
     * @param pageable     параметры пагинации
     * @return страница с DTO автомобилей
     */
    PageResponse<CarDto> getCars(
            String brand,
            String model,
            Integer yearFrom,
            Integer yearTo,
            Integer mileageFrom,
            Integer mileageTo,
            BigDecimal priceFrom,
            BigDecimal priceTo,
            String transmission,
            String driveType,
            String searchQuery,
            Pageable pageable
    );

    /**
     * Создание нового автомобиля.
     *
     * @param carDto DTO автомобиля
     * @return созданный DTO автомобиля
     * @throws PlatformException.DuplicateResourceException если автомобиль с таким externalId уже существует
     */
    CarDto createCar(CarDto carDto);

    /**
     * Полное обновление автомобиля.
     *
     * @param id     идентификатор автомобиля
     * @param carDto DTO с новыми данными
     * @return обновленный DTO автомобиля
     * @throws PlatformException.CarNotFoundException если автомобиль не найден
     */
    CarDto updateCar(Long id, CarDto carDto);

    /**
     * Удаление автомобиля.
     *
     * @param id идентификатор автомобиля
     * @throws PlatformException.CarNotFoundException если автомобиль не найден
     */
    void deleteCar(Long id);

    /**
     * Пакетное сохранение автомобилей.
     *
     * @param carDtos список DTO автомобилей
     * @return список сохраненных DTO
     */
    List<CarDto> saveAllCars(List<CarDto> carDtos);

    /**
     * Получение доступных опций для фильтров.
     *
     * @return карта с опциями фильтрации
     */
    Map<String, Object> getFilterOptions();

    /**
     * Получение недавно распарсенных автомобилей.
     *
     * @param limit максимальное количество
     * @return список DTO автомобилей
     */
    List<CarDto> getRecentlyParsedCars(int limit);

    /**
     * Получение автомобилей по марке.
     *
     * @param brand марка автомобиля
     * @return список DTO автомобилей
     */
    List<CarDto> getCarsByBrand(String brand);

    /**
     * Получение автомобилей по диапазону годов выпуска.
     *
     * @param fromYear начальный год
     * @param toYear   конечный год
     * @return список DTO автомобилей
     */
    List<CarDto> getCarsByYearRange(int fromYear, int toYear);

    /**
     * Получение автомобилей по диапазону цен.
     *
     * @param minPrice минимальная цена
     * @param maxPrice максимальная цена
     * @return список DTO автомобилей
     */
    List<CarDto> getCarsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice);

    /**
     * Получение общего количества автомобилей.
     *
     * @return количество автомобилей
     */
    long getTotalCarsCount();

    /**
     * Получение статистики по маркам.
     *
     * @return карта "марка -> количество"
     */
    Map<String, Long> getStatisticsByBrand();

    /**
     * Поиск похожих автомобилей.
     *
     * @param carId ID автомобиля-образца
     * @param limit максимальное количество результатов
     * @return список похожих автомобилей
     */
    List<CarDto> findSimilarCars(Long carId, int limit);

    /**
     * Экспорт автомобилей в CSV.
     *
     * @param carIds список ID автомобилей для экспорта
     * @return массив байт CSV файла
     */
    byte[] exportCarsToCsv(List<Long> carIds);

    /**
     * Импорт автомобилей из CSV.
     *
     * @param csvData массив байт CSV файла
     */
    void importCarsFromCsv(byte[] csvData);

    /**
     * Статистика по автомобилям.
     *
     * @param totalCars         общее количество
     * @param carsWithPhotos    количество с фото
     * @param carsWithoutPhotos количество без фото
     * @param averagePrice      средняя цена
     * @param averageMileage    средний пробег
     * @param oldestYear        самый старый год
     * @param newestYear        самый новый год
     * @param lastParsedAt      время последнего парсинга
     */
    record CarStatistics(
            long totalCars,
            long carsWithPhotos,
            long carsWithoutPhotos,
            double averagePrice,
            double averageMileage,
            int oldestYear,
            int newestYear,
            LocalDateTime lastParsedAt
    ) {
    }
}