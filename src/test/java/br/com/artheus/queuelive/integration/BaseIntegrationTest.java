package br.com.artheus.queuelive.integration;

import br.com.artheus.queuelive.repository.QueueEntryRepository;
import br.com.artheus.queuelive.repository.QueueRepository;
import br.com.artheus.queuelive.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(BaseIntegrationTest.TestConfig.class)
public abstract class BaseIntegrationTest {

    public static final String STAFF_TOKEN = "staff-token";
    public static final String USER_TOKEN = "user-token";
    public static final String NO_ROLE_TOKEN = "no-role-token";

    @LocalServerPort
    protected int port;

    protected WebTestClient webTestClient;

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    @Autowired
    private QueueRepository queueRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    protected void setUp() {
        this.webTestClient = WebTestClient
                .bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();

        queueEntryRepository.deleteAll();
        queueRepository.deleteAll();
        userRepository.deleteAll();
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        public JwtDecoder jwtDecoder() {
            JwtDecoder decoder = mock(JwtDecoder.class);

            when(decoder.decode(STAFF_TOKEN)).thenReturn(Jwt.withTokenValue(STAFF_TOKEN)
                    .header("alg", "none")
                    .subject("staff-user")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .claim("email", "staff@queuelive.com")
                    .claim("name", "Staff User")
                    .claim("realm_access", Map.of("roles", List.of("STAFF")))
                    .build());

            when(decoder.decode(USER_TOKEN)).thenReturn(Jwt.withTokenValue(USER_TOKEN)
                    .header("alg", "none")
                    .subject("regular-user")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .claim("email", "user@queuelive.com")
                    .claim("name", "Regular User")
                    .claim("realm_access", Map.of("roles", List.of("CLIENT")))
                    .build());

            when(decoder.decode(NO_ROLE_TOKEN)).thenReturn(Jwt.withTokenValue(NO_ROLE_TOKEN)
                    .header("alg", "none")
                    .subject("no-role-user")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .claim("email", "norole@queuelive.com")
                    .claim("name", "No Role User")
                    .build());

            return decoder;
        }
    }
}