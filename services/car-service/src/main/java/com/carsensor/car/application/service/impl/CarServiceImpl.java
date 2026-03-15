package com.carsensor.car.application.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.carsensor.car.application.mapper.CarMapper;
import com.carsensor.car.application.service.CarService;
import com.carsensor.car.domain.entity.Car;
import com.carsensor.car.domain.repository.CarRepository;
import com.carsensor.car.domain.specification.CarSpecification;
import com.carsensor.platform.dto.CarDto;
import com.carsensor.platform.dto.PageResponse;
import com.carsensor.platform.exception.PlatformException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Реализация сервиса для работы с автомобилями
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CarServiceImpl implements CarService {

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
    public PageResponse<CarDto> getCars(
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
            Pageable pageable) {

        log.debug("Fetching cars with filters - brand: {}, model: {}, yearFrom: {}, yearTo: {}",
                brand, model, yearFrom, yearTo);

        Specification<Car> spec = CarSpecification.withFilters(
                brand, model, yearFrom, yearTo, mileageFrom, mileageTo,
                priceFrom, priceTo, transmission, driveType, searchQuery
        );

        Page<Car> carPage = carRepository.findAll(spec, pageable);
        Page<CarDto> carDtoPage = carPage.map(carMapper::toDto);

        return PageResponse.fromPage(carDtoPage);
    }

    @Override
    @Transactional
    @CacheEvict(value = "cars", allEntries = true)
    public CarDto createCar(CarDto carDto) {
        log.info("Creating new car: {} {}", carDto.brand(), carDto.model());

        if (carDto.externalId() != null &&
                carRepository.existsByExternalId(carDto.externalId())) {
            throw new PlatformException.DuplicateResourceException(
                    "Car", "externalId: " + carDto.externalId());
        }

        Car car = carMapper.toEntity(carDto);
        car.setParsedAt(LocalDateTime.now());

        Car savedCar = carRepository.save(car);
        log.info("Car created successfully with id: {}", savedCar.getId());

        return carMapper.toDto(savedCar);
    }

    @Override
    @Transactional
    @CacheEvict(value = "cars", key = "#id")
    public CarDto updateCar(Long id, CarDto carDto) {
        if (carDto == null) {
            throw new IllegalArgumentException("CarDto must not be null");
        }
        log.info("Updating car with id: {}", id);

        Car car = carRepository.findById(id)
                .orElseThrow(() -> new PlatformException.CarNotFoundException(id));

        carMapper.updateEntityFromDto(carDto, car);

        Car updatedCar = carRepository.save(car);
        log.info("Car updated successfully: {}", id);

        return carMapper.toDto(updatedCar);
    }

    @Override
    @Transactional
    @CacheEvict(value = "cars", key = "#id")
    public void deleteCar(Long id) {
        log.info("Deleting car with id: {}", id);

        if (!carRepository.existsById(id)) {
            throw new PlatformException.CarNotFoundException(id);
        }

        carRepository.deleteById(id);
        log.info("Car deleted successfully: {}", id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "cars", allEntries = true)
    public List<CarDto> saveAllCars(List<CarDto> carDtos) {
        log.info("Saving {} cars", carDtos.size());

        if (carDtos.isEmpty()) {
            return List.of();
        }

        List<Car> cars = carDtos.stream()
                .map(dto -> {
                    Car car = carMapper.toEntity(dto);
                    car.setParsedAt(LocalDateTime.now());
                    return car;
                })
                .toList();

        List<Car> savedCars = carRepository.saveAll(cars);
        log.info("Successfully saved {} cars", savedCars.size());

        return carMapper.toDtoList(savedCars);
    }

    @Override
    public Map<String, Object> getFilterOptions() {
        log.debug("Fetching filter options");

        List<String> brands = carRepository.findAllBrands();
        List<Object[]> yearRangeList = carRepository.findYearRange();
        List<Object[]> priceRangeList = carRepository.findPriceRange();

        Object[] yearRange = yearRangeList.isEmpty() ? new Object[]{null, null} : yearRangeList.getFirst();
        Object[] priceRange = priceRangeList.isEmpty() ? new Object[]{null, null} : priceRangeList.getFirst();

        return Map.of(
                "brands", brands,
                "yearMin", yearRange[0],
                "yearMax", yearRange[1],
                "priceMin", priceRange[0],
                "priceMax", priceRange[1],
                "transmissions", List.of("AT", "MT", "CVT"),
                "driveTypes", List.of("2WD", "4WD", "AWD")
        );
    }

    @Override
    public List<CarDto> getRecentlyParsedCars(int limit) {
        log.debug("Fetching {} recently parsed cars", limit);

        LocalDateTime since = LocalDateTime.now().minusDays(7);
        return carRepository.findRecentlyParsed(since)
                .stream()
                .limit(limit)
                .map(carMapper::toDto)
                .toList();
    }

    @Override
    public List<CarDto> getCarsByBrand(String brand) {
        log.debug("Fetching cars by brand: {}", brand);

        Specification<Car> spec = CarSpecification.withFilters(brand, null, null, null,
                null, null, null, null, null, null, null);

        return carRepository.findAll(spec)
                .stream()
                .map(carMapper::toDto)
                .toList();
    }

    @Override
    public List<CarDto> getCarsByYearRange(int fromYear, int toYear) {
        log.debug("Fetching cars by year range: {} - {}", fromYear, toYear);

        return carRepository.findAll().stream()
                .filter(car -> car.getYear() >= fromYear && car.getYear() <= toYear)
                .map(carMapper::toDto)
                .toList();
    }

    @Override
    public List<CarDto> getCarsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        log.debug("Fetching cars by price range: {} - {}", minPrice, maxPrice);

        return carRepository.findAll().stream()
                .filter(car -> car.getPrice().compareTo(minPrice) >= 0 &&
                        car.getPrice().compareTo(maxPrice) <= 0)
                .map(carMapper::toDto)
                .toList();
    }

    @Override
    public long getTotalCarsCount() {
        log.debug("Fetching total cars count");
        return carRepository.count();
    }

    @Override
    public Map<String, Long> getStatisticsByBrand() {
        log.debug("Fetching statistics by brand");

        return carRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        Car::getBrand,
                        Collectors.counting()
                ));
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
                .filter(c -> !c.getId().equals(carId))
                .limit(limit)
                .map(carMapper::toDto)
                .toList();
    }

    @Override
    public byte[] exportCarsToCsv(List<Long> carIds) {
        log.debug("Exporting {} cars to CSV", carIds.size());

        List<Car> cars = carRepository.findAllById(carIds);
        StringBuilder csv = new StringBuilder("ID,Brand,Model,Year,Mileage,Price\n");

        for (Car car : cars) {
            csv.append(car.getId()).append(",")
                    .append(car.getBrand()).append(",")
                    .append(car.getModel()).append(",")
                    .append(car.getYear()).append(",")
                    .append(car.getMileage()).append(",")
                    .append(car.getPrice()).append("\n");
        }

        return csv.toString().getBytes();
    }

    @Override
    @Transactional
    public void importCarsFromCsv(byte[] csvData) {
        log.debug("Importing cars from CSV");

        String csv = new String(csvData);
        String[] lines = csv.split("\n");

        // Пропускаем заголовок
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            String[] fields = line.split(",");
            if (fields.length >= 6) {
                try {
                    CarDto carDto = CarDto.builder()
                            .brand(fields[1])
                            .model(fields[2])
                            .year(Integer.parseInt(fields[3]))
                            .mileage(Integer.parseInt(fields[4]))
                            .price(new BigDecimal(fields[5]))
                            .build();

                    createCar(carDto);
                } catch (Exception e) {
                    log.error("Failed to import car from line: {}", line, e);
                }
            }
        }

        log.info("CSV import completed");
    }
}