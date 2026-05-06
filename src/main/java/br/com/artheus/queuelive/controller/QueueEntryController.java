package br.com.artheus.queuelive.controller;

import br.com.artheus.queuelive.dto.queue.QueueEntryResponse;
import br.com.artheus.queuelive.entity.User;
import br.com.artheus.queuelive.service.QueueEntryService;
import br.com.artheus.queuelive.service.UserService;
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
public class QueueEntryController {

    private final QueueEntryService queueEntryService;
    private final UserService userService;

    @PostMapping("/{id}/join")
    public ResponseEntity<QueueEntryResponse> join(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        User user = userService.syncUser(jwt);
        return ResponseEntity.ok(queueEntryService.join(id, user));
    }

    @GetMapping("/{id}/entries")
    public ResponseEntity<List<QueueEntryResponse>> entries(@PathVariable UUID id) {
        return ResponseEntity.ok(queueEntryService.findAllByQueue(id));
    }

    @PostMapping("/{id}/next")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<QueueEntryResponse> callNext(@PathVariable UUID id) {
        return ResponseEntity.ok(queueEntryService.callNext(id));
    }
}