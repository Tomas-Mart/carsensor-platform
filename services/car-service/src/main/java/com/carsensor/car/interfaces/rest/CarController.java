package com.carsensor.car.interfaces.rest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.carsensor.car.application.service.command.CarCommandService;
import com.carsensor.car.application.service.query.CarQueryService;
import com.carsensor.platform.dto.CarDto;
import com.carsensor.platform.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST контроллер для управления автомобилями.
 *
 * <p>Предоставляет эндпоинты для работы с автомобилями:
 * <ul>
 *   <li>Получение списка автомобилей с фильтрацией, сортировкой и пагинацией</li>
 *   <li>Получение детальной информации по ID</li>
 *   <li>Создание нового автомобиля (только для ADMIN)</li>
 *   <li>Обновление существующего автомобиля (только для ADMIN)</li>
 *   <li>Удаление автомобиля (только для ADMIN)</li>
 * </ul>
 *
 * <p><b>Права доступа:</b>
 * <ul>
 *   <li>GET /api/v1/cars - доступно всем аутентифицированным пользователям</li>
 *   <li>GET /api/v1/cars/{id} - доступно всем аутентифицированным пользователям</li>
 *   <li>POST /api/v1/cars - только для пользователей с ролью ADMIN</li>
 *   <li>PUT /api/v1/cars/{id} - только для пользователей с ролью ADMIN</li>
 *   <li>DELETE /api/v1/cars/{id} - только для пользователей с ролью ADMIN</li>
 * </ul>
 *
 * <p><b>Поддерживаемые поля для сортировки:</b>
 * <ul>
 *   <li>id - идентификатор автомобиля</li>
 *   <li>brand - марка автомобиля</li>
 *   <li>model - модель автомобиля</li>
 *   <li>year - год выпуска</li>
 *   <li>mileage - пробег</li>
 *   <li>price - цена</li>
 *   <li>createdAt - дата создания</li>
 *   <li>updatedAt - дата обновления</li>
 * </ul>
 *
 * @author CarSensor Platform Team
 * @version 1.0
 * @see CarQueryService
 * @see CarCommandService
 * @see CarDto
 * @see PageResponse
 * @since 1.0
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cars")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Cars", description = "API для управления автомобилями")
public class CarController {

