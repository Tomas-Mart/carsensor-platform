package com.carsensor.auth.domain.entity;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.carsensor.auth.domain.valueobject.Email;
import com.carsensor.auth.domain.valueobject.Password;
import com.carsensor.auth.domain.valueobject.Username;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Сущность пользователя - Aggregate Root.
 *
 * <p>Использует Java 21 features:
 * <ul>
 *   <li>Records для Value Objects</li>
 *   <li>Text blocks для SQL (в репозитории)</li>
 *   <li>var для локальных переменных</li>
 *   <li>Switch expressions для статусов</li>
 * </ul>
 *
 * @author CarSensor Platform Team
 * @since 1.0
 */
@Slf4j
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "username", name = "uk_users_username"),
        @UniqueConstraint(columnNames = "email", name = "uk_users_email")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Максимальное количество неудачных попыток входа до блокировки
     */
    private static final int MAX_FAILED_ATTEMPTS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String username;

    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "is_locked")
    @Builder.Default
    private boolean isLocked = false;

    @Column(name = "failed_attempts", nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;

    @Column(name = "lock_time")
    private LocalDateTime lockTime;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    // ============================================================
    // ФАБРИЧНЫЙ МЕТОД
    // ============================================================

    /**
     * Фабричный метод для создания нового пользователя.
     *
     * @param username  имя пользователя (value object)
     * @param email     email адрес (value object)
     * @param password  пароль (value object)
     * @param firstName имя
     * @param lastName  фамилия
     * @param roles     начальный набор ролей
     * @return созданный пользователь
     */
    public static User create(
            Username username,
            Email email,
            Password password,
            String firstName,
            String lastName,
            Set<Role> roles
    ) {
        log.debug("Создание пользователя через фабричный метод: {}", username.value());

        var user = new User();
        user.username = username.value();
        user.email = email.value();
        user.password = password.getEncryptedValue();
        user.firstName = firstName;
        user.lastName = lastName;
        user.isActive = true;
        user.isLocked = false;
        user.failedLoginAttempts = 0;
        user.roles = new HashSet<>(roles);

        return user;
    }

    // ============================================================
    // МЕТОДЫ БИЗНЕС-ЛОГИКИ
    // ============================================================

    /**
     * Блокирует пользователя.
     */
    public void block() {
        log.info("Блокировка пользователя: {}", username);
        this.isLocked = true;
        this.lockTime = LocalDateTime.now();
    }

    /**
     * Разблокирует пользователя.
     */
    public void unblock() {
        log.info("Разблокировка пользователя: {}", username);
        this.isLocked = false;
        this.lockTime = null;
        this.failedLoginAttempts = 0;
    }

    /**
     * Изменяет пароль пользователя.
     *
     * @param newPassword новый пароль (value object)
     */
    public void changePassword(Password newPassword) {
        log.info("Смена пароля для пользователя: {}", username);
        this.password = newPassword.getEncryptedValue();
        this.failedLoginAttempts = 0;
    }

    /**
     * Обновляет профиль пользователя.
     *
     * @param firstName новое имя (может быть null)
     * @param lastName  новая фамилия (может быть null)
     */
    public void updateProfile(String firstName, String lastName) {
        log.info("Обновление профиля пользователя: {}", username);

        // Java 21: switch expression для проверки
        switch (firstName) {
            case null -> {
            }
            default -> this.firstName = firstName;
        }

        if (lastName != null) {
            this.lastName = lastName;
        }
    }

    /**
     * Добавляет роль пользователю.
     *
     * @param role роль для добавления
     */
    public void addRole(Role role) {
        log.debug("Добавление роли {} пользователю: {}", role.getName(), username);
        this.roles.add(role);
    }

    /**
     * Удаляет роль у пользователя.
     *
     * @param role роль для удаления
     */
    public void removeRole(Role role) {
        log.debug("Удаление роли {} у пользователя: {}", role.getName(), username);
        this.roles.remove(role);
    }

    /**
     * Увеличивает счетчик неудачных попыток входа.
     * При достижении лимита блокирует пользователя.
     */
    public void incrementFailedAttempts() {
        log.debug("Увеличение счетчика неудачных попыток для пользователя: {}", username);
        this.failedLoginAttempts++;

        if (this.failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
            log.warn("Достигнут лимит неудачных попыток, пользователь заблокирован: {}", username);
            block();
        }
    }

    /**
     * Сбрасывает счетчик неудачных попыток входа.
     */
    public void resetFailedAttempts() {
        log.debug("Сброс счетчика неудачных попыток для пользователя: {}", username);
        this.failedLoginAttempts = 0;
    }

    /**
     * Деактивирует учетную запись пользователя.
     */
    public void deactivate() {
        log.info("Деактивация пользователя: {}", username);
        this.isActive = false;
    }

    /**
     * Активирует учетную запись пользователя.
     */
    public void activate() {
        log.info("Активация пользователя: {}", username);
        this.isActive = true;
    }

    // ============================================================
    // МЕТОДЫ ПРОВЕРКИ СОСТОЯНИЯ
    // ============================================================

    /**
     * Проверяет, заблокирован ли пользователь.
     */
    public boolean isLocked() {
        return isLocked;
    }

    /**
     * Проверяет, активен ли пользователь.
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Проверяет, активен ли пользователь и не заблокирован.
     */
    public boolean isActiveAndNotLocked() {
        return isActive && !isLocked;
    }

    /**
     * Проверяет наличие роли у пользователя.
     *
     * @param roleName название роли
     * @return true если пользователь имеет указанную роль
     */
    public boolean hasRole(String roleName) {
        return roles.stream()
                .anyMatch(role -> role.getName().equals(roleName));
    }

    /**
     * Возвращает статус пользователя в виде текста (Java 21 switch expression).
     *
     * @return текстовое описание статуса
     */
    public String getStatusText() {
        return switch (this) {
            case User u when u.isLocked() && u.isActive() -> "Заблокирован";
            case User u when !u.isActive() -> "Деактивирован";
            case User u when u.isActive() && !u.isLocked() -> "Активен";
            default -> "Неизвестно";
        };
    }

    // ============================================================
    // SPRING SECURITY МЕТОДЫ
    // ============================================================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> new SimpleGrantedAuthority(permission.getName()))
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !isLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }

    // ============================================================
    // GETTERS ДЛЯ VALUE OBJECTS
    // ============================================================

    public Username getUsernameAsValueObject() {
        return new Username(username);
    }

    public Email getEmailAsValueObject() {
        return new Email(email);
    }

    public Password getPasswordAsValueObject() {
        return new Password(password);
    }
}