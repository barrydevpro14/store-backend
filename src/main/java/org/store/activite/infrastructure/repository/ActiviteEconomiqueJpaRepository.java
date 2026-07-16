package org.store.activite.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.activite.domain.model.ActiviteEconomique;
import org.store.activite.domain.repository.ActiviteEconomiqueRepository;

import java.util.UUID;

@Repository
public interface ActiviteEconomiqueJpaRepository extends JpaRepository<ActiviteEconomique, UUID>,
        ActiviteEconomiqueRepository {
}
