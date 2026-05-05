package br.com.artheus.queuelive.repository;

import br.com.artheus.queuelive.entity.QueueEntry;
import br.com.artheus.queuelive.enums.EntryStatus;
import br.com.artheus.queuelive.enums.QueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {

    List<QueueEntry> findByQueueIdOrderByPositionAsc(Long queueId);

    List<QueueEntry> findByQueueIdAndStatus(Long queueId, EntryStatus status);

    Optional<QueueEntry> findFirstByQueueIdAndStatusOrderByPositionAsc(Long queueId, EntryStatus status);

    boolean existsByQueueIdAndUserId(Long queueId, Long userId);

    int countByQueueIdAndStatus(Long queueId, EntryStatus status);
}
