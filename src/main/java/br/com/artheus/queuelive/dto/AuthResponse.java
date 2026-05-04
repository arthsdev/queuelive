package br.com.artheus.queuelive.dto;

public record AuthResponse(
        String token,
        String name,
        String role
) {}
