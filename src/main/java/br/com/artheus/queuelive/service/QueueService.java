package br.com.artheus.queuelive.service;

import br.com.artheus.queuelive.dto.QueueRequest;
import br.com.artheus.queuelive.dto.QueueResponse;
import br.com.artheus.queuelive.entity.Queue;
import br.com.artheus.queuelive.entity.User;
import br.com.artheus.queuelive.enums.QueueStatus;
import br.com.artheus.queuelive.repository.QueueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QueueService {

    private final QueueRepository queueRepository;

    public QueueResponse create(QueueRequest request, User createdBy) {
        Queue queue = Queue.builder()
                .name(request.name())
                .createdBy(createdBy)
                .build();

        return toResponse(queueRepository.save(queue));
    }

    @Transactional(readOnly = true)
    public List<QueueResponse> findAll() {
        return queueRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QueueResponse> findAllOpen() {
        return queueRepository.findByStatus(QueueStatus.OPEN)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public Queue findById(Long id) {
        return queueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Queue not found"));
    }

    public QueueResponse close(Long id) {
        Queue queue = findById(id);

        if (queue.getStatus() == QueueStatus.CLOSED) {
            throw new RuntimeException("Queue is already closed");
        }

        // Reflects the closed state in the database before notifying clients via WebSocket
        queue.close();
        return toResponse(queueRepository.save(queue));
    }

    public QueueResponse findByIdAsResponse(Long id) {
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