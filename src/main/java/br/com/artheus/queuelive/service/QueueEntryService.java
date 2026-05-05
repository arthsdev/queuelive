package br.com.artheus.queuelive.service;

import br.com.artheus.queuelive.config.QueueEventPublisher;
import br.com.artheus.queuelive.dto.QueueEntryResponse;
import br.com.artheus.queuelive.entity.Queue;
import br.com.artheus.queuelive.entity.QueueEntry;
import br.com.artheus.queuelive.entity.User;
import br.com.artheus.queuelive.enums.EntryStatus;
import br.com.artheus.queuelive.enums.QueueStatus;
import br.com.artheus.queuelive.repository.QueueEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QueueEntryService {

    private final QueueEntryRepository queueEntryRepository;
    private final QueueService queueService;
    private final QueueEventPublisher eventPublisher;

    @Transactional
    public QueueEntryResponse join(Long queueId, User user) {
        Queue queue = queueService.findById(queueId);

        if (queue.getStatus() == QueueStatus.CLOSED) {
            throw new RuntimeException("Queue is closed");
        }

        if (queueEntryRepository.existsByQueueIdAndUserId(queueId, user.getId())) {
            throw new RuntimeException("User already in queue");
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
    public QueueEntryResponse callNext(Long queueId) {
        Queue queue = queueService.findById(queueId);

        if (queue.getStatus() == QueueStatus.CLOSED) {
            throw new RuntimeException("Queue is closed");
        }

        // Gets the first WAITING entry ordered by position
        QueueEntry next = queueEntryRepository
                .findFirstByQueueIdAndStatusOrderByPositionAsc(queueId, EntryStatus.WAITING)
                .orElseThrow(() -> new RuntimeException("No users waiting in queue"));

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

    @Transactional(readOnly = true)
    public List<QueueEntryResponse> findAllByQueue(Long queueId) {
        return queueEntryRepository.findByQueueIdOrderByPositionAsc(queueId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void recalculatePositions(Long queueId) {
        List<QueueEntry> waiting = queueEntryRepository
                .findByQueueIdAndStatus(queueId, EntryStatus.WAITING);

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