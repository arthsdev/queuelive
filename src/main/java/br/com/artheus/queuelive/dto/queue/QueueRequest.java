package br.com.artheus.queuelive.dto.queue;

import jakarta.validation.constraints.NotBlank;

public record QueueRequest(
        @NotBlank(message = "Name is required.")
        String name
) {
}
