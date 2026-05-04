package br.com.artheus.queuelive.dto;

import br.com.artheus.queuelive.enums.QueueStatus;

import java.time.LocalDateTime;

public record QueueResponse(
        Long id,
        String name,
        QueueStatus status,
        String createdBy,
        LocalDateTime createdAt
) {}