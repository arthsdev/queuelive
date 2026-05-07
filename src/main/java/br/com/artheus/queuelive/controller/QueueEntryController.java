package br.com.artheus.queuelive.controller;

import br.com.artheus.queuelive.dto.queue.QueueEntryResponse;
import br.com.artheus.queuelive.entity.User;
import br.com.artheus.queuelive.service.QueueEntryService;
import br.com.artheus.queuelive.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
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
    @PostMapping("/{id}/join")
    public ResponseEntity<QueueEntryResponse> join(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        User user = userService.syncUser(jwt);
        return ResponseEntity.ok(queueEntryService.join(id, user));
    }

    @Operation(summary = "List all entries in a queue")
    @GetMapping("/{id}/entries")
    public ResponseEntity<List<QueueEntryResponse>> entries(@PathVariable UUID id) {
        return ResponseEntity.ok(queueEntryService.findAllByQueue(id));
    }

    @Operation(summary = "Call the next user in queue", description = "Only STAFF members can call the next user")
    @PostMapping("/{id}/next")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<QueueEntryResponse> callNext(@PathVariable UUID id) {
        return ResponseEntity.ok(queueEntryService.callNext(id));
    }
}