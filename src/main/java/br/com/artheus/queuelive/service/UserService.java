package br.com.artheus.queuelive.service;

import br.com.artheus.queuelive.dto.UserResponse;
import br.com.artheus.queuelive.entity.User;
import br.com.artheus.queuelive.enums.Role;
import br.com.artheus.queuelive.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

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
        if (realmAccess != null && realmAccess.get("roles") instanceof java.util.List<?> roles) {
            return roles.stream()
                    .map(Object::toString)
                    .filter(r -> r.equals("STAFF") || r.equals("CLIENT"))
                    .findFirst()
                    .orElse("CLIENT");
        }
        return "CLIENT";
    }

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