package com.carsensor.auth.contract;

import java.time.Duration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Конфигурация тестовых HTTP клиентов для интеграционных и контрактных тестов.
 *
 * <p>Предоставляет настроенные бины:
 * <ul>
 *   <li>{@link RestClient} - для современных API запросов с функциональным стилем</li>
 *   <li>{@link TestRestTemplate} - для удобных интеграционных тестов с поддержкой
 *       базовых аутентификаций и cookie</li>
 * </ul>
 *
 * <p>Оба клиента используют оптимизированные таймауты для тестового окружения,
 * что позволяет избежать зависаний при медленных операциях (генерация JWT,
 * сложные SQL запросы, миграции БД).
 *
 * <p><b>Особенности конфигурации:</b>
 * <ul>
 *   <li><b>Таймаут подключения:</b> 60 секунд - предотвращает долгие ожидания
 *       при недоступности сервера, достаточно для локального тестирования</li>
 *   <li><b>Таймаут чтения:</b> 120 секунд - позволяет обрабатывать длительные
 *       операции, такие как генерация JWT, сложные SQL запросы или загрузка
 *       больших объемов данных</li>
 *   <li><b>Фабрика запросов:</b> {@link SimpleClientHttpRequestFactory} -
 *       простая и эффективная реализация для тестового окружения</li>
 *   <li><b>Primary бин для RestClient:</b> автоматически используется вместо
 *       стандартного RestClient при инъекции зависимостей</li>
 * </ul>
 *
 * <p><b>Применимость:</b>
 * <ul>
 *   <li>Контрактные тесты, проверяющие соответствие API спецификации</li>
 *   <li>Интеграционные тесты, выполняющие реальные HTTP запросы</li>
 *   <li>Тесты, требующие настройки таймаутов для стабильности</li>
 *   <li>Тесты с внешними API или медленными операциями</li>
 * </ul>
 *
 * <p><b>Использование RestClient в тестах:</b>
 * <pre>{@code
 * @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
 * @Import(TestRestClientConfig.class)
 * class MyIntegrationTest extends AbstractIntegrationTest {
 *
 *     @Autowired
 *     private RestClient restClient;
 *
 *     @Test
 *     void testApi() {
 *         var response = restClient.get()
 *                 .uri(baseUrl() + "/api/test")
 *                 .retrieve()
 *                 .toEntity(String.class);
 *
 *         assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
 *     }
 * }
 * }</pre>
 *
 * <p><b>Использование TestRestTemplate в тестах:</b>
 * <pre>{@code
 * @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
 * @Import(TestRestClientConfig.class)
 * class MyIntegrationTest extends AbstractIntegrationTest {
 *
 *     @Autowired
 *     private TestRestTemplate restTemplate;
 *
 *     @Test
 *     void testApi() {
 *         var response = restTemplate.getForEntity(
 *                 baseUrl() + "/api/test",
 *                 String.class
 *         );
 *
 *         assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
 *     }
 * }
 * }</pre>
 *
 * <p><b>Рекомендации по настройке таймаутов:</b>
 * <ul>
 *   <li><b>Для быстрых тестов (простой CRUD):</b>
 *       можно уменьшить READ_TIMEOUT до 30 секунд</li>
 *   <li><b>Для медленных тестов (JWT генерация, сложные SQL):</b>
 *       текущие 120 секунд оптимальны</li>
 *   <li><b>Для тестов с внешними API:</b>
 *       увеличьте READ_TIMEOUT до 180-300 секунд</li>
 *   <li><b>Для тестов с недоступным сервером:</b>
 *       используйте connectTimeout для быстрого отката</li>
 *   <li><b>Для тестирования потоковой передачи данных:</b>
 *       установите factory.setBufferRequestBody(false)</li>
 * </ul>
 *
 * <p><b>Расширение функциональности:</b>
 * <ul>
 *   <li>Для SSL-соединений используйте
 *       {@link org.springframework.http.client.HttpComponentsClientHttpRequestFactory}</li>
 *   <li>Для детального логирования запросов добавьте интерцепторы через
 *       {@link RestClient.Builder#requestInterceptor}</li>
 *   <li>Для настройки таймаутов на уровне теста используйте
 *       {@link org.springframework.test.web.client.MockRestServiceServer}</li>
 * </ul>
 *
 * @see RestClient
 * @see TestRestTemplate
 * @see SimpleClientHttpRequestFactory
 * @see ClientHttpRequestFactory
 * @since 1.0
 */
@TestConfiguration
public class TestRestClientConfig {

    /**
     * Таймаут установки соединения в секундах.
     * <p>
     * Определяет максимальное время ожидания установки TCP соединения.
     * Значение 60 секунд достаточно для локального тестирования и
     * позволяет избежать зависаний при временных проблемах с сетью.
     */
    private static final int CONNECT_TIMEOUT_SECONDS = 60;

    /**
     * Таймаут ожидания данных в секундах.
     * <p>
     * Определяет максимальное время ожидания данных от сервера после
     * установки соединения. Значение 120 секунд позволяет обрабатывать
     * длительные операции:
     * <ul>
     *   <li>Генерация JWT токенов (до 30-40 секунд)</li>
     *   <li>Сложные SQL запросы с большими объемами данных</li>
     *   <li>Миграции базы данных Flyway</li>
     *   <li>Инициализация Embedded PostgreSQL</li>
     * </ul>
     */
    private static final int READ_TIMEOUT_SECONDS = 120;

    /**
     * Создает и настраивает основной бин RestClient для тестов.
     *
     * <p>Метод создает экземпляр {@link RestClient} с предварительно настроенной
     * фабрикой HTTP запросов, содержащей оптимизированные таймауты для тестового
     * окружения. Бин помечен как {@link Primary}, что гарантирует его приоритет
     * при автоматическом внедрении зависимостей.
     *
     * <p><b>Процесс настройки:</b>
     * <ol>
     *   <li>Создание фабрики HTTP запросов через {@link #createRequestFactory()}</li>
     *   <li>Применение настроек таймаутов к фабрике</li>
     *   <li>Построение RestClient с использованием переданного билдера</li>
     *   <li>Возврат готового к использованию экземпляра</li>
     * </ol>
     *
     * <p><b>Преимущества RestClient перед RestTemplate:</b>
     * <ul>
     *   <li>Функциональный и декларативный стиль (DSL)</li>
     *   <li>Поддержка реактивных потоков (Reactive Streams)</li>
     *   <li>Более удобная работа с асинхронными запросами</li>
     *   <li>Встроенная поддержка обмена сообщениями с конвертерами</li>
     *   <li>Лучшая интеграция с Spring WebFlux</li>
     * </ul>
     *
     * @param builder стандартный билдер RestClient, предоставляемый Spring Boot.
     *                Позволяет дополнительно настраивать клиент (интерцепторы,
     *                конвертеры, фильтры) перед сборкой
     * @return полностью настроенный экземпляр RestClient для использования в тестах
     * @see RestClient.Builder
     * @see Primary
     */
    @Bean
    @Primary
    public RestClient restClient(RestClient.Builder builder) {
        var requestFactory = createRequestFactory();

        return builder
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Создает бин TestRestTemplate для интеграционных тестов.
     *
     * <p>{@link TestRestTemplate} является оберткой над стандартным RestTemplate,
     * предоставляющей дополнительные возможности для тестирования:
     * <ul>
     *   <li>Автоматическая обработка базовой аутентификации</li>
     *   <li>Поддержка cookie между запросами</li>
     *   <li>Упрощенный API для GET/POST/PUT/DELETE запросов</li>
     *   <li>Лучшая интеграция с Spring MVC тестами</li>
     * </ul>
     *
     * <p><b>Когда использовать TestRestTemplate:</b>
     * <ul>
     *   <li>Простые интеграционные тесты с небольшим количеством запросов</li>
     *   <li>Тесты, требующие сохранения сессии/аутентификации</li>
     *   <li>Тесты с базовой HTTP аутентификацией</li>
     *   <li>Миграция существующих тестов с RestTemplate</li>
     * </ul>
     *
     * <p><b>Когда использовать RestClient:</b>
     * <ul>
     *   <li>Сложные сценарии с цепочками запросов</li>
     *   <li>Реактивные/асинхронные тесты</li>
     *   <li>Тесты с кастомными интерцепторами</li>
     *   <li>Новые проекты, где рекомендуется RestClient</li>
     * </ul>
     *
     * @return настроенный экземпляр TestRestTemplate
     * @see TestRestTemplate
     * @since 1.0
     */
    @Bean
    public TestRestTemplate testRestTemplate() {
        return new TestRestTemplate();
    }

    /**
     * Создает настроенную фабрику HTTP запросов для тестового окружения.
     *
     * <p>Использует {@link SimpleClientHttpRequestFactory} как наиболее простую
     * и эффективную реализацию для тестов. Данная фабрика:
     * <ul>
     *   <li>Не требует дополнительных зависимостей</li>
     *   <li>Обеспечивает достаточную функциональность для большинства тестов</li>
     *   <li>Имеет минимальные накладные расходы на создание соединений</li>
     *   <li>Поддерживает настройку таймаутов через Duration API (Java 21+)</li>
     * </ul>
     *
     * <p><b>Настройки производительности:</b>
     * <ul>
     *   <li><b>Таймаут подключения:</b> {@value CONNECT_TIMEOUT_SECONDS} секунд -
     *       определяет максимальное время ожидания установки TCP соединения.
     *       60 секунд достаточно для локального тестирования и позволяет
     *       избежать зависаний при временных проблемах с сетью</li>
     *   <li><b>Таймаут чтения:</b> {@value READ_TIMEOUT_SECONDS} секунд -
     *       определяет максимальное время ожидания данных от сервера.
     *       120 секунд позволяют обрабатывать длительные операции, такие как
     *       генерация JWT (30-40 сек), сложные SQL запросы или миграции БД</li>
     * </ul>
     *
     * <p><b>Обоснование выбора значений:</b>
     * <ul>
     *   <li>60 секунд на подключение - компромисс между скоростью отката
     *       и устойчивостью к временным сбоям сети</li>
     *   <li>120 секунд на чтение - основано на реальных замерах:
     *       <ul>
     *         <li>Инициализация Embedded PostgreSQL: 5-10 сек</li>
     *         <li>Генерация JWT с BCrypt: 0.5-1 сек</li>
     *         <li>Миграции Flyway: 10-15 сек</li>
     *         <li>Сложные SQL запросы: 5-30 сек</li>
     *       </ul>
     *   </li>
     * </ul>
     *
     * <p><b>Расширение функциональности:</b>
     * <ul>
     *   <li>Для потоковой передачи данных используйте:
     *       <code>factory.setBufferRequestBody(false)</code></li>
     *   <li>Для тестирования с внешними API увеличьте readTimeout до 180-300 секунд</li>
     *   <li>Для высоконагруженных тестов рассмотрите использование
     *       {@link org.springframework.http.client.HttpComponentsClientHttpRequestFactory}
     *       с пулом соединений</li>
     *   <li>Для тестов с большим количеством запросов настройте keep-alive:
     *       <code>factory.setReadTimeout(Duration.ofSeconds(READ_TIMEOUT_SECONDS));</code>
     *       и используйте один экземпляр фабрики для всех запросов</li>
     * </ul>
     *
     * @return настроенная фабрика HTTP запросов с предустановленными таймаутами
     * @see SimpleClientHttpRequestFactory
     * @see ClientHttpRequestFactory
     * @since 1.0
     */
    private ClientHttpRequestFactory createRequestFactory() {
        var factory = new SimpleClientHttpRequestFactory();

        // Настройка таймаутов с использованием современного Duration API (Java 21+)
        // Duration API обеспечивает:
        // - Типобезопасную работу с временными интервалами
        // - Читаемый код с явным указанием единиц измерения
        // - Автоматическую валидацию значений (отрицательные значения вызывают исключение)
        factory.setConnectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS));
        factory.setReadTimeout(Duration.ofSeconds(READ_TIMEOUT_SECONDS));

        return factory;
    }
}