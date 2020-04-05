package am.ik.lab.syaberu;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Testcontainers
@SpringBootTest(webEnvironment = RANDOM_PORT)
class SyaberuRsocketProxyApplicationTests {
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>();

    @DynamicPropertySource
    static void mySqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> mysql.getJdbcUrl().replace("jdbc:", "r2dbc:"));
        registry.add("spring.r2dbc.username", mysql::getUsername);
        registry.add("spring.r2dbc.password", mysql::getPassword);
    }

    @Test
    void checkHealth(@Autowired WebTestClient webClient) {
        webClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class).isEqualTo("{\"status\":\"UP\"}");
    }
}
