package org.store.abonnement.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.abonnement.domain.model.Promotion;
import org.store.abonnement.domain.repository.PromotionRepository;

import java.util.UUID;

@Repository
public interface PromotionJpaRepository extends JpaRepository<Promotion, UUID>, PromotionRepository {
}
