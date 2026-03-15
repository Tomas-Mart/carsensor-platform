package com.carsensor.auth.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.carsensor.auth.domain.entity.Permission;

/**
 * Репозиторий для работы с разрешениями
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    // Базовые методы поиска
    Optional<Permission> findByName(String name);

    boolean existsByName(String name);

    // Поиск по множеству имен
    @Query("SELECT p FROM Permission p WHERE p.name IN :names")
    Set<Permission> findByNameIn(@Param("names") Set<String> names);

    // Получение всех разрешений с сортировкой
    @Query("SELECT p FROM Permission p ORDER BY p.name")
    List<Permission> findAllOrderedByName();

    // Получение только имен по ID
    @Query("SELECT p.name FROM Permission p WHERE p.id IN :ids")
    List<String> findNamesByIds(@Param("ids") Set<Long> ids);

    // Статистика использования разрешений
    @Query("SELECT COUNT(r) FROM Role r JOIN r.permissions p WHERE p.id = :permissionId")
    long countRolesByPermissionId(@Param("permissionId") Long permissionId);

    // Поиск разрешений по категориям (префикс имени) - ИСПРАВЛЕНО
    @Query("SELECT p FROM Permission p WHERE p.name LIKE concat(:category, '%')")
    List<Permission> findByCategory(@Param("category") String category);

    // Поиск разрешений, которые не используются ни в одной роли
    @Query("SELECT p FROM Permission p WHERE p NOT IN (SELECT DISTINCT p2 FROM Role r JOIN r.permissions p2)")
    List<Permission> findUnusedPermissions();

    // Получение статистики по категориям
    @Query("SELECT " +
            "CASE " +
            "  WHEN p.name LIKE 'CAR_%' THEN 'CAR' " +
            "  WHEN p.name LIKE 'USER_%' THEN 'USER' " +
            "  WHEN p.name LIKE 'ROLE_%' THEN 'ROLE' " +
            "  ELSE 'OTHER' " +
            "END as category, " +
            "COUNT(p) as count " +
            "FROM Permission p " +
            "GROUP BY category")
    List<Object[]> getPermissionStatistics();

    // Поиск по части имени (для автодополнения)
    @Query("SELECT p FROM Permission p WHERE lower(p.name) LIKE lower(concat('%', :searchTerm, '%'))")
    List<Permission> searchByName(@Param("searchTerm") String searchTerm);

    // Проверка существования нескольких имен одновременно
    @Query("SELECT COUNT(p) > 0 FROM Permission p WHERE p.name IN :names")
    boolean existsAllByName(@Param("names") Set<String> names);
}