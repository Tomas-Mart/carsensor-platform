package com.carsensor.auth.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.carsensor.auth.domain.entity.Role;

/**
 * Репозиторий для работы с ролями
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    // Базовые методы
    Optional<Role> findByName(String name);

    boolean existsByName(String name);

    // Методы с JOIN FETCH для производительности
    @Query("SELECT r FROM Role r JOIN FETCH r.permissions WHERE r.name = :name")
    Optional<Role> findByNameWithPermissions(@Param("name") String name);

    @Query("SELECT r FROM Role r JOIN FETCH r.permissions WHERE r.name IN :names")
    Set<Role> findByNameInWithPermissions(@Param("names") Set<String> names);

    @Query("SELECT r FROM Role r JOIN FETCH r.permissions")
    List<Role> findAllWithPermissions();

    // Статистические методы
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.id = :roleId")
    long countUsersByRoleId(@Param("roleId") Long roleId);

    @Query("SELECT r.name, COUNT(u) FROM Role r LEFT JOIN r.users u GROUP BY r.name")
    List<Object[]> getRoleStatistics();

    // Поиск ролей по имени (частичное совпадение) - ИСПРАВЛЕНО
    @Query("SELECT r FROM Role r WHERE lower(r.name) LIKE lower(concat('%', :name, '%'))")
    List<Role> findByNameContaining(@Param("name") String name);

    // Получение ролей без пользователей (сирот)
    @Query("SELECT r FROM Role r WHERE r.users IS EMPTY")
    List<Role> findOrphanRoles();
}