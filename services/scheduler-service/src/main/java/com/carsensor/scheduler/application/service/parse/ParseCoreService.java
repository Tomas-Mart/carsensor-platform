package com.carsensor.scheduler.application.service.parse;

import java.util.List;
import com.carsensor.platform.dto.CarDto;

/**
 * Ядро парсинга (без состояния)
 */
public interface ParseCoreService {

    List<CarDto> parsePages(int maxPages);

    CarDto parseSingleCar(String url);
}