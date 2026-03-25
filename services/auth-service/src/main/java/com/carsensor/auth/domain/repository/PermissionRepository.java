package com.carsensor.auth.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.carsensor.auth.domain.entity.Permission;
import jakarta.persistence.QueryHint;

/**
 * Репозиторий для работы с разрешениями.
 *
 * <p>Предоставляет методы для доступа к данным разрешений,
 * включая поиск по имени, категориям, статистику использования.
 *
 * <p><b>Разделение ответственности:</b>
 * <ul>
 *   <li>CRUD операции - наследуются от {@link JpaRepository}</li>
 *   <li>Поиск по имени - базовые методы</li>
 *   <li>Групповые операции - поиск по множеству имен</li>
 *   <li>Статистика - агрегационные запросы</li>
 *   <li>Категоризация - группировка по префиксам</li>
 * </ul>
 *
 * @see Permission
 * @see JpaRepository
 * @since 1.0
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    // ============================================================
    // Базовые методы поиска (оптимизированные)
    // ============================================================

    /**
     * Находит разрешение по имени.
     *
     * <p><b>Оптимизация:</b> Использует индекс idx_permissions_name.
     *
     * @param name имя разрешения (не может быть null)
     * @return Optional с найденным разрешением или пустой Optional
     */
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "10"))
    Optional<Permission> findByName(String name);

    /**
     * Проверяет существование разрешения по имени.
     *
     * <p><b>Оптимизация:</b> Использует EXISTS вместо COUNT.
     *
     * @param name имя разрешения
     * @return true если разрешение существует, false в противном случае
     */
    @Query("""
            SELECT EXISTS(
                SELECT 1 FROM Permission p WHERE p.name = :name
            )
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "5"))
    boolean existsByName(@Param("name") String name);

    // ============================================================
    // Поиск по множеству имен (групповые операции)
    // ============================================================

    /**
     * Находит разрешения по набору имен.
     *
     * <p><b>Использование:</b> Для загрузки нескольких разрешений одним запросом.
     *
     * @param names набор имен разрешений
     * @return Set найденных разрешений
     */
    @Query("""
            SELECT p
            FROM Permission p
            WHERE p.name IN :names
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "15"))
    Set<Permission> findByNameIn(@Param("names") Set<String> names);

    // ============================================================
    // Получение всех разрешений с сортировкой
    // ============================================================

    /**
     * Получает все разрешения, отсортированные по имени.
     *
     * @return список всех разрешений, отсортированный по имени
     */
    @Query("""
            SELECT p
            FROM Permission p
            ORDER BY p.name
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "20"))
    List<Permission> findAllOrderedByName();

    /**
     * Получает имена разрешений по их идентификаторам.
     *
     * <p><b>Оптимизация:</b> Загружает только имена, не загружая полные сущности.
     *
     * @param ids набор идентификаторов разрешений
     * @return список имен разрешений
     */
    @Query("""
            SELECT p.name
            FROM Permission p
            WHERE p.id IN :ids
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "10"))
    List<String> findNamesByIds(@Param("ids") Set<Long> ids);

    // ============================================================
    // Статистика использования разрешений
    // ============================================================

    /**
     * Подсчитывает количество ролей, имеющих указанное разрешение.
     *
     * @param permissionId идентификатор разрешения
     * @return количество ролей с данным разрешением
     */
    @Query("""
            SELECT COUNT(r)
            FROM Role r
            JOIN r.permissions p
            WHERE p.id = :permissionId
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "10"))
    long countRolesByPermissionId(@Param("permissionId") Long permissionId);

    // ============================================================
    // Поиск по категориям
    // ============================================================

    /**
     * Находит разрешения по категории (префиксу имени).
     *
     * <p><b>Категории:</b>
     * <ul>
     *   <li>CAR_* - разрешения для автомобилей</li>
     *   <li>USER_* - разрешения для пользователей</li>
     *   <li>ROLE_* - разрешения для ролей</li>
     * </ul>
     *
     * @param category категория (например, "CAR_", "USER_", "ROLE_")
     * @return список разрешений, имя которых начинается с указанной категории
     */
    @Query("""
            SELECT p
            FROM Permission p
            WHERE p.name LIKE CONCAT(:category, '%')
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "15"))
    List<Permission> findByCategory(@Param("category") String category);

    /**
     * Находит разрешения, которые не используются ни в одной роли.
     *
     * <p><b>Использование:</b> Для очистки неиспользуемых разрешений.
     *
     * @return список неиспользуемых разрешений
     */
    @Query("""
            SELECT p
            FROM Permission p
            WHERE NOT EXISTS (
                SELECT 1 FROM Role r
                JOIN r.permissions rp
                WHERE rp.id = p.id
            )
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "20"))
    List<Permission> findUnusedPermissions();

    // ============================================================
    // Статистика по категориям
    // ============================================================

    /**
     * Получает статистику распределения разрешений по категориям.
     *
     * <p><b>Результат:</b> массив [category, count]
     *
     * @return список массивов [category, count]
     */
    @Query("""
            SELECT
                CASE
                    WHEN p.name LIKE 'CAR_%' THEN 'CAR'
                    WHEN p.name LIKE 'USER_%' THEN 'USER'
                    WHEN p.name LIKE 'ROLE_%' THEN 'ROLE'
                    ELSE 'OTHER'
                END AS category,
                COUNT(p) AS count
            FROM Permission p
            GROUP BY category
            ORDER BY 1
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "15"))
    List<Object[]> getPermissionStatistics();

    // ============================================================
    // Поиск по части имени (автодополнение)
    // ============================================================

    /**
     * Ищет разрешения по части имени (без учета регистра).
     *
     * <p><b>Использование:</b> Для автодополнения в UI.
     *
     * @param searchTerm поисковый запрос
     * @return список разрешений, имя которых содержит указанную строку
     */
    @Query("""
            SELECT p
            FROM Permission p
            WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            ORDER BY p.name
            LIMIT 50
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "10"))
    List<Permission> searchByName(@Param("searchTerm") String searchTerm);

    /**
     * Проверяет существование всех указанных разрешений одновременно.
     *
     * <p><b>Оптимизация:</b> Использует COUNT с сравнением.
     *
     * @param names набор имен разрешений
     * @return true если все разрешения существуют, false в противном случае
     */
    @Query("""
            SELECT COUNT(p) = :size
            FROM Permission p
            WHERE p.name IN :names
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "10"))
    boolean existsAllByName(@Param("names") Set<String> names, @Param("size") long size);
}