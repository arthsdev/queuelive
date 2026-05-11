package br.com.artheus.queuelive.unit.queue;

import br.com.artheus.queuelive.config.QueueEventPublisher;
import br.com.artheus.queuelive.dto.queue.QueueEntryResponse;
import br.com.artheus.queuelive.entity.Queue;
import br.com.artheus.queuelive.entity.QueueEntry;
import br.com.artheus.queuelive.entity.User;
import br.com.artheus.queuelive.enums.EntryStatus;
import br.com.artheus.queuelive.enums.QueueStatus;
import br.com.artheus.queuelive.enums.Role;
import br.com.artheus.queuelive.exception.domain.QueueEntryException;
import br.com.artheus.queuelive.exception.domain.QueueException;
import br.com.artheus.queuelive.repository.QueueEntryRepository;
import br.com.artheus.queuelive.service.QueueEntryService;
import br.com.artheus.queuelive.service.QueueService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QueueEntryService unit tests")
class QueueEntryServiceTest {

    @Mock
    private QueueEntryRepository queueEntryRepository;

    @Mock
    private QueueService queueService;

    @Mock
    private QueueEventPublisher eventPublisher;

    @InjectMocks
    private QueueEntryService queueEntryService;

    // ---- helpers ----

    private User buildClient() {
        return User.builder()
                .id(UUID.randomUUID())
                .name("Client 01")
                .email("client01@queuelive.com")
                .password("KEYCLOAK_MANAGED")
                .role(Role.CLIENT)
                .build();
    }

    private User buildStaff() {
        return User.builder()
                .id(UUID.randomUUID())
                .name("Staff 01")
                .email("staff01@queuelive.com")
                .password("KEYCLOAK_MANAGED")
                .role(Role.STAFF)
                .build();
    }

    private Queue buildOpenQueue() {
        return Queue.builder()
                .id(UUID.randomUUID())
                .name("Fila de Atendimento")
                .status(QueueStatus.OPEN)
                .createdBy(buildStaff())
                .createdAt(LocalDateTime.now())
                .build();
    }

    private QueueEntry buildEntry(Queue queue, User user, int position) {
        return QueueEntry.builder()
                .id(UUID.randomUUID())
                .queue(queue)
                .user(user)
                .position(position)
                .status(EntryStatus.WAITING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("should join queue successfully")
    void shouldJoinQueue() {
        // GIVEN
        User client = buildClient();
        Queue queue = buildOpenQueue();
        QueueEntry entry = buildEntry(queue, client, 1);

        when(queueService.findById(queue.getId())).thenReturn(queue);
        when(queueEntryRepository.existsByQueueIdAndUserId(queue.getId(), client.getId())).thenReturn(false);
        when(queueEntryRepository.countByQueueIdAndStatus(queue.getId(), EntryStatus.WAITING)).thenReturn(0);
        when(queueEntryRepository.save(any(QueueEntry.class))).thenAnswer(invocation -> {
            QueueEntry saved = invocation.getArgument(0);
            // simulates @PrePersist behavior since JPA lifecycle is not triggered in unit tests
            return QueueEntry.builder()
                    .id(UUID.randomUUID())
                    .queue(saved.getQueue())
                    .user(saved.getUser())
                    .position(saved.getPosition())
                    .status(EntryStatus.WAITING)
                    .createdAt(LocalDateTime.now())
                    .build();
        });
        when(queueEntryRepository.findByQueueIdOrderByPositionAsc(queue.getId())).thenReturn(List.of(entry));

        // WHEN
        QueueEntryResponse response = queueEntryService.join(queue.getId(), client);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.position()).isEqualTo(1);
        assertThat(response.status()).isEqualTo(EntryStatus.WAITING);
        verify(queueEntryRepository, times(1)).save(any(QueueEntry.class));
        verify(eventPublisher, times(1)).publishQueueUpdated(any(), any());
    }

    @Test
    @DisplayName("should throw when staff tries to join queue")
    void shouldThrowWhenStaffTriesToJoin() {
        // GIVEN
        User staff = buildStaff();
        Queue queue = buildOpenQueue();

        // WHEN / THEN
        assertThatThrownBy(() -> queueEntryService.join(queue.getId(), staff))
                .isInstanceOf(QueueEntryException.class);

        // verifies no database query was made
        verifyNoInteractions(queueService);
        verifyNoInteractions(queueEntryRepository);
    }

    @Test
    @DisplayName("should throw when queue is closed")
    void shouldThrowWhenQueueIsClosed() {
        // GIVEN
        User client = buildClient();
        Queue queue = buildOpenQueue();
        queue.close();

        when(queueService.findById(queue.getId())).thenReturn(queue);

        // WHEN / THEN
        assertThatThrownBy(() -> queueEntryService.join(queue.getId(), client))
                .isInstanceOf(QueueException.class);
    }

    @Test
    @DisplayName("should throw when user is already in queue")
    void shouldThrowWhenUserAlreadyInQueue() {
        // GIVEN
        User client = buildClient();
        Queue queue = buildOpenQueue();

        when(queueService.findById(queue.getId())).thenReturn(queue);
        when(queueEntryRepository.existsByQueueIdAndUserId(queue.getId(), client.getId())).thenReturn(true);

        // WHEN / THEN
        assertThatThrownBy(() -> queueEntryService.join(queue.getId(), client))
                .isInstanceOf(QueueEntryException.class);
    }

    // ---- callNext ----

    @Test
    @DisplayName("should call next user successfully")
    void shouldCallNextUser() {
        // GIVEN
        User client = buildClient();
        Queue queue = buildOpenQueue();
        QueueEntry entry = buildEntry(queue, client, 1);

        when(queueService.findById(queue.getId())).thenReturn(queue);
        when(queueEntryRepository.findFirstByQueueIdAndStatusOrderByPositionAsc(
                queue.getId(), EntryStatus.WAITING)).thenReturn(Optional.of(entry));
        when(queueEntryRepository.save(any(QueueEntry.class))).thenReturn(entry);
        when(queueEntryRepository.findByQueueIdAndStatus(queue.getId(), EntryStatus.WAITING)).thenReturn(List.of());
        when(queueEntryRepository.findByQueueIdOrderByPositionAsc(queue.getId())).thenReturn(List.of(entry));

        // WHEN
        QueueEntryResponse response = queueEntryService.callNext(queue.getId());

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(EntryStatus.CALLED);
        verify(eventPublisher, times(1)).publishUserCalled(any(), any());
        verify(eventPublisher, times(1)).publishQueueUpdated(any(), any());
    }

    @Test
    @DisplayName("should throw when no users waiting in queue")
    void shouldThrowWhenNoUsersWaiting() {
        // GIVEN
        Queue queue = buildOpenQueue();

        when(queueService.findById(queue.getId())).thenReturn(queue);
        when(queueEntryRepository.findFirstByQueueIdAndStatusOrderByPositionAsc(
                queue.getId(), EntryStatus.WAITING)).thenReturn(Optional.empty());

        // WHEN / THEN
        assertThatThrownBy(() -> queueEntryService.callNext(queue.getId()))
                .isInstanceOf(QueueException.class);
    }

    @Test
    @DisplayName("should throw when calling next on closed queue")
    void shouldThrowWhenCallingNextOnClosedQueue() {
        // GIVEN
        Queue queue = buildOpenQueue();
        queue.close();

        when(queueService.findById(queue.getId())).thenReturn(queue);

        // WHEN / THEN
        assertThatThrownBy(() -> queueEntryService.callNext(queue.getId()))
                .isInstanceOf(QueueException.class);
    }
}