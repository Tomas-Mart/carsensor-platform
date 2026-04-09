package com.carsensor.car.application.service.command.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.carsensor.car.application.mapper.CarMapper;
import com.carsensor.car.application.service.command.CarCommandService;
import com.carsensor.car.domain.entity.Car;
import com.carsensor.car.domain.repository.CarRepository;
import com.carsensor.platform.dto.CarDto;
import com.carsensor.platform.exception.PlatformException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CarCommandServiceImpl implements CarCommandService {

    private final CarRepository carRepository;
    private final CarMapper carMapper;

    @Override
    @CacheEvict(value = "cars", allEntries = true)
    public CarDto createCar(CarDto carDto) {
        log.info("Creating new car: {} {}", carDto.brand(), carDto.model());

        if (carDto.brand() == null || carDto.brand().isBlank()) {
            throw new PlatformException.ValidationException("Марка автомобиля обязательна для заполнения");
        }
        if (carDto.model() == null || carDto.model().isBlank()) {
            throw new PlatformException.ValidationException("Модель автомобиля обязательна для заполнения");
        }
        if (carDto.year() == null || carDto.year() < 1900 || carDto.year() > 2026) {
            throw new PlatformException.ValidationException("Год выпуска должен быть от 1900 до текущего года");
        }
        if (carDto.price() == null || carDto.price().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PlatformException.ValidationException("Цена должна быть положительным числом");
        }
        if (carDto.externalId() != null && carRepository.existsByExternalId(carDto.externalId())) {
            throw new PlatformException.DuplicateResourceException("Car", "externalId: " + carDto.externalId());
        }

        Car car = carMapper.toEntity(carDto);
        car.setParsedAt(LocalDateTime.now());
        Car savedCar = carRepository.save(car);
        log.info("Car created successfully with id: {}", savedCar.getId());
        return carMapper.toDto(savedCar);
    }

    @Override
    @CacheEvict(value = "cars", key = "#id")
    public CarDto updateCar(Long id, CarDto carDto) {
        if (carDto == null) {
            throw new IllegalArgumentException("CarDto must not be null");
        }
        log.info("Updating car with id: {}", id);

        Car car = carRepository.findById(id)
                .orElseThrow(() -> new PlatformException.CarNotFoundException(id));

        if (carDto.version() != null && !car.getVersion().equals(carDto.version())) {
            throw new PlatformException.OptimisticLockException(
                    "Car", car.getId(), carDto.version(), car.getVersion());
        }

        carMapper.updateEntity(carDto, car);
        Car updatedCar = carRepository.save(car);
        log.info("Car updated successfully: {}", id);
        return carMapper.toDto(updatedCar);
    }

    @Override
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
}