package com.carsensor.car.infrastructure.security;

/**
 * Константы прав доступа для security аннотаций.
 *
 * <p>Используются в {@link org.springframework.security.access.prepost.PreAuthorize}
 * для обеспечения type-safe доступа к правам.
 *
 * @author CarSensor Platform Team
 * @since 1.0
 */
public interface SecurityPermissions {

    /**
     * Право на просмотр автомобилей
     */
    String VIEW_CARS = "VIEW_CARS";

    /**
     * Право на создание автомобилей
     */
    String CREATE_CARS = "CREATE_CARS";

    /**
     * Право на редактирование автомобилей
     */
    String EDIT_CARS = "EDIT_CARS";

    /**
     * Право на удаление автомобилей
     */
    String DELETE_CARS = "DELETE_CARS";
}