package br.com.artheus.queuelive.controller;

import br.com.artheus.queuelive.dto.queue.QueueRequest;
import br.com.artheus.queuelive.dto.queue.QueueResponse;
import br.com.artheus.queuelive.entity.User;
import br.com.artheus.queuelive.service.QueueService;
import br.com.artheus.queuelive.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/queues")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<QueueResponse> create(
            @Valid @RequestBody QueueRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        User user = userService.syncUser(jwt);
        return ResponseEntity.ok(queueService.create(request, user));
    }

    @GetMapping
    public ResponseEntity<List<QueueResponse>> findAll() {
        return ResponseEntity.ok(queueService.findAllOpen());
    }

    @GetMapping("/{id}")
    public ResponseEntity<QueueResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(queueService.findByIdAsResponse(id));
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<QueueResponse> close(@PathVariable Long id) {
        return ResponseEntity.ok(queueService.close(id));
    }
}