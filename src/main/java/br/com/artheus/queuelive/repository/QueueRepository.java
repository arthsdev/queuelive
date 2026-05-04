package br.com.artheus.queuelive.repository;

import br.com.artheus.queuelive.entity.Queue;
import br.com.artheus.queuelive.enums.QueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QueueRepository extends JpaRepository<Queue, Long> {

    List<Queue> findByStatus(QueueStatus status);
}