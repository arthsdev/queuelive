package br.com.artheus.queuelive.controller;

import br.com.artheus.queuelive.dto.common.ErrorResponse;
import br.com.artheus.queuelive.dto.queue.QueueRequest;
import br.com.artheus.queuelive.dto.queue.QueueResponse;
import br.com.artheus.queuelive.entity.User;
import br.com.artheus.queuelive.service.QueueService;
import br.com.artheus.queuelive.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/queues")
@RequiredArgsConstructor
@Tag(name = "Queues", description = "Queue management endpoints")
public class QueueController {

    private final QueueService queueService;
    private final UserService userService;

    @Operation(summary = "Create a new queue", description = "Only STAFF members can create queues")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Queue created successfully",
                    content = @Content(schema = @Schema(implementation = QueueResponse.class))),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized — invalid or missing token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied — STAFF role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<QueueResponse> create(
            @Valid @RequestBody QueueRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        User user = userService.syncUser(jwt);
        return ResponseEntity.ok(queueService.create(request, user));
    }

    @Operation(summary = "List all open queues")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Queues returned successfully",
                    content = @Content(schema = @Schema(implementation = QueueResponse.class))),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized — invalid or missing token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<QueueResponse>> findAll() {
        return ResponseEntity.ok(queueService.findAllOpen());
    }

    @Operation(summary = "Find a queue by ID")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Queue returned successfully",
                    content = @Content(schema = @Schema(implementation = QueueResponse.class))),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized — invalid or missing token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Queue not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<QueueResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(queueService.findByIdAsResponse(id));
    }

    @Operation(summary = "Close a queue", description = "Only STAFF members can close queues")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Queue closed successfully",
                    content = @Content(schema = @Schema(implementation = QueueResponse.class))),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized — invalid or missing token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied — STAFF role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Queue not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "409",
                    description = "Queue is already closed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}/close")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<QueueResponse> close(@PathVariable UUID id) {
        return ResponseEntity.ok(queueService.close(id));
    }
}