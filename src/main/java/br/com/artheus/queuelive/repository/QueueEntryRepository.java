package br.com.artheus.queuelive.repository;

import br.com.artheus.queuelive.entity.QueueEntry;
import br.com.artheus.queuelive.enums.EntryStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QueueEntryRepository extends JpaRepository<QueueEntry, UUID> {

    @EntityGraph(attributePaths = {"user"})
    List<QueueEntry> findByQueueIdOrderByPositionAsc(UUID queueId);

    @EntityGraph(attributePaths = {"user"})
    List<QueueEntry> findByQueueIdAndStatus(UUID queueId, EntryStatus status);

    @EntityGraph(attributePaths = {"user"})
    Optional<QueueEntry> findFirstByQueueIdAndStatusOrderByPositionAsc(UUID queueId, EntryStatus status);

    boolean existsByQueueIdAndUserId(UUID queueId, UUID userId);

    int countByQueueIdAndStatus(UUID queueId, EntryStatus status);
}
