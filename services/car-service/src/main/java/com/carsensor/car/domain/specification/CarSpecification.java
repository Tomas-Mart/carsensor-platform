package com.carsensor.car.domain.specification;

import com.carsensor.car.domain.entity.Car;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Спецификации для фильтрации автомобилей
 */
public class CarSpecification {

    private CarSpecification() {
    }

    public static Specification<Car> withFilters(
            String brand,
            String model,
            Integer yearFrom,
            Integer yearTo,
            Integer mileageFrom,
            Integer mileageTo,
            BigDecimal priceFrom,
            BigDecimal priceTo,
            String transmission,
            String driveType,
            String searchQuery) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(brand)) {
                predicates.add(cb.equal(root.get("brand"), brand));
            }

            if (StringUtils.hasText(model)) {
                predicates.add(cb.like(cb.lower(root.get("model")),
                        "%" + model.toLowerCase() + "%"));
            }

            if (yearFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("year"), yearFrom));
            }

            if (yearTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("year"), yearTo));
            }

            if (mileageFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("mileage"), mileageFrom));
            }

            if (mileageTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("mileage"), mileageTo));
            }

            if (priceFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), priceFrom));
            }

            if (priceTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), priceTo));
            }

            if (StringUtils.hasText(transmission)) {
                predicates.add(cb.equal(root.get("transmission"), transmission));
            }

            if (StringUtils.hasText(driveType)) {
                predicates.add(cb.equal(root.get("driveType"), driveType));
            }

            if (StringUtils.hasText(searchQuery)) {
                String searchPattern = "%" + searchQuery.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("brand")), searchPattern),
                        cb.like(cb.lower(root.get("model")), searchPattern),
                        cb.like(cb.lower(root.get("description")), searchPattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}