    /**
     * Допустимые поля для сортировки.
     * Используются для валидации и предотвращения SQL инъекций.
     */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "brand", "model", "year", "mileage", "price", "createdAt", "updatedAt"
    );

    private final CarQueryService carQueryService;
    private final CarCommandService carCommandService;

    // ============================================================
    // GET /api/v1/cars - получение списка автомобилей
    // ============================================================

    /**
     * Получает список автомобилей с фильтрацией, сортировкой и пагинацией.
     *
     * <p><b>Поддерживаемые параметры фильтрации:</b>
     * <ul>
     *   <li>brand - марка автомобиля (точное совпадение)</li>
     *   <li>model - модель автомобиля (частичное совпадение, регистронезависимо)</li>
     *   <li>yearFrom / yearTo - диапазон годов выпуска</li>
     *   <li>mileageFrom / mileageTo - диапазон пробега</li>
     *   <li>priceFrom / priceTo - диапазон цен</li>
     *   <li>transmission - тип трансмиссии (AT, MT, CVT)</li>
     *   <li>driveType - тип привода (2WD, 4WD, AWD)</li>
     *   <li>search - полнотекстовый поиск по марке, модели и описанию</li>
     * </ul>
     *
     * <p><b>Сортировка:</b>
     * <ul>
     *   <li>Формат: {@code sort=field,order}</li>
     *   <li>Пример: {@code sort=price,DESC}</li>
     *   <li>Поддерживаемые поля: id, brand, model, year, mileage, price, createdAt, updatedAt</li>
     *   <li>Направление: ASC (по возрастанию) или DESC (по убыванию)</li>
     *   <li>Можно указать несколько полей: {@code sort=brand,ASC&sort=price,DESC}</li>
     * </ul>
     *
     * <p><b>Пагинация:</b>
     * <ul>
     *   <li>page - номер страницы (0-based, по умолчанию 0)</li>
     *   <li>size - размер страницы (по умолчанию 20)</li>
     * </ul>
     *
     * @param brand        марка автомобиля (опционально)
     * @param model        модель автомобиля (опционально)
     * @param yearFrom     минимальный год выпуска (опционально)
     * @param yearTo       максимальный год выпуска (опционально)
     * @param mileageFrom  минимальный пробег в км (опционально)
     * @param mileageTo    максимальный пробег в км (опционально)
     * @param priceFrom    минимальная цена в рублях (опционально)
     * @param priceTo      максимальная цена в рублях (опционально)
     * @param transmission тип трансмиссии (опционально, возможные значения: AT, MT, CVT)
     * @param driveType    тип привода (опционально, возможные значения: 2WD, 4WD, AWD)
     * @param search       полнотекстовый поиск (опционально)
     * @param page         номер страницы (по умолчанию 0)
     * @param size         размер страницы (по умолчанию 20)
     * @param sort         сортировка (по умолчанию id,DESC)
     * @return страница с автомобилями, содержащая список автомобилей и метаинформацию
     */
    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_CARS') or hasRole('ADMIN') or isAuthenticated()")
    @Operation(summary = "Получить список автомобилей",
            description = "Возвращает список автомобилей с возможностью фильтрации, сортировки и пагинации. " +
                          "Доступно всем аутентифицированным пользователям.")
    public PageResponse<CarDto> getCars(
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo,
            @RequestParam(required = false) Integer mileageFrom,
            @RequestParam(required = false) Integer mileageTo,
            @RequestParam(required = false) Integer priceFrom,
            @RequestParam(required = false) Integer priceTo,
            @RequestParam(required = false) String transmission,
            @RequestParam(required = false) String driveType,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,DESC") String[] sort
    ) {

        log.debug("GET /api/v1/cars with page: {}, size: {}, sort: {}", page, size, Arrays.toString(sort));

        BigDecimal priceFromBigDecimal = priceFrom != null ? BigDecimal.valueOf(priceFrom) : null;
        BigDecimal priceToBigDecimal = priceTo != null ? BigDecimal.valueOf(priceTo) : null;

        Sort sortOrder = createSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortOrder);

        return carQueryService.getCars(
                brand, model, yearFrom, yearTo, mileageFrom, mileageTo,
                priceFromBigDecimal, priceToBigDecimal, transmission, driveType, search, pageable);
    }

    // ============================================================
    // GET /api/v1/cars/{id} - получение автомобиля по ID
    // ============================================================

    /**
     * Получает детальную информацию об автомобиле по его уникальному идентификатору.
     *
     * <p>Возвращает полную информацию об автомобиле, включая:
     * <ul>
     *   <li>Основные характеристики (марка, модель, год, пробег, цена)</li>
     *   <li>Технические характеристики (трансмиссия, привод, объем двигателя)</li>
     *   <li>Внешние данные (цвет кузова, цвет салона)</li>
     *   <li>Фотографии (список URL)</li>
     *   <li>Метаданные (дата парсинга, дата создания, дата обновления)</li>
     * </ul>
     *
     * @param id уникальный идентификатор автомобиля
     * @return данные автомобиля в формате {@link CarDto}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_CARS') or hasRole('ADMIN')")
    @Operation(summary = "Получить автомобиль по ID",
            description = "Возвращает детальную информацию об автомобиле. " +
                          "Доступно всем аутентифицированным пользователям.")
    public CarDto getCarById(@PathVariable Long id) {
        log.debug("GET /api/v1/cars/{}", id);
        return carQueryService.getCarById(id);
    }

    // ============================================================
    // POST /api/v1/cars - создание автомобиля
    // ============================================================

    /**
     * Создает новый автомобиль.
     *
     * <p>Доступно только для пользователей с ролью ADMIN.
     * При создании автомобиля выполняются следующие проверки:
     * <ul>
     *   <li>Валидация входных данных (марка, модель, год, пробег, цена)</li>
     *   <li>Проверка уникальности externalId (если указан)</li>
     *   <li>Автоматическое заполнение метаданных (createdAt, updatedAt)</li>
     * </ul>
     *
     * @param carDto данные нового автомобиля (должны быть валидными)
     * @return созданный автомобиль с присвоенным ID и заполненными метаданными
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('CREATE_CARS') or hasRole('ADMIN')")
    @Operation(summary = "Создать автомобиль", description = "Создает новый автомобиль." +
                                                             "Доступно только для пользователей с ролью ADMIN.")
    public CarDto createCar(@Valid @RequestBody CarDto carDto) {
        log.debug("POST /api/v1/cars - brand: {}, model: {}", carDto.brand(), carDto.model());
        return carCommandService.createCar(carDto);
    }

    // ============================================================
    // PUT /api/v1/cars/{id} - обновление автомобиля
    // ============================================================

    /**
     * Обновляет существующий автомобиль.
     *
     * <p>Доступно только для пользователей с ролью ADMIN.
     * При обновлении автомобиля:
     * <ul>
     *   <li>Валидируются входные данные</li>
     *   <li>Обновляются только переданные поля (паттерн частичного обновления)</li>
     *   <li>Автоматически обновляется поле updatedAt</li>
     *   <li>Используется оптимистичная блокировка (версионирование)</li>
     * </ul>
     *
     * @param id     уникальный идентификатор обновляемого автомобиля
     * @param carDto обновленные данные автомобиля
     * @return обновленный автомобиль с актуальными данными
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDIT_CARS') or hasRole('ADMIN')")
    @Operation(summary = "Обновить автомобиль",
            description = "Обновляет существующий автомобиль. Доступно только для пользователей с ролью ADMIN.")
    public CarDto updateCar(@PathVariable Long id, @Valid @RequestBody CarDto carDto) {
        log.debug("PUT /api/v1/cars/{}", id);
        return carCommandService.updateCar(id, carDto);
    }

    // ============================================================
    // DELETE /api/v1/cars/{id} - удаление автомобиля
    // ============================================================

    /**
     * Удаляет автомобиль по его уникальному идентификатору.
     *
     * <p>Доступно только для пользователей с ролью ADMIN.
     * Удаление выполняется физически (hard delete) с каскадным удалением связанных данных.
     *
     * @param id уникальный идентификатор удаляемого автомобиля
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('DELETE_CARS') or hasRole('ADMIN')")
    @Operation(summary = "Удалить автомобиль",
            description = "Удаляет автомобиль по ID. Доступно только для пользователей с ролью ADMIN.")
    public void deleteCar(@PathVariable Long id) {
        log.debug("DELETE /api/v1/cars/{}", id);
        carCommandService.deleteCar(id);
    }

    // ============================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ============================================================

    /**
     * Создает объект Sort из массива строк формата "field,order".
     *
     * <p>Поддерживает два формата ввода:
     * <ul>
     *   <li>Обычный: sort=price,DESC (один параметр с запятой)</li>
     *   <li>Spring формат: sort=price&sort=DESC (раздельные параметры)</li>
     *   <li>Смешанный: sort=price,DESC&sort=year,ASC</li>
     * </ul>
     *
     * <p>Валидирует имена полей для предотвращения SQL инъекций.
     *
     * @param sort массив строк сортировки (например, {"price,DESC", "year,ASC"} или {"price", "DESC"})
     * @return объект Sort для использования в Pageable
     */
    private Sort createSort(String[] sort) {
        if (sort == null || sort.length == 0) {
            log.debug("No sort parameters provided, using default: id DESC");
            return Sort.by(Sort.Direction.DESC, "id");
        }

        List<Sort.Order> orders = new ArrayList<>();
        String lastProperty = null;

        for (String sortParam : sort) {
            if (sortParam == null || sortParam.trim().isEmpty()) {
                continue;
            }

            String param = sortParam.trim();
            String upperParam = param.toUpperCase();

            // Если это направление сортировки (ASC/DESC) в любом регистре
            if (upperParam.equals("ASC") || upperParam.equals("DESC")) {
                if (lastProperty != null) {
                    Sort.Direction direction = upperParam.equals("DESC") ?
                            Sort.Direction.DESC : Sort.Direction.ASC;
                    orders.add(new Sort.Order(direction, lastProperty));
                    log.debug("Added sort order: {} {}", lastProperty, direction);
                    lastProperty = null;
                }
                continue;
            }

            // Обработка формата "field,direction"
            if (param.contains(",")) {
                String[] parts = param.split(",");
                String property = parts[0].trim();
                String directionStr = parts.length > 1 ? parts[1].trim().toUpperCase() : "ASC";

                if (!ALLOWED_SORT_FIELDS.contains(property)) {
                    log.warn("Invalid sort field: {}, skipping", property);
                    continue;
                }

                Sort.Direction direction = "DESC".equals(directionStr) ?
                        Sort.Direction.DESC : Sort.Direction.ASC;
                orders.add(new Sort.Order(direction, property));
                log.debug("Added sort order: {} {}", property, direction);
                continue;
            }

            // Это поле сортировки без направления
            if (!ALLOWED_SORT_FIELDS.contains(param)) {
                log.warn("Invalid sort field: {}, skipping", param);
                continue;
            }
            lastProperty = param;
        }

        // Если осталось поле без направления
        if (lastProperty != null) {
            orders.add(new Sort.Order(Sort.Direction.ASC, lastProperty));
            log.debug("Added sort order: {} ASC", lastProperty);
        }

        return orders.isEmpty() ? Sort.by(Sort.Direction.DESC, "id") : Sort.by(orders);
    }
}