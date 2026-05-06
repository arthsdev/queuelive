package br.com.artheus.queuelive.repository;

import br.com.artheus.queuelive.entity.QueueEntry;
import br.com.artheus.queuelive.enums.EntryStatus;
import br.com.artheus.queuelive.enums.QueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QueueEntryRepository extends JpaRepository<QueueEntry, UUID> {

    List<QueueEntry> findByQueueIdOrderByPositionAsc(UUID queueId);

    List<QueueEntry> findByQueueIdAndStatus(UUID queueId, EntryStatus status);

    Optional<QueueEntry> findFirstByQueueIdAndStatusOrderByPositionAsc(UUID queueId, EntryStatus status);

    boolean existsByQueueIdAndUserId(UUID queueId, UUID userId);

    int countByQueueIdAndStatus(UUID queueId, EntryStatus status);
}
