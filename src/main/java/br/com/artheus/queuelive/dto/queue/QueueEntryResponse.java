package br.com.artheus.queuelive.dto.queue;

import br.com.artheus.queuelive.enums.EntryStatus;

import java.util.UUID;

public record QueueEntryResponse(
        UUID id,
        String username,
        Integer position,
        EntryStatus status
) {
}
