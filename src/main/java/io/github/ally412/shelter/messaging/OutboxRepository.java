package io.github.ally412.shelter.messaging;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
    @Query("SELECT e FROM OutboxEvent e WHERE e.publishedAt IS NULL ORDER BY e.createdAt")
    List<OutboxEvent> findUnpublished(Pageable pageable);
    @Query(value = "SELECT pg_try_advisory_xact_lock(:key)",
            nativeQuery = true)
    boolean tryClaimRelay(@Param("key") long key);
}
