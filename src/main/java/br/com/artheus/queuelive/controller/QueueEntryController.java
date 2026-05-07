package br.com.artheus.queuelive.controller;

import br.com.artheus.queuelive.dto.common.ErrorResponse;
import br.com.artheus.queuelive.dto.queue.QueueEntryResponse;
import br.com.artheus.queuelive.entity.User;
import br.com.artheus.queuelive.service.QueueEntryService;
import br.com.artheus.queuelive.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Queue Entries", description = "Queue entry management endpoints")
public class QueueEntryController {

    private final QueueEntryService queueEntryService;
    private final UserService userService;

    @Operation(summary = "Join a queue", description = "Only CLIENT members can join queues")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Joined queue successfully",
                    content = @Content(schema = @Schema(implementation = QueueEntryResponse.class))),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized — invalid or missing token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "Staff members cannot join a queue",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Queue not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "409",
                    description = "User is already in this queue",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/join")
    public ResponseEntity<QueueEntryResponse> join(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        User user = userService.syncUser(jwt);
        return ResponseEntity.ok(queueEntryService.join(id, user));
    }

    @Operation(summary = "List all entries in a queue")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Entries returned successfully",
                    content = @Content(schema = @Schema(implementation = QueueEntryResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized — invalid or missing token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Queue not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{id}/entries")
    public ResponseEntity<List<QueueEntryResponse>> entries(@PathVariable UUID id) {
        return ResponseEntity.ok(queueEntryService.findAllByQueue(id));
    }

    @Operation(summary = "Call the next user in queue", description = "Only STAFF members can call the next user")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Next user called successfully",
                    content = @Content(schema = @Schema(implementation = QueueEntryResponse.class))
            ),
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
                    description = "Queue not found or no users waiting",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "409",
                    description = "Queue is closed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/next")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<QueueEntryResponse> callNext(@PathVariable UUID id) {
        return ResponseEntity.ok(queueEntryService.callNext(id));
    }
}