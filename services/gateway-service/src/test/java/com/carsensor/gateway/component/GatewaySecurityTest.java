package com.carsensor.gateway.component;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.carsensor.gateway.GatewayApplication;
import com.carsensor.gateway.config.TestGatewayConfig;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DisplayName("Компонентные тесты Gateway - Security")
@SpringBootTest(classes = GatewayApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.cloud.compatibility-verifier.enabled=false"})
@Import(TestGatewayConfig.class)
class GatewaySecurityTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void registerWireMockProperties(DynamicPropertyRegistry registry) {
        registry.add("services.auth.url", () -> "http://localhost:" + wireMock.getPort());
        registry.add("services.car.url", () -> "http://localhost:" + wireMock.getPort());
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://localhost:" + wireMock.getPort() + "/.well-known/jwks.json");
    }

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    @DisplayName("Должен требовать JWT токен для защищенных эндпоинтов")
    void shouldRequireJwtTokenForProtectedEndpoints() {
        // When - без мока, используется реальный JwtAuthenticationGatewayFilter
        ResponseEntity<String> response = restTemplate.getForEntity(
                getBaseUrl() + "/api/v1/cars",
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        wireMock.verify(0, getRequestedFor(urlEqualTo("/api/v1/cars")));
    }
}