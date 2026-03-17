package com.carsensor.platform.dto;

import java.util.List;
import org.springframework.data.domain.Page;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Универсальный ответ с пагинацией
 */
@Builder
@Schema(description = "Ответ с пагинацией")
public record PageResponse<T>(
        @JsonProperty("content")
        @Schema(description = "Содержимое текущей страницы")
        List<T> content,

        @JsonProperty("total_elements")
        @Schema(description = "Общее количество элементов",
                example = "100")
        long totalElements,

        @JsonProperty("total_pages")
        @Schema(description = "Общее количество страниц",
                example = "10")
        int totalPages,

        @JsonProperty("current_page")
        @Schema(description = "Номер текущей страницы (начиная с 0)",
                example = "0")
        int currentPage,

        @JsonProperty("page_size")
        @Schema(description = "Размер страницы",
                example = "10")
        int pageSize,

        @JsonProperty("first")
        @Schema(description = "Является ли первая страница",
                example = "true")
        boolean first,

        @JsonProperty("last")
        @Schema(description = "Является ли последняя страница",
                example = "false")
        boolean last,

        @JsonProperty("empty")
        @Schema(description = "Пустая ли страница",
                example = "false")
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