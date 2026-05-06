package br.com.artheus.queuelive.config;

import br.com.artheus.queuelive.dto.queue.QueueEntryResponse;
import br.com.artheus.queuelive.dto.queue.QueueEventPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class QueueEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishQueueUpdated(Long queueId, List<QueueEntryResponse> entries) {
        QueueEventPayload payload = new QueueEventPayload("QUEUE_UPDATED", queueId, entries);
        messagingTemplate.convertAndSend("/topic/queue/" + queueId, payload);
    }

    public void publishUserCalled(Long queueId, QueueEntryResponse entry) {
        QueueEventPayload payload = new QueueEventPayload("USER_CALLED", queueId, List.of(entry));
        messagingTemplate.convertAndSend("/topic/queue/" + queueId, payload);
    }
}