package br.com.artheus.queuelive.dto.queue;

import java.util.List;

public record QueueEventPayload(
        String type,
        Long queueId,
        List<QueueEntryResponse> entries
) {}