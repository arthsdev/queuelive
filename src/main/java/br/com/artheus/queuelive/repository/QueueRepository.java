package br.com.artheus.queuelive.repository;

import br.com.artheus.queuelive.entity.Queue;
import br.com.artheus.queuelive.enums.QueueStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QueueRepository extends JpaRepository<Queue, UUID> {

    @EntityGraph(attributePaths = {"createdBy"})
    List<Queue> findByStatus(QueueStatus status);

    @EntityGraph(attributePaths = {"createdBy"})
    Optional<Queue> findById(UUID id);
}