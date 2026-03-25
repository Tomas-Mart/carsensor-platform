package com.carsensor.scheduler.infrastructure.client;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.carsensor.platform.dto.CarDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Клиент для взаимодействия с car-service
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CarServiceClient {

    private final RestTemplate restTemplate;

    @Value("${car-service.url:http://localhost:8082}")
    private String carServiceUrl;

    public List<CarDto> saveCars(List<CarDto> cars) {
        log.debug("Sending {} cars to car-service", cars.size());

        String url = UriComponentsBuilder.fromHttpUrl(carServiceUrl)
                .path("/api/v1/cars/batch")
                .build()
                .toUriString();

        try {
            HttpEntity<List<CarDto>> request = new HttpEntity<>(cars);

            ResponseEntity<List<CarDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<List<CarDto>>() {
                    }
            );

            log.debug("Successfully saved {} cars", response.getBody().size());
            return response.getBody();

        } catch (Exception e) {
            log.error("Error saving cars to car-service: {}", e.getMessage());
            throw e;
        }
    }

    public CarDto getCarById(Long id) {
        String url = UriComponentsBuilder.fromHttpUrl(carServiceUrl)
                .path("/api/v1/cars/{id}")
                .buildAndExpand(id)
                .toUriString();

        return restTemplate.getForObject(url, CarDto.class);
    }
}