package com.carsensor.car.interfaces.rest;

import com.carsensor.platform.dto.CarDto;
import com.carsensor.platform.dto.PageResponse;
import com.carsensor.car.application.service.CarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cars")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cars", description = "API для работы с автомобилями")
public class CarController {

    private final CarService carService;

    @GetMapping
    @Operation(summary = "Получить список автомобилей",
            description = "Возвращает список автомобилей с фильтрацией, сортировкой и пагинацией")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение списка")
    })
    public ResponseEntity<PageResponse<CarDto>> getCars(
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo,
            @RequestParam(required = false) Integer mileageFrom,
            @RequestParam(required = false) Integer mileageTo,
            @RequestParam(required = false) BigDecimal priceFrom,
            @RequestParam(required = false) BigDecimal priceTo,
            @RequestParam(required = false) String transmission,
            @RequestParam(required = false) String driveType,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {

        log.debug("GET /api/v1/cars with page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size, parseSort(sort));

        PageResponse<CarDto> response = carService.getCars(
                brand, model, yearFrom, yearTo, mileageFrom, mileageTo,
                priceFrom, priceTo, transmission, driveType, search, pageable
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить автомобиль по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Автомобиль найден"),
            @ApiResponse(responseCode = "404", description = "Автомобиль не найден")
    })
    public ResponseEntity<CarDto> getCarById(
            @Parameter(description = "ID автомобиля") @PathVariable Long id) {
        log.debug("GET /api/v1/cars/{}", id);
        CarDto car = carService.getCarById(id);
        return ResponseEntity.ok(car);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CAR_CREATE')")
    @Operation(summary = "Создать новый автомобиль", description = "Доступно только администраторам")
    public ResponseEntity<CarDto> createCar(@Valid @RequestBody CarDto carDto) {
        log.info("POST /api/v1/cars - creating car: {} {}", carDto.brand(), carDto.model());
        CarDto created = carService.createCar(carDto);
        return ResponseEntity
                .created(URI.create("/api/v1/cars/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CAR_EDIT')")
    @Operation(summary = "Обновить автомобиль", description = "Доступно только администраторам")
    public ResponseEntity<CarDto> updateCar(
            @PathVariable Long id, @Valid @RequestBody CarDto carDto) {
        log.info("PUT /api/v1/cars/{} - updating car", id);
        CarDto updated = carService.updateCar(id, carDto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CAR_DELETE')")
    @Operation(summary = "Удалить автомобиль", description = "Доступно только администраторам")
    public ResponseEntity<Void> deleteCar(@PathVariable Long id) {
        log.info("DELETE /api/v1/cars/{} - deleting car", id);
        carService.deleteCar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/filters")
    @Operation(summary = "Получить доступные опции для фильтров")
    public ResponseEntity<Map<String, Object>> getFilterOptions() {
        return ResponseEntity.ok(carService.getFilterOptions());
    }

    private Sort parseSort(String[] sort) {
        if (sort.length == 0) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        String sortField = sort[0];
        String sortDirection = sort.length > 1 ? sort[1] : "asc";

        return Sort.by(Sort.Direction.fromString(sortDirection), sortField);
    }
}