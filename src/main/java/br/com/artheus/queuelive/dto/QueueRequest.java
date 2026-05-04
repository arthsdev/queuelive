package br.com.artheus.queuelive.dto;

import jakarta.validation.constraints.NotBlank;

public record QueueRequest(
        @NotBlank(message = "Name is required.")
        String name
) {
}
