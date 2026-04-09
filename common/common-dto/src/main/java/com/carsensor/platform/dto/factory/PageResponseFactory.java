package com.carsensor.platform.dto.factory;

import java.util.List;
import com.carsensor.platform.dto.PageResponse;

/**
 * Фабрика для создания PageResponse
 * Содержит все методы для удобного создания объектов PageResponse
 */
public final class PageResponseFactory {

    private PageResponseFactory() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }

    /**
     * Создает PageResponse из Spring Page
     *
     * @param page Spring Page объект
     * @param <T>  тип содержимого
     * @return PageResponse
     */
    public static <T> PageResponse<T> fromPage(org.springframework.data.domain.Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty()
        );
    }

    /**
     * Создает пустую страницу с размером по умолчанию 10
     *
     * @param <T> тип содержимого
     * @return пустая PageResponse
     */
    @SuppressWarnings("unchecked")
    public static <T> PageResponse<T> empty() {
        return new PageResponse<>(
                List.of(),
                0,
                0,
                0,
                10,
                true,
                true,
                true
        );
    }

    /**
     * Создает пустую страницу с указанным размером
     *
     * @param pageSize размер страницы
     * @param <T>      тип содержимого
     * @return пустая PageResponse
     */
    @SuppressWarnings("unchecked")
    public static <T> PageResponse<T> empty(int pageSize) {
        return new PageResponse<>(
                List.of(),
                0,
                0,
                0,
                pageSize,
                true,
                true,
                true
        );
    }

    /**
     * Создает PageResponse с кастомными параметрами
     *
     * @param content       содержимое страницы
     * @param totalElements общее количество элементов
     * @param currentPage   номер текущей страницы (начиная с 0)
     * @param pageSize      размер страницы
     * @param <T>           тип содержимого
     * @return PageResponse
     */
    public static <T> PageResponse<T> of(List<T> content, long totalElements, int currentPage, int pageSize) {
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        boolean first = currentPage == 0;
        boolean last = currentPage == totalPages - 1 || totalPages == 0;
        boolean empty = content == null || content.isEmpty();

        return new PageResponse<>(
                content != null ? content : List.of(),
                totalElements,
                totalPages,
                currentPage,
                pageSize,
                first,
                last,
                empty
        );
    }

    /**
     * Создает PageResponse из списка (для случаев без Spring Page)
     *
     * @param content       содержимое страницы
     * @param totalElements общее количество элементов
     * @param currentPage   номер текущей страницы
     * @param pageSize      размер страницы
     * @param <T>           тип содержимого
     * @return PageResponse
     */
    public static <T> PageResponse<T> fromList(List<T> content, long totalElements, int currentPage, int pageSize) {
        return of(content, totalElements, currentPage, pageSize);
    }

    /**
     * Создает PageResponse для первой страницы
     *
     * @param content       содержимое страницы
     * @param totalElements общее количество элементов
     * @param pageSize      размер страницы
     * @param <T>           тип содержимого
     * @return PageResponse для первой страницы
     */
    public static <T> PageResponse<T> firstPage(List<T> content, long totalElements, int pageSize) {
        return of(content, totalElements, 0, pageSize);
    }
}