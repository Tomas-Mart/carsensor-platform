package com.carsensor.scheduler.application.service.parse.impl;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import com.carsensor.platform.dto.CarDto;
import com.carsensor.scheduler.application.service.parse.ParseCoreService;
import com.carsensor.scheduler.domain.parser.CarSensorParser;
import com.carsensor.scheduler.infrastructure.client.CarServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParseCoreServiceImpl implements ParseCoreService {

    private final CarSensorParser carSensorParser;
    private final CarServiceClient carServiceClient;

    @Override
    public List<CarDto> parsePages(int maxPages) {
        log.info("Parsing with max pages: {}", maxPages);

        List<CarDto> parsedCars = carSensorParser.parseCars(maxPages);

        if (parsedCars.isEmpty()) {
            log.warn("No cars found during parsing");
            return new ArrayList<>();
        }

        List<CarDto> savedCars = carServiceClient.saveCars(parsedCars);
        log.info("Successfully parsed and saved {} cars", savedCars.size());

        return savedCars;
    }

    @Override
    public CarDto parseSingleCar(String url) {
        log.info("Parsing single car from URL: {}", url);
        return carSensorParser.parseSingleCar(url);
    }
}