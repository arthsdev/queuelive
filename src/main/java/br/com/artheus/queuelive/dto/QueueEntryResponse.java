package br.com.artheus.queuelive.dto;

import br.com.artheus.queuelive.enums.EntryStatus;

public record QueueEntryResponse(
        Long id,
        String username,
        Integer position,
        EntryStatus status
) {
}
