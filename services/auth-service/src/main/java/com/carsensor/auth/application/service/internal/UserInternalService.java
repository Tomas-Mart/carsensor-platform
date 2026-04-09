// application/service/UserInternalService.java
package com.carsensor.auth.application.service.internal;

import java.util.Optional;
import com.carsensor.auth.domain.entity.User;

/**
 * Внутренний сервис для доменного слоя.
 * Не должен использоваться извне (интерфейсы REST).
 * Принцип: Separation of Concerns - отделение внутренних операций.
 */
public interface UserInternalService {

    /**
     * Получение сущности пользователя по username (для внутреннего использования)
     */
    Optional<User> findUserEntityByUsername(String username);
}