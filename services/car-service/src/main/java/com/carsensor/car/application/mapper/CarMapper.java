package com.carsensor.car.application.mapper;

import java.util.List;
import org.springframework.stereotype.Component;
import com.carsensor.car.domain.entity.Car;
import com.carsensor.platform.dto.CarDto;

/**
 * Маппер для Car <-> CarDto
 * Без MapStruct, использует явное преобразование
 */
@Component
public class CarMapper {

    /**
     * Преобразует Car в CarDto
     */
    public CarDto toDto(Car car) {
        if (car == null) {
            return null;
        }

        return new CarDto(
                car.getId(),
                car.getBrand(),
                car.getModel(),
                car.getYear(),
                car.getMileage(),
                car.getPrice(),
                car.getDescription(),
                car.getOriginalBrand(),
                car.getOriginalModel(),
                car.getExteriorColor(),
                car.getInteriorColor(),
                car.getEngineCapacity(),
                car.getTransmission(),
                car.getDriveType(),
                car.getPhotoUrls(),
                car.getMainPhotoUrl(),
                car.getExternalId(),
                car.getSourceUrl(),
                car.getParsedAt(),
                car.getCreatedAt(),
                car.getUpdatedAt(),
                car.getVersion()
        );
    }

    /**
     * Преобразует CarDto в Car
     */
    public Car toEntity(CarDto dto) {
        if (dto == null) {
            return null;
        }

        return Car.builder()
                .id(dto.id())
                .brand(dto.brand())
                .model(dto.model())
                .year(dto.year())
                .mileage(dto.mileage())
                .price(dto.price())
                .description(dto.description())
                .originalBrand(dto.originalBrand())
                .originalModel(dto.originalModel())
                .exteriorColor(dto.exteriorColor())
                .interiorColor(dto.interiorColor())
                .engineCapacity(dto.engineCapacity())
                .transmission(dto.transmission())
                .driveType(dto.driveType())
                .photoUrls(dto.photoUrls() != null ? dto.photoUrls() : List.of())
                .mainPhotoUrl(dto.mainPhotoUrl())
                .externalId(dto.externalId())
                .sourceUrl(dto.sourceUrl())
                .parsedAt(dto.parsedAt())
                .version(dto.version())
                .build();
    }

    /**
     * Обновляет существующий Car из CarDto
     */
    public void updateEntity(CarDto dto, Car car) {
        if (dto == null || car == null) {
            return;
        }

        if (dto.brand() != null) car.setBrand(dto.brand());
        if (dto.model() != null) car.setModel(dto.model());
        if (dto.year() != null) car.setYear(dto.year());
        if (dto.mileage() != null) car.setMileage(dto.mileage());
        if (dto.price() != null) car.setPrice(dto.price());
        if (dto.description() != null) car.setDescription(dto.description());
        if (dto.originalBrand() != null) car.setOriginalBrand(dto.originalBrand());
        if (dto.originalModel() != null) car.setOriginalModel(dto.originalModel());
        if (dto.exteriorColor() != null) car.setExteriorColor(dto.exteriorColor());
        if (dto.interiorColor() != null) car.setInteriorColor(dto.interiorColor());
        if (dto.engineCapacity() != null) car.setEngineCapacity(dto.engineCapacity());
        if (dto.transmission() != null) car.setTransmission(dto.transmission());
        if (dto.driveType() != null) car.setDriveType(dto.driveType());
        if (dto.photoUrls() != null) car.setPhotoUrls(dto.photoUrls());
        if (dto.mainPhotoUrl() != null) car.setMainPhotoUrl(dto.mainPhotoUrl());
        if (dto.externalId() != null) car.setExternalId(dto.externalId());
        if (dto.sourceUrl() != null) car.setSourceUrl(dto.sourceUrl());
        if (dto.parsedAt() != null) car.setParsedAt(dto.parsedAt());
        if (dto.version() != null) car.setVersion(dto.version());
    }

    /**
     * Преобразует список Car в список CarDto
     */
    public List<CarDto> toDtoList(List<Car> cars) {
        if (cars == null) {
            return List.of();
        }
        return cars.stream()
                .map(this::toDto)
                .toList();
    }
}