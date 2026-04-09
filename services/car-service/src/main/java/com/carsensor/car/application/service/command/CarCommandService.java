package com.carsensor.car.application.service.command;

import java.util.List;
import com.carsensor.platform.dto.CarDto;

/**
 * Сервис для команд (CRUD операций) над автомобилями (CQRS - Command part).
 */
public interface CarCommandService {

    /**
     * Создание нового автомобиля.
     */
    CarDto createCar(CarDto carDto);

    /**
     * Полное обновление автомобиля.
     */
    CarDto updateCar(Long id, CarDto carDto);

    /**
     * Удаление автомобиля.
     */
    void deleteCar(Long id);

    /**
     * Пакетное сохранение автомобилей.
     */
    List<CarDto> saveAllCars(List<CarDto> carDtos);
}