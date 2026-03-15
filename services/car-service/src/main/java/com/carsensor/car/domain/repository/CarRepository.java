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
 * Репозиторий для работы с автомобилями
 * Поддерживает спецификации для сложных запросов
 */
@Repository
public interface CarRepository extends JpaRepository<Car, Long>, JpaSpecificationExecutor<Car> {

    // Поиск по externalId
    Optional<Car> findByExternalId(String externalId);

    // Проверка существования
    boolean existsByExternalId(String externalId);

    // Статистика и фильтры
    @Query("SELECT DISTINCT c.brand FROM Car c ORDER BY c.brand")
    List<String> findAllBrands();

    @Query("SELECT DISTINCT c.model FROM Car c WHERE c.brand = :brand ORDER BY c.model")
    List<String> findModelsByBrand(@Param("brand") String brand);

    @Query("SELECT MIN(c.year), MAX(c.year) FROM Car c")
    List<Object[]> findYearRange();

    @Query("SELECT MIN(c.price), MAX(c.price) FROM Car c")
    List<Object[]> findPriceRange();

    // Недавно спарсенные автомобили
    @Query("SELECT c FROM Car c WHERE c.parsedAt >= :since")
    List<Car> findRecentlyParsed(@Param("since") LocalDateTime since);

    // Количество по марке и модели
    @Query("SELECT COUNT(c) FROM Car c WHERE c.brand = :brand AND c.model = :model")
    long countByBrandAndModel(@Param("brand") String brand, @Param("model") String model);

    // Счетчик недавно спарсенных
    @Query("SELECT COUNT(c) FROM Car c WHERE c.parsedAt >= :since")
    long countRecentlyParsed(@Param("since") LocalDateTime since);
}