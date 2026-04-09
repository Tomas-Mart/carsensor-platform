package com.carsensor.car.application.service.filter;

import java.util.Map;

/**
 * Сервис для получения опций фильтрации автомобилей.
 */
public interface CarFilterOptionsService {

    /**
     * Получение доступных опций для фильтров.
     */
    Map<String, Object> getFilterOptions();
}