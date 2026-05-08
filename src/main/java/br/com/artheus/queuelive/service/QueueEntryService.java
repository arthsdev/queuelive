package br.com.artheus.queuelive.service;

import br.com.artheus.queuelive.config.QueueEventPublisher;
import br.com.artheus.queuelive.dto.queue.QueueEntryResponse;
import br.com.artheus.queuelive.entity.Queue;
import br.com.artheus.queuelive.entity.QueueEntry;
import br.com.artheus.queuelive.entity.User;
import br.com.artheus.queuelive.enums.EntryStatus;
import br.com.artheus.queuelive.enums.Role;
import br.com.artheus.queuelive.exception.domain.QueueEntryException;
import br.com.artheus.queuelive.exception.domain.QueueException;
import br.com.artheus.queuelive.repository.QueueEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QueueEntryService {

    private final QueueEntryRepository queueEntryRepository;
    private final QueueService queueService;
    private final QueueEventPublisher eventPublisher;

    @Transactional
    public QueueEntryResponse join(UUID queueId, User user) {

        // Validates role before any database query
        if (user.getRole() == Role.STAFF) {
            throw QueueEntryException.staffCannotJoin();
        }

        Queue queue = queueService.findById(queueId);

        if (queue.getStatus().isClosed()) {
            throw QueueException.isClosed();
        }

        if (queueEntryRepository.existsByQueueIdAndUserId(queueId, user.getId())) {
            throw QueueEntryException.userAlreadyInQueue();
        }

        int position = queueEntryRepository.countByQueueIdAndStatus(queueId, EntryStatus.WAITING) + 1;

        QueueEntry entry = QueueEntry.builder()
                .queue(queue)
                .user(user)
                .position(position)
                .build();

        queueEntryRepository.save(entry);

        // Notifies all connected clients about the queue update
        List<QueueEntryResponse> entries = findAllByQueue(queueId);
        eventPublisher.publishQueueUpdated(queueId, entries);

        return toResponse(entry);
    }

    @Transactional
    public QueueEntryResponse callNext(UUID queueId) {
        Queue queue = queueService.findById(queueId);

        if (queue.getStatus().isClosed()) {
            throw QueueException.isClosed();
        }

        // Gets the first WAITING entry ordered by position
        QueueEntry next = queueEntryRepository
                .findFirstByQueueIdAndStatusOrderByPositionAsc(queueId, EntryStatus.WAITING)
                .orElseThrow(QueueException::noUsersWaiting);

        next.call();
        queueEntryRepository.save(next);

        // Recalculates positions for all remaining WAITING entries
        recalculatePositions(queueId);

        // Notifies all connected clients about who was called
        eventPublisher.publishUserCalled(queueId, toResponse(next));

        // Notifies all connected clients about the updated queue
        List<QueueEntryResponse> entries = findAllByQueue(queueId);
        eventPublisher.publishQueueUpdated(queueId, entries);

        return toResponse(next);
    }

    @Transactional
    public List<QueueEntryResponse> findAllByQueue(UUID queueId) {
        return queueEntryRepository.findByQueueIdOrderByPositionAsc(queueId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void recalculatePositions(UUID queueId) {
        List<QueueEntry> waiting = queueEntryRepository
                .findByQueueIdAndStatus(queueId, EntryStatus.WAITING);

        // Recalculates position for each waiting entry to avoid gaps after a CALLED status
        for (int i = 0; i < waiting.size(); i++) {
            waiting.get(i).updatePosition(i + 1);
        }

        queueEntryRepository.saveAll(waiting);
    }

    private QueueEntryResponse toResponse(QueueEntry entry) {
        return new QueueEntryResponse(
                entry.getId(),
                entry.getUser().getName(),
                entry.getPosition(),
                entry.getStatus()
        );
    }
}