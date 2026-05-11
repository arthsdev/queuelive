package br.com.artheus.queuelive.unit.queue;

import br.com.artheus.queuelive.dto.user.UserResponse;
import br.com.artheus.queuelive.entity.User;
import br.com.artheus.queuelive.enums.Role;
import br.com.artheus.queuelive.repository.UserRepository;
import br.com.artheus.queuelive.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService unit tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    // ---- helpers ----

    private User buildUser(Role role) {
        return User.builder()
                .id(UUID.randomUUID())
                .name("Fabiano Augusto")
                .email("fabiano.augusto@queuelive.com")
                .password("KEYCLOAK_MANAGED")
                .role(role)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Jwt buildJwt(String email, String name, List<String> roles) {
        Map<String, Object> claims = Map.of(
                "email", email,
                "name", name,
                "realm_access", Map.of("roles", roles)
        );

        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claims(c -> c.putAll(claims))
                .build();
    }

    // ---- syncUser ----

    @Test
    @DisplayName("should return existing user when email is already registered")
    void shouldReturnExistingUserWhenEmailAlreadyRegistered() {
        // GIVEN
        User existingUser = buildUser(Role.STAFF);
        Jwt jwt = buildJwt(existingUser.getEmail(), existingUser.getName(), List.of("STAFF"));

        when(userRepository.findByEmail(existingUser.getEmail())).thenReturn(Optional.of(existingUser));

        // WHEN
        User result = userService.syncUser(jwt);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(existingUser.getEmail());
        assertThat(result.getRole()).isEqualTo(Role.STAFF);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("should create and return new user when email is not registered")
    void shouldCreateNewUserWhenEmailNotRegistered() {
        // GIVEN
        User newUser = buildUser(Role.STAFF);
        Jwt jwt = buildJwt("newuser@queuelive.com", "New User", List.of("STAFF"));

        when(userRepository.findByEmail("newuser@queuelive.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // WHEN
        User result = userService.syncUser(jwt);

        // THEN
        assertThat(result).isNotNull();
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("should assign CLIENT role when no matching role is found in realm_access")
    void shouldAssignClientRoleWhenNoMatchingRoleFound() {
        // GIVEN
        User newUser = buildUser(Role.CLIENT);
        Jwt jwt = buildJwt("client@queuelive.com", "Client User", List.of("offline_access", "uma_authorization"));

        when(userRepository.findByEmail("client@queuelive.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // WHEN
        User result = userService.syncUser(jwt);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getRole()).isEqualTo(Role.CLIENT);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("should assign CLIENT role when realm_access claim is absent")
    void shouldAssignClientRoleWhenRealmAccessIsAbsent() {
        // GIVEN
        User newUser = buildUser(Role.CLIENT);

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("email", "norealmaccess@queuelive.com")
                .claim("name", "No Realm User")
                .build();

        when(userRepository.findByEmail("norealmaccess@queuelive.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // WHEN
        User result = userService.syncUser(jwt);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getRole()).isEqualTo(Role.CLIENT);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("should pick STAFF role when multiple roles are present and STAFF comes first")
    void shouldPickStaffRoleWhenMultipleRolesPresent() {
        // GIVEN
        User newUser = buildUser(Role.STAFF);
        Jwt jwt = buildJwt("staff@queuelive.com", "Staff User", List.of("offline_access", "STAFF", "CLIENT"));

        when(userRepository.findByEmail("staff@queuelive.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // WHEN
        User result = userService.syncUser(jwt);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getRole()).isEqualTo(Role.STAFF);
    }

    // ---- toResponse ----

    @Test
    @DisplayName("should map user to response correctly")
    void shouldMapUserToResponseCorrectly() {
        // GIVEN
        User user = buildUser(Role.STAFF);

        // WHEN
        UserResponse response = userService.toResponse(user);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(user.getId());
        assertThat(response.name()).isEqualTo(user.getName());
        assertThat(response.email()).isEqualTo(user.getEmail());
        assertThat(response.role()).isEqualTo(Role.STAFF);
        assertThat(response.createdAt()).isEqualTo(user.getCreatedAt());
    }

    @Test
    @DisplayName("should assign CLIENT role when realm_access roles is not a List")
    void shouldAssignClientRoleWhenRealmAccessRolesIsNotAList() {
        // GIVEN
        User newUser = buildUser(Role.CLIENT);

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("email", "malformed@queuelive.com")
                .claim("name", "Malformed User")
                .claim("realm_access", Map.of("roles", "NOT_A_LIST"))
                .build();

        when(userRepository.findByEmail("malformed@queuelive.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // WHEN
        User result = userService.syncUser(jwt);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getRole()).isEqualTo(Role.CLIENT);
        verify(userRepository, times(1)).save(any(User.class));
    }
}