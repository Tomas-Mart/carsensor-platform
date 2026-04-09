package com.carsensor.car.application.service.filter.impl;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.carsensor.car.application.service.filter.CarFilterOptionsService;
import com.carsensor.car.domain.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CarFilterOptionsServiceImpl implements CarFilterOptionsService {

    private final CarRepository carRepository;

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
}