package br.com.artheus.queuelive.dto.user;

import br.com.artheus.queuelive.enums.Role;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        Role role,
        LocalDateTime createdAt
) {}