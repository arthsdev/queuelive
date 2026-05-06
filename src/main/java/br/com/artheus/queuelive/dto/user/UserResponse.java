package br.com.artheus.queuelive.dto.user;

import br.com.artheus.queuelive.enums.Role;
import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        LocalDateTime createdAt
) {}