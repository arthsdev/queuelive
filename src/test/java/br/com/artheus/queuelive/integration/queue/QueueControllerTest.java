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
        @DisplayName("should create queue when authenticated as STAFF")
        void shouldCreateQueueWhenStaff() {
            postQueues()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + STAFF_TOKEN)
                    .bodyValue(new QueueRequest("Counter 1"))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(QueueResponse.class)
                    .value(response -> {
                        assertThat(response).isNotNull();
                        assertThat(response.name()).isEqualTo("Counter 1");
                        assertThat(response.status()).isEqualTo(QueueStatus.OPEN);
                        assertThat(response.createdBy()).isEqualTo("Staff User");
                    });
        }

        @Test
        @DisplayName("should return 403 when user is not STAFF")
        void shouldReturn403WhenNotStaff() {
            postQueues()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + USER_TOKEN)
                    .bodyValue(new QueueRequest("Counter 1"))
                    .exchange()
                    .expectStatus().isForbidden();
        }

        @Test
        @DisplayName("should return 401 when no token is provided")
        void shouldReturn401WhenNoToken() {
            postQueues()
                    .bodyValue(new QueueRequest("Counter 1"))
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("should return 400 when name is blank")
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
        @DisplayName("should return list of open queues")
        void shouldReturnOpenQueues() {
            getQueues()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + NO_ROLE_TOKEN)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBodyList(QueueResponse.class);
        }

        @Test
        @DisplayName("should return 401 when no token is provided")
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
        @DisplayName("should return queue by ID")
        void shouldReturnQueueById() {
            UUID id = createQueue("Screening");

            getQueueById(id)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + NO_ROLE_TOKEN)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(QueueResponse.class)
                    .value(response -> {
                        assertThat(response).isNotNull();
                        assertThat(response.id()).isEqualTo(id);
                        assertThat(response.name()).isEqualTo("Screening");
                    });
        }

        @Test
        @DisplayName("should return 404 when queue does not exist")
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
        @DisplayName("should close queue when authenticated as STAFF")
        void shouldCloseQueueWhenStaff() {
            UUID id = createQueue("Regular Ticket");

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
        @DisplayName("should return 409 when closing an already closed queue")
        void shouldReturn409WhenAlreadyClosed() {
            UUID id = createQueue("Priority Ticket");

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
        @DisplayName("should return 403 when user is not STAFF")
        void shouldReturn403WhenNotStaff() {
            closeQueue(UUID.randomUUID())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + USER_TOKEN)
                    .exchange()
                    .expectStatus().isForbidden();
        }
    }
}