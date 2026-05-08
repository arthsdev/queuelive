package br.com.artheus.queuelive.service;

import br.com.artheus.queuelive.dto.user.UserResponse;
import br.com.artheus.queuelive.entity.User;
import br.com.artheus.queuelive.enums.Role;
import br.com.artheus.queuelive.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User syncUser(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        String role = extractRole(jwt);

        return userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .email(email)
                                .name(name)
                                .password("KEYCLOAK_MANAGED")
                                .role(Role.valueOf(role))
                                .build()
                ));
    }

    private String extractRole(Jwt jwt) {
        var realmAccess = jwt.getClaimAsMap("realm_access");

        if (realmAccess != null && realmAccess.get("roles") instanceof List<?> roles) {
            // Filters only application roles, ignoring Keycloak internal roles
            return roles.stream()
                    .map(Object::toString)
                    .filter(r -> r.equals("STAFF") || r.equals("CLIENT"))
                    .findFirst()
                    .orElse("CLIENT");
        }

        return "CLIENT";
    }

    @Transactional(readOnly = true)
    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}