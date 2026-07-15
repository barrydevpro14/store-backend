package org.store.sequence.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.sequence.domain.model.DocumentSequence;
import org.store.sequence.domain.repository.DocumentSequenceRepository;

import java.util.UUID;

public interface DocumentSequenceJpaRepository extends JpaRepository<DocumentSequence, UUID>, DocumentSequenceRepository {
}
