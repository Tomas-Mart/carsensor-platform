package com.carsensor.car.application.service.statistics.impl;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.carsensor.car.application.service.statistics.CarStatisticsService;
import com.carsensor.car.domain.entity.Car;
import com.carsensor.car.domain.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CarStatisticsServiceImpl implements CarStatisticsService {

    private final CarRepository carRepository;

    @Override
    public long getTotalCarsCount() {
        log.debug("Fetching total cars count");
        return carRepository.count();
    }

    @Override
    public Map<String, Long> getStatisticsByBrand() {
        log.debug("Fetching statistics by brand");
        return carRepository.findAll().stream()
                .collect(Collectors.groupingBy(Car::getBrand, Collectors.counting()));
    }

    @Override
    public CarStatistics getCarStatistics() {
        log.debug("Fetching car statistics");

        long totalCars = carRepository.count();
        long carsWithPhotos = carRepository.countCarsWithPhotos();
        long carsWithoutPhotos = totalCars - carsWithPhotos;

        Double avgPrice = carRepository.getAveragePrice();
        double averagePrice = avgPrice != null ? avgPrice : 0.0;

        Double avgMileage = carRepository.getAverageMileage();
        double averageMileage = avgMileage != null ? avgMileage : 0.0;

        Object[] yearRange = carRepository.findYearRange().stream().findFirst().orElse(new Object[]{null, null});
        int oldestYear = yearRange[0] != null ? ((Number) yearRange[0]).intValue() : 0;
        int newestYear = yearRange[1] != null ? ((Number) yearRange[1]).intValue() : 0;

        LocalDateTime lastParsedAt = carRepository.findMaxParsedAt().orElse(null);

        return new CarStatistics(
                totalCars, carsWithPhotos, carsWithoutPhotos,
                averagePrice, averageMileage, oldestYear, newestYear, lastParsedAt
        );
    }
}