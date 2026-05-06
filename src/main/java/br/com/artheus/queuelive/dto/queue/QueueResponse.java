package br.com.artheus.queuelive.dto.queue;

import br.com.artheus.queuelive.enums.QueueStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record QueueResponse(
        UUID id,
        String name,
        QueueStatus status,
        String createdBy,
        LocalDateTime createdAt
) {}