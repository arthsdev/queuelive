package br.com.artheus.queuelive.dto.queue;

import java.util.List;
import java.util.UUID;

public record QueueEventPayload(
        String type,
        UUID queueId,
        List<QueueEntryResponse> entries
) {}