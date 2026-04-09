package com.carsensor.car.application.service.export.impl;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.carsensor.car.application.service.command.CarCommandService;
import com.carsensor.car.application.service.export.CarExportImportService;
import com.carsensor.car.domain.entity.Car;
import com.carsensor.car.domain.repository.CarRepository;
import com.carsensor.platform.dto.CarDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CarExportImportServiceImpl implements CarExportImportService {

    private final CarRepository carRepository;
    private final CarCommandService carCommandService;

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
    public void importCarsFromCsv(byte[] csvData) {
        log.debug("Importing cars from CSV");

        String csv = new String(csvData);
        String[] lines = csv.split("\n");

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            String[] fields = line.split(",");
            if (fields.length >= 6) {
                try {
                    CarDto carDto = new CarDto(
                            null, fields[1], fields[2],
                            Integer.parseInt(fields[3]), Integer.parseInt(fields[4]),
                            new BigDecimal(fields[5]),
                            null, null, null, null, null, null, null, null,
                            null, null, null, null, null, null, null, null
                    );
                    carCommandService.createCar(carDto);
                } catch (Exception e) {
                    log.error("Failed to import car from line: {}", line, e);
                }
            }
        }
        log.info("CSV import completed");
    }
}