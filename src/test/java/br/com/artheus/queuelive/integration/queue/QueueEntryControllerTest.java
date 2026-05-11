package br.com.artheus.queuelive.integration.queue;

import br.com.artheus.queuelive.dto.queue.QueueEntryResponse;
import br.com.artheus.queuelive.dto.queue.QueueRequest;
import br.com.artheus.queuelive.dto.queue.QueueResponse;
import br.com.artheus.queuelive.entity.User;
import br.com.artheus.queuelive.enums.EntryStatus;
import br.com.artheus.queuelive.enums.Role;
import br.com.artheus.queuelive.integration.BaseIntegrationTest;
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

@DisplayName("QueueEntryController")
class QueueEntryControllerTest extends BaseIntegrationTest {

    @MockitoBean
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private User clientUser;
    private User staffUser;
    private UUID queueId;

    @BeforeEach
    protected void setUp() {
        super.setUp();

        staffUser = userRepository.save(User.builder()
                .name("Staff User")
                .email("staff@queuelive.com")
                .password("password")
                .role(Role.STAFF)
                .build());

        clientUser = userRepository.save(User.builder()
                .name("Client User")
                .email("client@queuelive.com")
                .password("password")
                .role(Role.CLIENT)
                .build());

        // mock do staff antes de criar a fila
        when(userService.syncUser(any(Jwt.class))).thenReturn(staffUser);

        QueueResponse queue = webTestClient.post().uri("/queues")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + STAFF_TOKEN)
                .bodyValue(new QueueRequest("Fila Test"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(QueueResponse.class)
                .returnResult().getResponseBody();

        assertThat(queue).isNotNull();
        queueId = queue.id();

        // depois da criação da fila, muda o mock para clientUser
        when(userService.syncUser(any(Jwt.class))).thenReturn(clientUser);
    }

    private WebTestClient.RequestHeadersSpec<?> joinQueue(UUID id) {
        return webTestClient.post().uri("/queues/" + id + "/join")
                .contentType(MediaType.APPLICATION_JSON);
    }

    private WebTestClient.RequestHeadersSpec<?> getEntries(UUID id) {
        return webTestClient.get().uri("/queues/" + id + "/entries");
    }

    private WebTestClient.RequestHeadersSpec<?> callNext(UUID id) {
        return webTestClient.post().uri("/queues/" + id + "/next")
                .contentType(MediaType.APPLICATION_JSON);
    }

    private void joinAsClient() {
        joinQueue(queueId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + NO_ROLE_TOKEN)
                .exchange()
                .expectStatus().isOk();
    }

    @Nested
    @DisplayName("POST /queues/{id}/join")
    class JoinQueue {

        @Test
        @DisplayName("deve entrar na fila como CLIENT")
        void shouldJoinQueueAsClient() {
            joinQueue(queueId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + NO_ROLE_TOKEN)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(QueueEntryResponse.class)
                    .value(response -> {
                        assertThat(response).isNotNull();
                        assertThat(response.username()).isEqualTo("Client User");
                        assertThat(response.position()).isEqualTo(1);
                        assertThat(response.status()).isEqualTo(EntryStatus.WAITING);
                    });
        }

        @Test
        @DisplayName("deve retornar 403 quando STAFF tenta entrar na fila")
        void shouldReturn403WhenStaffJoinsQueue() {
            when(userService.syncUser(any(Jwt.class))).thenReturn(staffUser);

            joinQueue(queueId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + STAFF_TOKEN)
                    .exchange()
                    .expectStatus().isForbidden();
        }

        @Test
        @DisplayName("deve retornar 401 sem token")
        void shouldReturn401WhenNoToken() {
            joinQueue(queueId)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("deve retornar 404 quando fila não existe")
        void shouldReturn404WhenQueueNotFound() {
            joinQueue(UUID.randomUUID())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + NO_ROLE_TOKEN)
                    .exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        @DisplayName("deve retornar 409 quando usuário já está na fila")
        void shouldReturn409WhenUserAlreadyInQueue() {
            joinAsClient();

            joinQueue(queueId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + NO_ROLE_TOKEN)
                    .exchange()
                    .expectStatus().isEqualTo(409);
        }

        @Test
        @DisplayName("deve retornar 409 quando fila está fechada")
        void shouldReturn409WhenQueueIsClosed() {
            // fecha a fila
            when(userService.syncUser(any(Jwt.class))).thenReturn(staffUser);
            webTestClient.patch().uri("/queues/" + queueId + "/close")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + STAFF_TOKEN)
                    .exchange()
                    .expectStatus().isOk();

            when(userService.syncUser(any(Jwt.class))).thenReturn(clientUser);
            joinQueue(queueId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + NO_ROLE_TOKEN)
                    .exchange()
                    .expectStatus().isEqualTo(409);
        }
    }

    @Nested
    @DisplayName("GET /queues/{id}/entries")
    class GetEntries {

        @Test
        @DisplayName("deve retornar entradas da fila")
        void shouldReturnQueueEntries() {
            joinAsClient();

            getEntries(queueId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + NO_ROLE_TOKEN)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBodyList(QueueEntryResponse.class)
                    .value(entries -> {
                        assertThat(entries).hasSize(1);
                        assertThat(entries.get(0).username()).isEqualTo("Client User");
                        assertThat(entries.get(0).position()).isEqualTo(1);
                    });
        }

        @Test
        @DisplayName("deve retornar lista vazia quando não há entradas")
        void shouldReturnEmptyWhenNoEntries() {
            getEntries(queueId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + NO_ROLE_TOKEN)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBodyList(QueueEntryResponse.class)
                    .value(entries -> assertThat(entries).isEmpty());
        }

        @Test
        @DisplayName("deve retornar 401 sem token")
        void shouldReturn401WhenNoToken() {
            getEntries(queueId)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("deve retornar lista vazia quando fila não existe")
        void shouldReturn200EmptyWhenQueueNotFound() {
            getEntries(UUID.randomUUID())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + NO_ROLE_TOKEN)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBodyList(QueueEntryResponse.class)
                    .value(entries -> assertThat(entries).isEmpty());
        }

        @Nested
        @DisplayName("POST /queues/{id}/next")
        class CallNext {

            @Test
            @DisplayName("deve chamar próximo usuário quando STAFF")
            void shouldCallNextWhenStaff() {
                joinAsClient();

                when(userService.syncUser(any(Jwt.class))).thenReturn(staffUser);

                callNext(queueId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + STAFF_TOKEN)
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody(QueueEntryResponse.class)
                        .value(response -> {
                            assertThat(response).isNotNull();
                            assertThat(response.username()).isEqualTo("Client User");
                            assertThat(response.status()).isEqualTo(EntryStatus.CALLED);
                        });
            }

            @Test
            @DisplayName("deve retornar 403 sem role STAFF")
            void shouldReturn403WhenNotStaff() {
                callNext(queueId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + USER_TOKEN)
                        .exchange()
                        .expectStatus().isForbidden();
            }

            @Test
            @DisplayName("deve retornar 401 sem token")
            void shouldReturn401WhenNoToken() {
                callNext(queueId)
                        .exchange()
                        .expectStatus().isUnauthorized();
            }

            @Test
            @DisplayName("deve retornar 404 quando não há usuários esperando")
            void shouldReturn404WhenNoUsersWaiting() {
                when(userService.syncUser(any(Jwt.class))).thenReturn(staffUser);

                callNext(queueId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + STAFF_TOKEN)
                        .exchange()
                        .expectStatus().isNotFound();
            }

            @Test
            @DisplayName("deve retornar 409 quando fila está fechada")
            void shouldReturn409WhenQueueIsClosed() {
                when(userService.syncUser(any(Jwt.class))).thenReturn(staffUser);

                webTestClient.patch().uri("/queues/" + queueId + "/close")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + STAFF_TOKEN)
                        .exchange()
                        .expectStatus().isOk();

                callNext(queueId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + STAFF_TOKEN)
                        .exchange()
                        .expectStatus().isEqualTo(409);
            }
        }
    }
}