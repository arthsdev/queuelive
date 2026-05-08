package br.com.artheus.queuelive.unit.queue;

import br.com.artheus.queuelive.dto.queue.QueueRequest;
import br.com.artheus.queuelive.dto.queue.QueueResponse;
import br.com.artheus.queuelive.entity.Queue;
import br.com.artheus.queuelive.entity.User;
import br.com.artheus.queuelive.enums.QueueStatus;
import br.com.artheus.queuelive.enums.Role;
import br.com.artheus.queuelive.exception.domain.QueueException;
import br.com.artheus.queuelive.repository.QueueRepository;
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
@DisplayName("QueueService unit tests")
class QueueServiceTest {

    @Mock
    private QueueRepository queueRepository;

    @InjectMocks
    private QueueService queueService;

    // ---- helpers ----

    private User buildStaff() {
        return User.builder()
                .id(UUID.randomUUID())
                .name("Staff 01")
                .email("staff01@queuelive.com")
                .password("KEYCLOAK_MANAGED")
                .role(Role.STAFF)
                .build();
    }

    private Queue buildQueue(User createdBy) {
        return Queue.builder()
                .id(UUID.randomUUID())
                .name("Fila de Atendimento")
                .status(QueueStatus.OPEN)
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ---- create ----

    @Test
    @DisplayName("should create a queue successfully")
    void shouldCreateQueue() {
        // GIVEN
        User staff = buildStaff();
        QueueRequest request = new QueueRequest("Fila de Atendimento");
        Queue savedQueue = buildQueue(staff);

        when(queueRepository.save(any(Queue.class))).thenReturn(savedQueue);

        // WHEN
        QueueResponse response = queueService.create(request, staff);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Fila de Atendimento");
        assertThat(response.status()).isEqualTo(QueueStatus.OPEN);
        assertThat(response.createdBy()).isEqualTo("Staff 01");
        verify(queueRepository, times(1)).save(any(Queue.class));
    }

    // ---- findById ----

    @Test
    @DisplayName("should find queue by id successfully")
    void shouldFindQueueById() {
        // GIVEN
        User staff = buildStaff();
        Queue queue = buildQueue(staff);

        when(queueRepository.findById(queue.getId())).thenReturn(Optional.of(queue));

        // WHEN
        Queue result = queueService.findById(queue.getId());

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(queue.getId());
        assertThat(result.getName()).isEqualTo("Fila de Atendimento");
    }

    @Test
    @DisplayName("should throw QueueException when queue not found")
    void shouldThrowWhenQueueNotFound() {
        // GIVEN
        UUID randomId = UUID.randomUUID();
        when(queueRepository.findById(randomId)).thenReturn(Optional.empty());

        // WHEN / THEN
        assertThatThrownBy(() -> queueService.findById(randomId))
                .isInstanceOf(QueueException.class);
    }

    // ---- close ----

    @Test
    @DisplayName("should close a queue successfully")
    void shouldCloseQueue() {
        // GIVEN
        User staff = buildStaff();
        Queue queue = buildQueue(staff);
        Queue closedQueue = buildQueue(staff);
        closedQueue.close();

        when(queueRepository.findById(queue.getId())).thenReturn(Optional.of(queue));
        when(queueRepository.save(any(Queue.class))).thenReturn(closedQueue);

        // WHEN
        QueueResponse response = queueService.close(queue.getId());

        // THEN
        assertThat(response.status()).isEqualTo(QueueStatus.CLOSED);
        verify(queueRepository, times(1)).save(any(Queue.class));
    }

    @Test
    @DisplayName("should throw when closing an already closed queue")
    void shouldThrowWhenClosingAlreadyClosedQueue() {
        // GIVEN
        User staff = buildStaff();
        Queue queue = buildQueue(staff);
        queue.close();

        when(queueRepository.findById(queue.getId())).thenReturn(Optional.of(queue));

        // WHEN / THEN
        assertThatThrownBy(() -> queueService.close(queue.getId()))
                .isInstanceOf(QueueException.class);
    }

    // ---- findAllOpen ----

    @Test
    @DisplayName("should return all open queues")
    void shouldReturnAllOpenQueues() {
        // GIVEN
        User staff = buildStaff();
        List<Queue> queues = List.of(buildQueue(staff), buildQueue(staff));

        when(queueRepository.findByStatus(QueueStatus.OPEN)).thenReturn(queues);

        // WHEN
        List<QueueResponse> response = queueService.findAllOpen();

        // THEN
        assertThat(response).hasSize(2);
        assertThat(response).allMatch(q -> q.status() == QueueStatus.OPEN);
    }
}