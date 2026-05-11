package br.com.artheus.queuelive.integration.queue;

import br.com.artheus.queuelive.dto.queue.QueueRequest;
import br.com.artheus.queuelive.dto.queue.QueueResponse;
import br.com.artheus.queuelive.entity.User;
import br.com.artheus.queuelive.enums.QueueStatus;
import br.com.artheus.queuelive.enums.Role;
import br.com.artheus.queuelive.integration.BaseIntegrationTest;
import br.com.artheus.queuelive.repository.QueueRepository;
import br.com.artheus.queuelive.repository.UserRepository;
import br.com.artheus.queuelive.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("QueueController")
class QueueControllerTest extends BaseIntegrationTest {

    @MockitoBean
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QueueRepository queueRepository;

    private User staffUser;

    @BeforeEach
    protected void setUp() {
        super.setUp();

        staffUser = userRepository.save(User.builder()
                .name("Staff User")
                .email("staff@queuelive.com")
                .password("password")
                .role(Role.STAFF)
                .build());

        when(userService.syncUser(any(Jwt.class))).thenReturn(staffUser);
    }

    private WebTestClient.RequestBodySpec postQueues() {
        return webTestClient.post().uri("/queues")
                .contentType(MediaType.APPLICATION_JSON);
    }

    private WebTestClient.RequestHeadersSpec<?> getQueues() {
        return webTestClient.get().uri("/queues");
    }

    private WebTestClient.RequestHeadersSpec<?> getQueueById(UUID id) {
        return webTestClient.get().uri("/queues/" + id);
    }

    private WebTestClient.RequestHeadersSpec<?> closeQueue(UUID id) {
        return webTestClient.patch().uri("/queues/" + id + "/close");
    }

    private UUID createQueue(String name) {
        QueueResponse response = postQueues()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + STAFF_TOKEN)
                .bodyValue(new QueueRequest(name))
                .exchange()
                .expectStatus().isOk()
                .expectBody(QueueResponse.class)
                .returnResult().getResponseBody();
        assertThat(response).isNotNull();
        return response.id();
    }

    @Nested
    @DisplayName("POST /queues")
    class CreateQueue {

        @Test
        @DisplayName("deve criar fila quando STAFF")
        void shouldCreateQueueWhenStaff() {
            postQueues()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + STAFF_TOKEN)
                    .bodyValue(new QueueRequest("Caixa 1"))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(QueueResponse.class)
                    .value(response -> {
                        assertThat(response).isNotNull();
                        assertThat(response.name()).isEqualTo("Caixa 1");
                        assertThat(response.status()).isEqualTo(QueueStatus.OPEN);
                        assertThat(response.createdBy()).isEqualTo("Staff User");
                    });
        }

        @Test
        @DisplayName("deve retornar 403 sem role STAFF")
        void shouldReturn403WhenNotStaff() {
            postQueues()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + USER_TOKEN)
                    .bodyValue(new QueueRequest("Caixa 1"))
                    .exchange()
                    .expectStatus().isForbidden();
        }

        @Test
        @DisplayName("deve retornar 401 sem token")
        void shouldReturn401WhenNoToken() {
            postQueues()
                    .bodyValue(new QueueRequest("Caixa 1"))
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("deve retornar 400 com nome em branco")
        void shouldReturn400WhenNameIsBlank() {
            postQueues()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + STAFF_TOKEN)
                    .bodyValue(new QueueRequest(""))
                    .exchange()
                    .expectStatus().isBadRequest();
        }
    }

    @Nested
    @DisplayName("GET /queues")
    class FindAllQueues {

        @Test
        @DisplayName("deve retornar lista de filas abertas")
        void shouldReturnOpenQueues() {
            getQueues()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + NO_ROLE_TOKEN)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBodyList(QueueResponse.class);
        }

        @Test
        @DisplayName("deve retornar 401 sem token")
        void shouldReturn401WhenNoToken() {
            getQueues()
                    .exchange()
                    .expectStatus().isUnauthorized();
        }
    }

    @Nested
    @DisplayName("GET /queues/{id}")
    class FindQueueById {

        @Test
        @DisplayName("deve retornar fila por ID")
        void shouldReturnQueueById() {
            UUID id = createQueue("Triagem");

            getQueueById(id)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + NO_ROLE_TOKEN)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(QueueResponse.class)
                    .value(response -> {
                        assertThat(response).isNotNull();
                        assertThat(response.id()).isEqualTo(id);
                        assertThat(response.name()).isEqualTo("Triagem");
                    });
        }

        @Test
        @DisplayName("deve retornar 404 quando fila não existe")
        void shouldReturn404WhenQueueNotFound() {
            getQueueById(UUID.randomUUID())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + NO_ROLE_TOKEN)
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    @Nested
    @DisplayName("PATCH /queues/{id}/close")
    class CloseQueue {

        @Test
        @DisplayName("deve fechar fila quando STAFF")
        void shouldCloseQueueWhenStaff() {
            UUID id = createQueue("Senha Normal");

            closeQueue(id)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + STAFF_TOKEN)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(QueueResponse.class)
                    .value(response -> {
                        assertThat(response).isNotNull();
                        assertThat(response.status()).isEqualTo(QueueStatus.CLOSED);
                    });
        }

        @Test
        @DisplayName("deve retornar 409 ao fechar fila já fechada")
        void shouldReturn409WhenAlreadyClosed() {
            UUID id = createQueue("Senha Prioritária");

            closeQueue(id)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + STAFF_TOKEN)
                    .exchange()
                    .expectStatus().isOk();

            closeQueue(id)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + STAFF_TOKEN)
                    .exchange()
                    .expectStatus().isEqualTo(409);
        }

        @Test
        @DisplayName("deve retornar 403 sem role STAFF")
        void shouldReturn403WhenNotStaff() {
            closeQueue(UUID.randomUUID())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + USER_TOKEN)
                    .exchange()
                    .expectStatus().isForbidden();
        }
    }
}