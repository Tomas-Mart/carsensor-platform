package com.carsensor.car.application.service.query;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Pageable;
import com.carsensor.platform.dto.CarDto;
import com.carsensor.platform.dto.PageResponse;

/**
 * Сервис для чтения и поиска автомобилей (CQRS - Query part).
 */
public interface CarQueryService {

    /**
     * Получение автомобиля по ID.
     */
    CarDto getCarById(Long id);

    /**
     * Получение автомобиля по марке и модели.
     */
    CarDto getCarByBrandAndModel(String brand, String model);

    /**
     * Получение списка автомобилей с фильтрацией и пагинацией.
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
     * Получение недавно распарсенных автомобилей.
     */
    List<CarDto> getRecentlyParsedCars(int limit);

    /**
     * Получение автомобилей по марке.
     */
    List<CarDto> getCarsByBrand(String brand);

    /**
     * Получение автомобилей по диапазону годов выпуска.
     */
    List<CarDto> getCarsByYearRange(int fromYear, int toYear);

    /**
     * Получение автомобилей по диапазону цен.
     */
    List<CarDto> getCarsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice);

    /**
     * Поиск похожих автомобилей.
     */
    List<CarDto> findSimilarCars(Long carId, int limit);
}