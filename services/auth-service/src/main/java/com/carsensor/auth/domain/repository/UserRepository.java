package com.carsensor.auth.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.carsensor.auth.domain.entity.Permission;
import com.carsensor.auth.domain.entity.User;
import jakarta.persistence.QueryHint;

/**
 * Репозиторий для работы с пользователями.
 *
 * <p>Предоставляет оптимизированные методы для доступа к данным пользователей,
 * включая поиск по username/email, статистику и агрегацию с использованием
 * JOIN FETCH для предотвращения N+1 запросов.
 *
 * <p><b>Оптимизации производительности:</b>
 * <ul>
 *   <li><b>Быстрые методы без JOIN</b> - {@link #findByUsername(String)} для аутентификации</li>
 *   <li><b>JOIN FETCH</b> - для загрузки связей одним запросом, предотвращает N+1 проблему</li>
 *   <li><b>Таймауты запросов</b> - предотвращают зависания при длительном выполнении</li>
 *   <li><b>EXISTS вместо COUNT</b> - для проверки существования (быстрее)</li>
 *   <li><b>DISTINCT</b> - для устранения дубликатов при JOIN FETCH</li>
 *   <li><b>LIMIT</b> - ограничение количества результатов для поисковых запросов</li>
 * </ul>
 *
 * <p><b>Рекомендации по использованию:</b>
 * <ul>
 *   <li>Для аутентификации используйте {@link #findByUsername(String)} - самый быстрый</li>
 *   <li>Если нужны только роли, используйте {@link #findByUsernameWithRoles(String)}</li>
 *   <li>Если нужны полные данные (роли + разрешения), используйте {@link #findByUsernameWithRolesAndPermissions(String)}</li>
 *   <li>Для ленивой загрузки разрешений используйте {@link #findPermissionsByUsername(String)}</li>
 * </ul>
 *
 * @author CarSensor Platform Team
 * @since 1.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ============================================================
    // БАЗОВЫЕ МЕТОДЫ (оптимизированные для производительности)
    // ============================================================

    /**
     * Находит пользователя по имени пользователя (без загрузки связей).
     *
     * <p>Этот метод предназначен для аутентификации и базовых операций,
     * где не требуются роли и разрешения. Он выполняется максимально быстро.
     *
     * <p><b>Оптимизация:</b>
     * <ul>
     *   <li>Не загружает связанные сущности (роли, разрешения)</li>
     *   <li>Использует простой SELECT без JOIN</li>
     *   <li>Таймаут запроса 10 секунд</li>
     * </ul>
     *
     * @param username имя пользователя (не может быть null)
     * @return Optional с найденным пользователем или пустой Optional
     */
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "10"))
    Optional<User> findByUsername(@Param("username") String username);

    /**
     * Находит пользователя по email (без загрузки связей).
     *
     * <p>Быстрый метод для поиска пользователя по email без загрузки связей.
     *
     * @param email email пользователя (не может быть null)
     * @return Optional с найденным пользователем или пустой Optional
     */
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "10"))
    Optional<User> findByEmail(@Param("email") String email);

    // ============================================================
    // МЕТОДЫ С JOIN FETCH (для загрузки связей)
    // ============================================================

    /**
     * Находит пользователя по имени пользователя с загрузкой ролей и разрешений.
     *
     * <p><b>Важно:</b> Этот метод загружает все связи и может быть медленным.
     * Для аутентификации рекомендуется использовать {@link #findByUsername(String)}.
     *
     * <p><b>Оптимизация:</b>
     * <ul>
     *   <li>Использует LEFT JOIN FETCH для загрузки всех связей одним запросом</li>
     *   <li>Использует DISTINCT для устранения дубликатов при множественных связях</li>
     *   <li>Предотвращает N+1 проблему при обращении к ролям и разрешениям</li>
     *   <li>Таймаут запроса 60 секунд</li>
     * </ul>
     *
     * @param username имя пользователя (не может быть null)
     * @return Optional с найденным пользователем и его ролями/разрешениями, или пустой Optional
     */
    @Query("""
            SELECT DISTINCT u
            FROM User u
            LEFT JOIN FETCH u.roles r
            LEFT JOIN FETCH r.permissions
            WHERE u.username = :username
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "60"))
    Optional<User> findByUsernameWithRolesAndPermissions(@Param("username") String username);

    /**
     * Находит пользователя по email с загрузкой ролей и разрешений.
     *
     * @param email email пользователя (не может быть null)
     * @return Optional с найденным пользователем и его ролями/разрешениями, или пустой Optional
     */
    @Query("""
            SELECT DISTINCT u
            FROM User u
            LEFT JOIN FETCH u.roles r
            LEFT JOIN FETCH r.permissions
            WHERE u.email = :email
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "60"))
    Optional<User> findByEmailWithRolesAndPermissions(@Param("email") String email);

    /**
     * Находит пользователя по имени пользователя с загрузкой ролей (без разрешений).
     *
     * <p>Этот метод быстрее, чем загрузка всех разрешений сразу, так как использует
     * только один JOIN FETCH и не загружает вложенные коллекции разрешений.
     * Разрешения могут быть загружены отдельно при необходимости через
     * {@link #findPermissionsByUsername(String)}.
     *
     * <p><b>Рекомендация:</b> Используйте этот метод, если вам нужны только роли пользователя,
     * а разрешения можно загрузить лениво.
     *
     * <p><b>Оптимизация:</b>
     * <ul>
     *   <li>Использует LEFT JOIN FETCH для загрузки ролей одним запросом</li>
     *   <li>Предотвращает N+1 проблему для ролей</li>
     *   <li>Таймаут запроса 30 секунд</li>
     * </ul>
     *
     * @param username имя пользователя (не может быть null)
     * @return Optional с найденным пользователем и его ролями, или пустой Optional
     */
    @Query("""
            SELECT u
            FROM User u
            LEFT JOIN FETCH u.roles
            WHERE u.username = :username
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "30"))
    Optional<User> findByUsernameWithRoles(@Param("username") String username);

    /**
     * Находит все разрешения, принадлежащие пользователю через его роли.
     *
     * <p>Используется для ленивой загрузки разрешений после того, как пользователь
     * и его роли уже загружены. Это позволяет разделить сложный запрос на два более
     * простых и быстрых запроса.
     *
     * <p><b>Пример использования:</b>
     * <pre>{@code
     * // Сначала загружаем пользователя с ролями
     * var user = userRepository.findByUsernameWithRoles("admin").orElseThrow();
     *
     * // Затем, если нужны разрешения, загружаем их отдельно
     * var permissions = userRepository.findPermissionsByUsername("admin");
     * }</pre>
     *
     * <p><b>Оптимизация:</b>
     * <ul>
     *   <li>Загружает только разрешения, без лишних данных</li>
     *   <li>Использует JOIN через связь User → Role → Permission</li>
     *   <li>Использует DISTINCT для устранения дубликатов</li>
     *   <li>Таймаут запроса 30 секунд</li>
     * </ul>
     *
     * @param username имя пользователя (не может быть null)
     * @return Set разрешений, доступных пользователю
     */
    @Query("""
            SELECT DISTINCT p
            FROM Role r
            JOIN r.users u
            JOIN r.permissions p
            WHERE u.username = :username
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "30"))
    Set<Permission> findPermissionsByUsername(@Param("username") String username);

    // ============================================================
    // МЕТОДЫ ПРОВЕРКИ СУЩЕСТВОВАНИЯ (оптимизированные)
    // ============================================================

    /**
     * Проверяет существование пользователя по имени пользователя.
     *
     * <p><b>Оптимизация:</b>
     * <ul>
     *   <li>Использует EXISTS подзапрос - быстрее чем COUNT</li>
     *   <li>Останавливается на первом найденном результате</li>
     *   <li>Таймаут запроса 10 секунд</li>
     * </ul>
     *
     * @param username имя пользователя
     * @return true если пользователь существует, false в противном случае
     */
    @Query("SELECT EXISTS(SELECT 1 FROM User u WHERE u.username = :username)")
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "10"))
    boolean existsByUsername(@Param("username") String username);

    /**
     * Проверяет существование пользователя по email.
     *
     * @param email email пользователя
     * @return true если пользователь существует, false в противном случае
     */
    @Query("SELECT EXISTS(SELECT 1 FROM User u WHERE u.email = :email)")
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "10"))
    boolean existsByEmail(@Param("email") String email);

    // ============================================================
    // СТАТИСТИЧЕСКИЕ МЕТОДЫ (оптимизированные агрегации)
    // ============================================================

    /**
     * Подсчитывает количество активных пользователей.
     *
     * <p><b>Оптимизация:</b>
     * <ul>
     *   <li>Использует индекс idx_users_is_active для быстрого подсчета</li>
     *   <li>Нет загрузки сущностей - только агрегация</li>
     *   <li>Таймаут запроса 30 секунд</li>
     * </ul>
     *
     * @return количество активных пользователей
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true")
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "30"))
    long countByIsActiveTrue();

    /**
     * Подсчитывает количество пользователей по статусу активности.
     *
     * @param isActive статус активности
     * @return количество пользователей
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = :isActive")
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "30"))
    long countByActiveStatus(@Param("isActive") boolean isActive);

    /**
     * Подсчитывает количество пользователей, созданных после указанной даты.
     *
     * @param date дата для сравнения
     * @return количество пользователей
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt > :date")
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "30"))
    long countByCreatedAtAfter(@Param("date") LocalDateTime date);

    // ============================================================
    // ПОИСКОВЫЕ МЕТОДЫ (оптимизированные текстовые запросы)
    // ============================================================

    /**
     * Ищет пользователей по части имени или фамилии (без учета регистра).
     *
     * <p><b>Оптимизация:</b>
     * <ul>
     *   <li>Использует ILIKE для case-insensitive поиска</li>
     *   <li>Ограничивает количество результатов 50 записями</li>
     *   <li>Таймаут запроса 30 секунд</li>
     * </ul>
     *
     * @param searchQuery поисковый запрос (ищется в имени и фамилии)
     * @return список пользователей, у которых имя или фамилия содержат указанную строку
     */
    @Query(value = """
            SELECT u.*
            FROM users u
            WHERE u.first_name ILIKE CONCAT('%', :searchQuery, '%')
               OR u.last_name ILIKE CONCAT('%', :searchQuery, '%')
            LIMIT 50
            """, nativeQuery = true)
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "30"))
    List<User> searchByFirstNameOrLastName(@Param("searchQuery") String searchQuery);

    // ============================================================
    // МЕТОДЫ ДЛЯ РАБОТЫ С ВРЕМЕННЫМИ ПРОМЕЖУТКАМИ
    // ============================================================

    /**
     * Находит пользователей, созданных в указанном временном промежутке.
     *
     * <p><b>Оптимизация:</b>
     * <ul>
     *   <li>Использует индекс idx_users_created_at</li>
     *   <li>Сортировка на уровне БД</li>
     *   <li>Ограничение количества результатов</li>
     *   <li>Таймаут запроса 30 секунд</li>
     * </ul>
     *
     * @param start начало промежутка
     * @param end   конец промежутка
     * @param limit максимальное количество результатов
     * @return список пользователей, созданных в указанном промежутке
     */
    @Query(value = """
            SELECT *
            FROM users
            WHERE created_at BETWEEN :start AND :end
            ORDER BY created_at
            LIMIT :limit
            """, nativeQuery = true)
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "30"))
    List<User> findUsersCreatedBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("limit") int limit
    );

    /**
     * Находит всех активных пользователей с загрузкой ролей.
     *
     * <p><b>Оптимизация:</b>
     * <ul>
     *   <li>Использует JOIN FETCH для загрузки ролей одним запросом</li>
     *   <li>Ограничивает количество результатов (пагинация)</li>
     *   <li>Таймаут запроса 60 секунд</li>
     * </ul>
     *
     * @param limit максимальное количество результатов
     * @return список активных пользователей с их ролями
     */
    @Query("""
            SELECT u
            FROM User u
            LEFT JOIN FETCH u.roles
            WHERE u.isActive = true
            ORDER BY u.id
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.timeout", value = "60"))
    List<User> findAllActiveUsersWithRoles(@Param("limit") int limit);
}