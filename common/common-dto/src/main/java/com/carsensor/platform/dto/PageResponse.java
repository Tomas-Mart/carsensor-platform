package com.carsensor.platform.dto;

import java.util.List;
import org.springframework.data.domain.Page;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * Универсальный ответ с пагинацией
 */
@Builder
public record PageResponse<T>(
        @JsonProperty("content")
        List<T> content,

        @JsonProperty("total_elements")
        long totalElements,

        @JsonProperty("total_pages")
        int totalPages,

        @JsonProperty("current_page")
        int currentPage,

        @JsonProperty("page_size")
        int pageSize,

        @JsonProperty("first")
        boolean first,

        @JsonProperty("last")
        boolean last,

        @JsonProperty("empty")
        boolean empty
) {
    public static <T> PageResponse<T> fromPage(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }
}