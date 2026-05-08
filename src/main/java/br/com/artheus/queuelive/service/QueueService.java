package br.com.artheus.queuelive.service;

import br.com.artheus.queuelive.dto.queue.QueueRequest;
import br.com.artheus.queuelive.dto.queue.QueueResponse;
import br.com.artheus.queuelive.entity.Queue;
import br.com.artheus.queuelive.entity.User;
import br.com.artheus.queuelive.enums.QueueStatus;
import br.com.artheus.queuelive.exception.domain.QueueException;
import br.com.artheus.queuelive.repository.QueueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QueueService {

    private final QueueRepository queueRepository;

    @Transactional
    public QueueResponse create(QueueRequest request, User createdBy) {
        Queue queue = Queue.builder()
                .name(request.name())
                .createdBy(createdBy)
                .build();

        return toResponse(queueRepository.save(queue));
    }

    @Transactional(readOnly = true)
    public List<QueueResponse> findAllOpen() {
        return queueRepository.findByStatus(QueueStatus.OPEN)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Queue findById(UUID id) {
        return queueRepository.findById(id)
                .orElseThrow(QueueException::notFound);
    }

    @Transactional
    public QueueResponse close(UUID id) {
        Queue queue = findById(id);

        if (queue.getStatus().isClosed()) {
            throw QueueException.alreadyClosed();
        }

        // Reflects the closed state in the database before notifying clients via WebSocket
        queue.close();
        return toResponse(queueRepository.save(queue));
    }

    @Transactional(readOnly = true)
    public QueueResponse findByIdAsResponse(UUID id) {
        return toResponse(findById(id));
    }

    private QueueResponse toResponse(Queue queue) {
        return new QueueResponse(
                queue.getId(),
                queue.getName(),
                queue.getStatus(),
                queue.getCreatedBy().getName(),
                queue.getCreatedAt()
        );
    }
}