package org.store.entreprise.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.entreprise.domain.model.EntrepriseSetting;
import org.store.entreprise.domain.repository.EntrepriseSettingRepository;

import java.util.UUID;

@Repository
public interface EntrepriseSettingJpaRepository extends JpaRepository<EntrepriseSetting, UUID>, EntrepriseSettingRepository {
}
