package com.carsensor.auth.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.carsensor.auth.domain.entity.Role;
import jakarta.persistence.QueryHint;

/**
 * Репозиторий для работы с ролями.
 *
 * <p>Предоставляет методы для доступа к данным ролей,
 * включая поиск по имени, загрузку разрешений и статистику использования.
 *
 * <p><b>Разделение ответственности:</b>
 * <ul>
 *   <li>CRUD операции - наследуются от {@link JpaRepository}</li>
 *   <li>Поиск по имени - базовые методы</li>
 *   <li>Загрузка с разрешениями - JOIN FETCH методы</li>
 *   <li>Статистика - агрегационные запросы</li>
 *   <li>Поиск сирот - роли без пользователей</li>
 * </ul>
 *
 * @see Role
 * @see JpaRepository
 * @since 1.0
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    // ============================================================
    // Базовые методы (оптимизированные)
    // ============================================================

    /**
     * Находит роль по имени.
     *
     * <p><b>Оптимизация:</b> Использует индекс idx_roles_name.
     *
     * @param name имя роли (не может быть null)
     * @return Optional с найденной ролью или пустой Optional
     */
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "10"))
    Optional<Role> findByName(String name);

    /**
     * Проверяет существование роли по имени.
     *
     * <p><b>Оптимизация:</b> Использует EXISTS вместо COUNT.
     *
     * @param name имя роли
     * @return true если роль существует, false в противном случае
     */
    @Query("""
            SELECT EXISTS(
                SELECT 1 FROM Role r WHERE r.name = :name
            )
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "5"))
    boolean existsByName(@Param("name") String name);

    // ============================================================
    // Методы с JOIN FETCH для производительности
    // ============================================================

    /**
     * Находит роль по имени с загрузкой разрешений.
     *
     * <p><b>Оптимизация:</b> Использует JOIN FETCH для загрузки разрешений
     * одним запросом, предотвращая N+1 проблему.
     *
     * @param name имя роли
     * @return Optional с найденной ролью и ее разрешениями
     */
    @Query("""
            SELECT r
            FROM Role r
            LEFT JOIN FETCH r.permissions
            WHERE r.name = :name
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "15"))
    Optional<Role> findByNameWithPermissions(@Param("name") String name);

    /**
     * Находит роли по именам с загрузкой разрешений.
     *
     * <p><b>Оптимизация:</b> Загружает несколько ролей с разрешениями одним запросом.
     *
     * @param names набор имен ролей
     * @return Set ролей с их разрешениями
     */
    @Query("""
            SELECT r
            FROM Role r
            LEFT JOIN FETCH r.permissions
            WHERE r.name IN :names
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "20"))
    Set<Role> findByNameInWithPermissions(@Param("names") Set<String> names);

    /**
     * Находит все роли с загрузкой разрешений.
     *
     * <p><b>Использование:</b> Для кэширования всех ролей с разрешениями.
     *
     * @return список всех ролей с их разрешениями
     */
    @Query("""
            SELECT r
            FROM Role r
            LEFT JOIN FETCH r.permissions
            ORDER BY r.name
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "30"))
    List<Role> findAllWithPermissions();

    // ============================================================
    // Статистические методы
    // ============================================================

    /**
     * Подсчитывает количество пользователей, имеющих указанную роль.
     *
     * @param roleId идентификатор роли
     * @return количество пользователей с данной ролью
     */
    @Query("""
            SELECT COUNT(u)
            FROM User u
            JOIN u.roles r
            WHERE r.id = :roleId
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "10"))
    long countUsersByRoleId(@Param("roleId") Long roleId);

    /**
     * Получает статистику распределения пользователей по ролям.
     *
     * <p><b>Результат:</b> массив [roleName, userCount]
     *
     * @return список массивов [roleName, userCount]
     */
    @Query("""
            SELECT r.name, COUNT(u)
            FROM Role r
            LEFT JOIN r.users u
            GROUP BY r.name
            ORDER BY r.name
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "20"))
    List<Object[]> getRoleStatistics();

    // ============================================================
    // Поисковые методы
    // ============================================================

    /**
     * Ищет роли по части имени (без учета регистра).
     *
     * <p><b>Использование:</b> Для автодополнения в UI.
     *
     * @param name часть имени роли
     * @return список ролей, содержащих указанную строку в имени
     */
    @Query("""
            SELECT r
            FROM Role r
            WHERE LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%'))
            ORDER BY r.name
            LIMIT 50
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "10"))
    List<Role> findByNameContaining(@Param("name") String name);

    /**
     * Находит роли, которые не назначены ни одному пользователю (сироты).
     *
     * <p><b>Использование:</b> Для очистки неиспользуемых ролей.
     *
     * @return список ролей без пользователей
     */
    @Query("""
            SELECT r
            FROM Role r
            WHERE r.users IS EMPTY
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "15"))
    List<Role> findOrphanRoles();
}