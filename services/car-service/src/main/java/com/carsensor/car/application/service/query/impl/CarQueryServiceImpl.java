package com.carsensor.car.application.service.query.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.carsensor.car.application.mapper.CarMapper;
import com.carsensor.car.application.service.query.CarQueryService;
import com.carsensor.car.domain.entity.Car;
import com.carsensor.car.domain.repository.CarRepository;
import com.carsensor.car.domain.specification.CarSpecification;
import com.carsensor.platform.dto.CarDto;
import com.carsensor.platform.dto.PageResponse;
import com.carsensor.platform.dto.factory.PageResponseFactory;
import com.carsensor.platform.exception.PlatformException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CarQueryServiceImpl implements CarQueryService {

    private final CarRepository carRepository;
    private final CarMapper carMapper;

    @Override
    @Cacheable(value = "cars", key = "#id")
    public CarDto getCarById(Long id) {
        log.debug("Fetching car by id: {}", id);
        return carRepository.findById(id)
                .map(carMapper::toDto)
                .orElseThrow(() -> new PlatformException.CarNotFoundException(id));
    }

    @Override
    public CarDto getCarByBrandAndModel(String brand, String model) {
        log.debug("Fetching car by brand: {} and model: {}", brand, model);
        return carRepository.findByBrandAndModel(brand, model)
                .map(carMapper::toDto)
                .orElseThrow(() -> new PlatformException.CarNotFoundException(brand, model));
    }

    @Override
    public PageResponse<CarDto> getCars(
            String brand, String model, Integer yearFrom, Integer yearTo,
            Integer mileageFrom, Integer mileageTo, BigDecimal priceFrom, BigDecimal priceTo,
            String transmission, String driveType, String searchQuery, Pageable pageable
    ) {

        log.debug("Fetching cars with filters - brand: {}, model: {}", brand, model);

        Specification<Car> spec = CarSpecification.withFilters(
                brand, model, yearFrom, yearTo, mileageFrom, mileageTo,
                priceFrom, priceTo, transmission, driveType, searchQuery
        );

        Page<Car> carPage = carRepository.findAll(spec, pageable);
        Page<CarDto> carDtoPage = carPage.map(carMapper::toDto);
        return PageResponseFactory.fromPage(carDtoPage);
    }

    @Override
    public List<CarDto> getRecentlyParsedCars(int limit) {
        log.debug("Fetching {} recently parsed cars", limit);
        return carRepository.findRecentlyParsed(LocalDateTime.now().minusDays(7))
                .stream().limit(limit).map(carMapper::toDto).toList();
    }

    @Override
    public List<CarDto> getCarsByBrand(String brand) {
        log.debug("Fetching cars by brand: {}", brand);
        Specification<Car> spec = CarSpecification.withFilters(brand, null, null, null,
                null, null, null, null, null, null, null);
        return carRepository.findAll(spec).stream().map(carMapper::toDto).toList();
    }

    @Override
    public List<CarDto> getCarsByYearRange(int fromYear, int toYear) {
        log.debug("Fetching cars by year range: {} - {}", fromYear, toYear);
        return carRepository.findAll().stream()
                .filter(car -> car.getYear() >= fromYear && car.getYear() <= toYear)
                .map(carMapper::toDto).toList();
    }

    @Override
    public List<CarDto> getCarsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        log.debug("Fetching cars by price range: {} - {}", minPrice, maxPrice);
        return carRepository.findAll().stream()
                .filter(car -> car.getPrice().compareTo(minPrice) >= 0 &&
                               car.getPrice().compareTo(maxPrice) <= 0)
                .map(carMapper::toDto).toList();
    }

    @Override
    public List<CarDto> findSimilarCars(Long carId, int limit) {
        log.debug("Finding similar cars for car id: {}, limit: {}", carId, limit);
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new PlatformException.CarNotFoundException(carId));
        Specification<Car> spec = CarSpecification.withFilters(
                car.getBrand(), null, car.getYear() - 2, car.getYear() + 2,
                car.getMileage() - 10000, car.getMileage() + 10000,
                car.getPrice().subtract(new BigDecimal("500000")),
                car.getPrice().add(new BigDecimal("500000")),
                car.getTransmission(), car.getDriveType(), null
        );
        return carRepository.findAll(spec).stream()
                .filter(c -> !c.getId().equals(carId)).limit(limit)
                .map(carMapper::toDto).toList();
    }
}