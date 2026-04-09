package com.carsensor.gateway.component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.carsensor.gateway.GatewayApplication;
import com.carsensor.gateway.config.TestGatewayConfig;
import com.carsensor.gateway.infrastructure.config.JwtProperties;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@Import(TestGatewayConfig.class)
@DisplayName("Компонентные тесты Gateway")
@SpringBootTest(classes = GatewayApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.cloud.compatibility-verifier.enabled=false",
                "spring.main.allow-bean-definition-overriding=true"})
class GatewayComponentTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtProperties jwtProperties;

    private String validJwtToken;

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void registerWireMockProperties(DynamicPropertyRegistry registry) {
        registry.add("services.auth.url", () -> "http://localhost:" + wireMock.getPort());
        registry.add("services.car.url", () -> "http://localhost:" + wireMock.getPort());
        registry.add("spring.cloud.compatibility-verifier.enabled", () -> "false");
    }

    @BeforeEach
    void setUp() {
        validJwtToken = generateValidJwtToken();
        wireMock.resetAll();
    }

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    @DisplayName("Должен перенаправлять запросы на auth-service для публичных эндпоинтов")
    void shouldRouteToAuthServiceForPublicEndpoints() {
        // Given
        wireMock.stubFor(post(urlEqualTo("/api/v1/auth/login"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"token\":\"" + validJwtToken + "\"}")));

        // Создаем тело запроса
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(
                Map.of("username", "test", "password", "test"),
                headers
        );

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
                getBaseUrl() + "/api/v1/auth/login",
                requestEntity,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        wireMock.verify(1, postRequestedFor(urlEqualTo("/api/v1/auth/login")));
    }

    @Test
    @DisplayName("Должен пропускать запросы с валидным JWT токеном")
    void shouldAllowRequestsWithValidJwtToken() {
        wireMock.stubFor(get(urlEqualTo("/api/v1/cars"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"id\":1,\"model\":\"Tesla\"}]")));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(validJwtToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                getBaseUrl() + "/api/v1/cars",
                HttpMethod.GET,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Tesla");
        wireMock.verify(1, getRequestedFor(urlEqualTo("/api/v1/cars")));
    }

    @Test
    @DisplayName("Должен отклонять запросы с невалидным JWT токеном")
    void shouldRejectRequestsWithInvalidJwtToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("invalid-token");
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                getBaseUrl() + "/api/v1/cars",
                HttpMethod.GET,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        wireMock.verify(0, getRequestedFor(urlEqualTo("/api/v1/cars")));
    }

    @Test
    @DisplayName("Должен пробрасывать заголовки пользователя после аутентификации")
    void shouldPropagateUserHeadersAfterAuthentication() {
        wireMock.stubFor(get(urlEqualTo("/api/v1/cars"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(validJwtToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                getBaseUrl() + "/api/v1/cars",
                HttpMethod.GET,
                entity,
                String.class
        );

        // Проверяем, что запрос прошел
        assertThat(response.getStatusCode())
                .withFailMessage("Expected 200 OK, but got %s. Check if JWT filter is working.", response.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        wireMock.verify(getRequestedFor(urlEqualTo("/api/v1/cars"))
                .withHeader("X-User-Id", WireMock.equalTo("testuser"))
                .withHeader("X-User-Roles", WireMock.containing("USER")));
    }

    @Test
    @DisplayName("Должен корректно переписывать пути для actuator эндпоинтов")
    void shouldRewritePathsForActuatorEndpoints() {
        wireMock.stubFor(get(urlEqualTo("/actuator/health"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"UP\"}")));

        ResponseEntity<String> response = restTemplate.getForEntity(
                getBaseUrl() + "/actuator/auth/health",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
        wireMock.verify(1, getRequestedFor(urlEqualTo("/actuator/health")));
    }

    @Test
    @DisplayName("Должен корректно переписывать пути для actuator эндпоинтов car-service")
    void shouldRewritePathsForCarActuatorEndpoints() {
        wireMock.stubFor(get(urlEqualTo("/actuator/health"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"UP\"}")));

        ResponseEntity<String> response = restTemplate.getForEntity(
                getBaseUrl() + "/actuator/car/health",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        wireMock.verify(1, getRequestedFor(urlEqualTo("/actuator/health")));
    }

    @Test
    @DisplayName("Должен корректно переписывать пути для swagger эндпоинтов")
    void shouldRewritePathsForSwaggerEndpoints() {
        wireMock.stubFor(get(urlEqualTo("/swagger-ui/index.html"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody("<html>Swagger UI</html>")));

        ResponseEntity<String> response = restTemplate.getForEntity(
                getBaseUrl() + "/swagger/auth/index.html",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Swagger UI");
        wireMock.verify(1, getRequestedFor(urlEqualTo("/swagger-ui/index.html")));
    }

    @Test
    @DisplayName("Должен корректно переписывать пути для swagger эндпоинтов car-service")
    void shouldRewritePathsForCarSwaggerEndpoints() {
        wireMock.stubFor(get(urlEqualTo("/swagger-ui/index.html"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("<html>Swagger UI</html>")));

        ResponseEntity<String> response = restTemplate.getForEntity(
                getBaseUrl() + "/swagger/car/index.html",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        wireMock.verify(1, getRequestedFor(urlEqualTo("/swagger-ui/index.html")));
    }

    @Test
    @DisplayName("Должен обрабатывать ошибки сервисов")
    void shouldHandleServiceErrors() {
        wireMock.stubFor(get(urlEqualTo("/api/v1/cars"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Internal Server Error\"}")));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(validJwtToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                getBaseUrl() + "/api/v1/cars",
                HttpMethod.GET,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        wireMock.verify(1, getRequestedFor(urlEqualTo("/api/v1/cars")));
    }

    @Test
    @DisplayName("Должен обрабатывать 404 ошибки сервисов")
    void shouldHandleNotFoundError() {
        wireMock.stubFor(get(urlEqualTo("/api/v1/cars/999"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("{\"error\":\"Not Found\"}")));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(validJwtToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                getBaseUrl() + "/api/v1/cars/999",
                HttpMethod.GET,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        wireMock.verify(1, getRequestedFor(urlEqualTo("/api/v1/cars/999")));
    }

    @Test
    @DisplayName("Должен обрабатывать 400 ошибки сервисов")
    void shouldHandleBadRequestError() {
        // Given
        wireMock.stubFor(post(urlEqualTo("/api/v1/cars"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Bad Request\"}")));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(validJwtToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{\"invalid\":\"data\"}", headers);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                getBaseUrl() + "/api/v1/cars",
                HttpMethod.POST,
                entity,
                String.class
        );

        // Then
        // Проверяем, что ответ не 403 (проблема с авторизацией)
        if (response.getStatusCode() == HttpStatus.FORBIDDEN) {
            System.err.println("Got 403 FORBIDDEN - check JWT token roles. Required role might be ADMIN for POST.");
        }

        assertThat(response.getStatusCode())
                .withFailMessage("Expected 400 BAD_REQUEST, but got %s. Check JWT token and roles.", response.getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        wireMock.verify(1, postRequestedFor(urlEqualTo("/api/v1/cars")));
    }

    @Test
    @DisplayName("Должен поддерживать разные HTTP методы")
    void shouldSupportDifferentHttpMethods() {
        wireMock.stubFor(put(urlEqualTo("/api/v1/cars/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"updated\":true}")));

        wireMock.stubFor(delete(urlEqualTo("/api/v1/cars/1"))
                .willReturn(aResponse()
                        .withStatus(204)));

        wireMock.stubFor(post(urlEqualTo("/api/v1/cars"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Location", "/api/v1/cars/2")
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":2}")));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(validJwtToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> putResponse = restTemplate.exchange(
                getBaseUrl() + "/api/v1/cars/1",
                HttpMethod.PUT,
                entity,
                String.class
        );
        assertThat(putResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        wireMock.verify(1, putRequestedFor(urlEqualTo("/api/v1/cars/1")));

        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                getBaseUrl() + "/api/v1/cars/1",
                HttpMethod.DELETE,
                entity,
                String.class
        );
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        wireMock.verify(1, deleteRequestedFor(urlEqualTo("/api/v1/cars/1")));

        HttpEntity<String> postEntity = new HttpEntity<>("{\"model\":\"New Car\"}", headers);
        ResponseEntity<String> postResponse = restTemplate.exchange(
                getBaseUrl() + "/api/v1/cars",
                HttpMethod.POST,
                postEntity,
                String.class
        );
        assertThat(postResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        wireMock.verify(1, postRequestedFor(urlEqualTo("/api/v1/cars")));
    }

    @Test
    @DisplayName("Должен корректно обрабатывать пустой ответ от сервиса")
    void shouldHandleEmptyResponse() {
        wireMock.stubFor(get(urlEqualTo("/api/v1/cars"))
                .willReturn(aResponse()
                        .withStatus(204)
                        .withBody("")));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(validJwtToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                getBaseUrl() + "/api/v1/cars",
                HttpMethod.GET,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        wireMock.verify(1, getRequestedFor(urlEqualTo("/api/v1/cars")));
    }

    private String generateValidJwtToken() {
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject("testuser")
                .claim("roles", List.of("USER", "ADMIN"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.accessTokenExpiration() * 1000))
                .signWith(key)
                .compact();
    }
}