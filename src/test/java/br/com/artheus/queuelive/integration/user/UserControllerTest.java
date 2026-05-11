package br.com.artheus.queuelive.integration.user;

import br.com.artheus.queuelive.dto.user.UserResponse;
import br.com.artheus.queuelive.enums.Role;
import br.com.artheus.queuelive.integration.BaseIntegrationTest;
import br.com.artheus.queuelive.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserController")
class UserControllerTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    protected void setUp() {
        super.setUp();
    }

    @Nested
    @DisplayName("GET /users/me")
    class GetMe {

        @Test
        @DisplayName("should return authenticated user as STAFF")
        void shouldReturnAuthenticatedStaffUser() {
            webTestClient.get().uri("/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + STAFF_TOKEN)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(UserResponse.class)
                    .value(response -> {
                        assertThat(response).isNotNull();
                        assertThat(response.name()).isEqualTo("Staff User");
                        assertThat(response.email()).isEqualTo("staff@queuelive.com");
                        assertThat(response.role()).isEqualTo(Role.STAFF);
                    });
        }

        @Test
        @DisplayName("should return authenticated user as CLIENT")
        void shouldReturnAuthenticatedClientUser() {
            webTestClient.get().uri("/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + USER_TOKEN)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(UserResponse.class)
                    .value(response -> {
                        assertThat(response).isNotNull();
                        assertThat(response.name()).isEqualTo("Regular User");
                        assertThat(response.email()).isEqualTo("user@queuelive.com");
                        assertThat(response.role()).isEqualTo(Role.CLIENT);
                    });
        }

        @Test
        @DisplayName("should create user in database if it does not exist")
        void shouldCreateUserIfNotExists() {
            assertThat(userRepository.findByEmail("staff@queuelive.com")).isEmpty();

            webTestClient.get().uri("/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + STAFF_TOKEN)
                    .exchange()
                    .expectStatus().isOk();

            assertThat(userRepository.findByEmail("staff@queuelive.com")).isPresent();
        }

        @Test
        @DisplayName("should return the same user on subsequent calls")
        void shouldReturnSameUserOnSubsequentCalls() {
            webTestClient.get().uri("/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + STAFF_TOKEN)
                    .exchange()
                    .expectStatus().isOk();

            webTestClient.get().uri("/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + STAFF_TOKEN)
                    .exchange()
                    .expectStatus().isOk();

            assertThat(userRepository.findAll())
                    .filteredOn(u -> u.getEmail().equals("staff@queuelive.com"))
                    .hasSize(1);
        }

        @Test
        @DisplayName("should return 401 when no token is provided")
        void shouldReturn401WhenNoToken() {
            webTestClient.get().uri("/users/me")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }
    }
}