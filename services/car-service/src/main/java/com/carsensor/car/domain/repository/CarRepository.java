package com.carsensor.car.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.carsensor.car.domain.entity.Car;

/**
 * Репозиторий для работы с автомобилями.
 *
 * <p>Предоставляет методы для доступа к данным автомобилей,
 * включая поиск, фильтрацию, статистику и агрегацию.
 *
 * <p>Поддерживает:
 * <ul>
 *   <li>Базовые CRUD операции через JpaRepository</li>
 *   <li>Сложные запросы через JpaSpecificationExecutor</li>
 *   <li>Пользовательские запросы с JPQL</li>
 * </ul>
 *
 * @see Car
 * @see JpaRepository
 * @see JpaSpecificationExecutor
 */
@Repository
public interface CarRepository extends JpaRepository<Car, Long>, JpaSpecificationExecutor<Car> {

    // ============================================================
    // Поиск по внешним идентификаторам
    // ============================================================

    /**
     * Находит автомобиль по внешнему идентификатору (ID из источника данных).
     *
     * @param externalId внешний идентификатор
     * @return Optional с найденным автомобилем или пустой Optional
     */
    Optional<Car> findByExternalId(String externalId);

    /**
     * Проверяет существование автомобиля с указанным внешним идентификатором.
     *
     * @param externalId внешний идентификатор
     * @return true если автомобиль существует, false в противном случае
     */
    boolean existsByExternalId(String externalId);

    /**
     * Находит автомобиль по марке и модели.
     *
     * @param brand марка автомобиля
     * @param model модель автомобиля
     * @return Optional с найденным автомобилем или пустой Optional
     */
    Optional<Car> findByBrandAndModel(String brand, String model);

    /**
     * Находит все автомобили указанной марки.
     *
     * @param brand марка автомобиля
     * @return список автомобилей указанной марки
     */
    List<Car> findByBrand(String brand);

    // ============================================================
    // Статистика и агрегация
    // ============================================================

    /**
     * Получает список всех уникальных марок автомобилей, отсортированный по алфавиту.
     *
     * @return список марок автомобилей
     */
    @Query("""
            SELECT DISTINCT c.brand
            FROM Car c
            ORDER BY c.brand
            """)
    List<String> findAllBrands();

    /**
     * Получает список всех уникальных моделей для указанной марки.
     *
     * @param brand марка автомобиля
     * @return список моделей автомобилей указанной марки
     */
    @Query("""
            SELECT DISTINCT c.model
            FROM Car c
            WHERE c.brand = :brand
            ORDER BY c.model
            """)
    List<String> findModelsByBrand(@Param("brand") String brand);

    /**
     * Получает минимальный и максимальный год выпуска автомобилей.
     *
     * @return массив из двух элементов [minYear, maxYear]
     */
    @Query("""
            SELECT MIN(c.year), MAX(c.year)
            FROM Car c
            """)
    List<Object[]> findYearRange();

    /**
     * Получает минимальную и максимальную цену автомобилей.
     *
     * @return массив из двух элементов [minPrice, maxPrice]
     */
    @Query("""
            SELECT MIN(c.price), MAX(c.price)
            FROM Car c
            """)
    List<Object[]> findPriceRange();

    /**
     * Подсчитывает количество автомобилей по марке и модели.
     *
     * @param brand марка автомобиля
     * @param model модель автомобиля
     * @return количество автомобилей
     */
    @Query("""
            SELECT COUNT(c)
            FROM Car c
            WHERE c.brand = :brand AND c.model = :model
            """)
    long countByBrandAndModel(@Param("brand") String brand, @Param("model") String model);

    // ============================================================
    // Поиск по дате парсинга
    // ============================================================

    /**
     * Находит автомобили, спарсенные после указанной даты.
     *
     * @param since дата, после которой были спарсены автомобили
     * @return список автомобилей, спарсенных после указанной даты
     */
    @Query("""
            SELECT c
            FROM Car c
            WHERE c.parsedAt >= :since
            ORDER BY c.parsedAt DESC
            """)
    List<Car> findRecentlyParsed(@Param("since") LocalDateTime since);

    /**
     * Подсчитывает количество автомобилей, спарсенных после указанной даты.
     *
     * @param since дата, после которой были спарсены автомобили
     * @return количество автомобилей
     */
    @Query("""
            SELECT COUNT(c)
            FROM Car c
            WHERE c.parsedAt >= :since
            """)
    long countRecentlyParsed(@Param("since") LocalDateTime since);

    // ============================================================
    // Поиск по диапазонам значений
    // ============================================================

    /**
     * Находит автомобили в указанном диапазоне цен.
     *
     * @param minPrice минимальная цена
     * @param maxPrice максимальная цена
     * @return список автомобилей в диапазоне цен
     */
    List<Car> findByPriceBetween(java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice);

    /**
     * Находит автомобили в указанном диапазоне пробега.
     *
     * @param minMileage минимальный пробег
     * @param maxMileage максимальный пробег
     * @return список автомобилей в диапазоне пробега
     */
    List<Car> findByMileageBetween(Integer minMileage, Integer maxMileage);

    /**
     * Находит автомобили в указанном диапазоне годов выпуска.
     *
     * @param minYear минимальный год выпуска
     * @param maxYear максимальный год выпуска
     * @return список автомобилей в диапазоне годов
     */
    List<Car> findByYearBetween(Integer minYear, Integer maxYear);
}