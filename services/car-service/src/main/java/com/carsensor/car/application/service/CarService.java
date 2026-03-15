package com.carsensor.car.application.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import com.carsensor.platform.dto.CarDto;
import com.carsensor.platform.dto.PageResponse;

/**
 * Интерфейс сервиса для работы с автомобилями
 */
public interface CarService {

    CarDto getCarById(Long id);

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

    CarDto createCar(CarDto carDto);

    CarDto updateCar(Long id, CarDto carDto);

    void deleteCar(Long id);

    List<CarDto> saveAllCars(List<CarDto> carDtos);

    Map<String, Object> getFilterOptions();

    // Новые методы
    List<CarDto> getRecentlyParsedCars(int limit);

    List<CarDto> getCarsByBrand(String brand);

    List<CarDto> getCarsByYearRange(int fromYear, int toYear);

    List<CarDto> getCarsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice);

    long getTotalCarsCount();

    Map<String, Long> getStatisticsByBrand();

    List<CarDto> findSimilarCars(Long carId, int limit);

    byte[] exportCarsToCsv(List<Long> carIds);

    void importCarsFromCsv(byte[] csvData);

    /**
     * Статистика по автомобилям
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