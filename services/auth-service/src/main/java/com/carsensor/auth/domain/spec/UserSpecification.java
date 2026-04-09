package com.carsensor.auth.domain.spec;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import com.carsensor.auth.domain.entity.User;
import jakarta.persistence.criteria.Predicate;

/**
 * Спецификации для динамического поиска пользователей.
 *
 * <p>Позволяет строить динамические запросы с различными комбинациями фильтров.
 *
 * @author CarSensor Platform Team
 * @since 1.0
 */
public final class UserSpecification {

    private UserSpecification() {
        // Приватный конструктор для утилитарного класса
    }

    /**
     * Создает спецификацию для поиска пользователей по имени или фамилии.
     *
     * @param searchQuery поисковый запрос
     * @return спецификация для поиска
     */
    public static Specification<User> byFirstNameOrLastName(String searchQuery) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(searchQuery)) {
                return criteriaBuilder.conjunction();
            }

            String likePattern = "%" + searchQuery.toLowerCase() + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), likePattern)
            );
        };
    }

    /**
     * Создает спецификацию для поиска пользователей по статусу активности.
     *
     * @param isActive статус активности (true - активен, false - неактивен)
     * @return спецификация для фильтрации по активности
     */
    public static Specification<User> byActiveStatus(boolean isActive) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("isActive"), isActive);
    }

    /**
     * Создает спецификацию для поиска пользователей по статусу блокировки.
     *
     * @param isLocked статус блокировки (true - заблокирован, false - не заблокирован)
     * @return спецификация для фильтрации по блокировке
     */
    public static Specification<User> byLockedStatus(boolean isLocked) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("isLocked"), isLocked);
    }

    /**
     * Создает спецификацию для поиска пользователей по роли.
     *
     * @param roleName название роли
     * @return спецификация для фильтрации по роли
     */
    public static Specification<User> byRole(String roleName) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(roleName)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.join("roles").get("name"), roleName);
        };
    }

    /**
     * Создает спецификацию для поиска пользователей по нескольким критериям.
     * Комбинирует все фильтры с помощью AND.
     *
     * @param searchQuery поисковый запрос (имя/фамилия)
     * @param isActive    статус активности (null - без фильтра)
     * @param isLocked    статус блокировки (null - без фильтра)
     * @param roleName    название роли (null - без фильтра)
     * @return комбинированная спецификация
     */
    public static Specification<User> withFilters(
            String searchQuery,
            Boolean isActive,
            Boolean isLocked,
            String roleName
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Поиск по имени/фамилии
            if (StringUtils.hasText(searchQuery)) {
                String likePattern = "%" + searchQuery.toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), likePattern)
                ));
            }

            // Фильтр по активности
            if (isActive != null) {
                predicates.add(criteriaBuilder.equal(root.get("isActive"), isActive));
            }

            // Фильтр по блокировке
            if (isLocked != null) {
                predicates.add(criteriaBuilder.equal(root.get("isLocked"), isLocked));
            }

            // Фильтр по роли
            if (StringUtils.hasText(roleName)) {
                predicates.add(criteriaBuilder.equal(root.join("roles").get("name"), roleName));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}