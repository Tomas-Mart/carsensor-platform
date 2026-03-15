package com.carsensor.car.application.mapper;

import com.carsensor.car.domain.entity.Car;
import com.carsensor.platform.dto.CarDto;
import org.mapstruct.*;

import java.util.List;

/**
 * Маппер для Car <-> CarDto с использованием MapStruct
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CarMapper {

    CarDto toDto(Car car);

    List<CarDto> toDtoList(List<Car> cars);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Car toEntity(CarDto carDto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromDto(CarDto carDto, @MappingTarget Car car);
}