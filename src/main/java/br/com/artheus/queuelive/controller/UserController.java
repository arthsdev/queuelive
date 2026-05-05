package br.com.artheus.queuelive.controller;

import br.com.artheus.queuelive.entity.User;
import br.com.artheus.queuelive.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<User> me(@AuthenticationPrincipal Jwt jwt) {
        User user = userService.syncUser(jwt);
        return ResponseEntity.ok(user);
    }
}