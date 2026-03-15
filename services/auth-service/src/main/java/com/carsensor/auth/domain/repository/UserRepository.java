package com.carsensor.auth.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.carsensor.auth.domain.entity.User;

/**
 * Репозиторий для работы с пользователями
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Базовые методы с EntityGraph для загрузки связей
    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<User> findByEmail(String email);

    // Методы проверки существования
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // Статистические методы - ИСПРАВЛЕНО: используем правильное имя поля
    long countByIsActiveTrue();

    long countByCreatedAtAfter(LocalDateTime date);

    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = :isActive")
    long countByActiveStatus(@Param("isActive") boolean isActive);

    // Поисковые методы
    List<User> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            @Param("firstName") String firstName,
            @Param("lastName") String lastName);

    // Методы с JOIN FETCH для производительности
    @Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.username = :username")
    Optional<User> findByUsernameWithRoles(@Param("username") String username);

    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :start AND :end")
    List<User> findUsersCreatedBetween(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    // Метод для поиска всех активных пользователей с их ролями - ИСПРАВЛЕНО
    @Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.isActive = true")
    List<User> findAllActiveUsersWithRoles();
}