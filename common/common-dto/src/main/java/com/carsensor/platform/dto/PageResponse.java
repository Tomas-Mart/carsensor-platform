package com.carsensor.platform.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Универсальный ответ с пагинацией
 * Только данные — без логики создания
 */
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
    // Компактный конструктор для нормализации
    public PageResponse {
        if (content == null) {
            content = List.of();
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be positive");
        }
        if (totalPages < 0) {
            throw new IllegalArgumentException("totalPages cannot be negative");
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements cannot be negative");
        }
    }
}