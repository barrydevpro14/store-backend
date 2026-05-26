package org.store.abonnement.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.store.abonnement.application.dto.AbonnementFilter;
import org.store.abonnement.application.dto.AbonnementResponse;
import org.store.abonnement.domain.enums.AbonnementStatut;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.model.TypePlanAbonnement;
import org.store.abonnement.domain.repository.AbonnementRepository;
import org.store.entreprise.domain.model.Entreprise;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbonnementDomainServiceTest {

    @Mock private AbonnementRepository repository;

    @InjectMocks
    private AbonnementDomainService service;

    private UUID entrepriseId;
    private UUID abonnementId;
    private Entreprise entreprise;
    private PlanAbonnement plan;
    private TypePlanAbonnement type;

    @BeforeEach
    void setUp() {
        entrepriseId = UUID.randomUUID();
        abonnementId = UUID.randomUUID();

        entreprise = new Entreprise();
        entreprise.setId(entrepriseId);

        plan = new PlanAbonnement();
        plan.setId(UUID.randomUUID());
        plan.setNom("Pro");

        type = new TypePlanAbonnement();
        type.setId(UUID.randomUUID());
        type.setPlan(plan);
        type.setNom("Annuel");
        type.setDureeMois(12);
    }

    @Test
    void createPending_should_persist_abonnement_en_attente_with_type_only() {
        when(repository.save(any(Abonnement.class))).thenAnswer(inv -> inv.getArgument(0));

        Abonnement result = service.createPending(entreprise, type);

        ArgumentCaptor<Abonnement> captor = ArgumentCaptor.forClass(Abonnement.class);
        verify(repository).save(captor.capture());
        Abonnement saved = captor.getValue();
        assertThat(saved.getEntreprise()).isSameAs(entreprise);
        assertThat(saved.getTypePlanAbonnement()).isSameAs(type);
        assertThat(saved.isActif()).isFalse();
        assertThat(saved.isRenouvellementAuto()).isFalse();
        assertThat(saved.getStatut()).isEqualTo(AbonnementStatut.EN_ATTENTE);
        assertThat(result).isSameAs(saved);
    }

    @Test
    void setRenouvellementAuto_should_enable_and_persist() {
        Abonnement abonnement = new Abonnement();
        abonnement.setRenouvellementAuto(false);
        when(repository.save(abonnement)).thenAnswer(inv -> inv.getArgument(0));

        Abonnement result = service.setRenouvellementAuto(abonnement, true);

        assertThat(result.isRenouvellementAuto()).isTrue();
        verify(repository).save(abonnement);
    }

    @Test
    void setRenouvellementAuto_should_disable_and_persist() {
        Abonnement abonnement = new Abonnement();
        abonnement.setRenouvellementAuto(true);
        when(repository.save(abonnement)).thenAnswer(inv -> inv.getArgument(0));

        Abonnement result = service.setRenouvellementAuto(abonnement, false);

        assertThat(result.isRenouvellementAuto()).isFalse();
        verify(repository).save(abonnement);
    }

    @Test
    void activate_should_set_dates_actif_and_status() {
        Abonnement abonnement = new Abonnement();
        abonnement.setId(abonnementId);
        abonnement.setEntreprise(entreprise);
        abonnement.setStatut(AbonnementStatut.EN_ATTENTE);
        abonnement.setActif(false);
        LocalDate dateDebut = LocalDate.of(2026, 5, 21);
        LocalDate dateFin = dateDebut.plusMonths(12);
        when(repository.save(abonnement)).thenAnswer(inv -> inv.getArgument(0));

        Abonnement result = service.activate(abonnement, dateDebut, dateFin);

        assertThat(result.getDateDebut()).isEqualTo(dateDebut);
        assertThat(result.getDateFin()).isEqualTo(dateFin);
        assertThat(result.isActif()).isTrue();
        assertThat(result.getStatut()).isEqualTo(AbonnementStatut.ACTIF);
    }

    @Test
    void activate_should_expire_sibling_actif_rows_before_flipping_target() {
        Abonnement target = new Abonnement();
        target.setId(abonnementId);
        target.setEntreprise(entreprise);
        target.setStatut(AbonnementStatut.EN_ATTENTE);
        target.setActif(false);
        when(repository.save(target)).thenAnswer(inv -> inv.getArgument(0));

        service.activate(target, LocalDate.now(), LocalDate.now().plusMonths(1));

        verify(repository).expireOtherActifByEntreprise(entrepriseId, abonnementId);
    }

    @Test
    void findCurrentActif_should_return_optional_from_repository() {
        Abonnement abonnement = new Abonnement();
        when(repository.findFirstByEntrepriseAndStatut(entrepriseId, AbonnementStatut.ACTIF))
                .thenReturn(Optional.of(abonnement));

        Optional<Abonnement> result = service.findCurrentActif(entrepriseId);

        assertThat(result).containsSame(abonnement);
    }

    @Test
    void findCurrentActif_should_return_empty_when_repository_finds_none() {
        when(repository.findFirstByEntrepriseAndStatut(entrepriseId, AbonnementStatut.ACTIF))
                .thenReturn(Optional.empty());

        assertThat(service.findCurrentActif(entrepriseId)).isEmpty();
    }

    @Test
    void findLatestActifDateFin_should_delegate_with_exclusion_id() {
        LocalDate expected = LocalDate.of(2026, 12, 31);
        when(repository.findLatestActifDateFin(entrepriseId, abonnementId)).thenReturn(Optional.of(expected));

        Optional<LocalDate> result = service.findLatestActifDateFin(entrepriseId, abonnementId);

        assertThat(result).contains(expected);
    }

    @Test
    void findLatestActifDateFin_should_return_empty_when_none() {
        when(repository.findLatestActifDateFin(entrepriseId, abonnementId)).thenReturn(Optional.empty());

        assertThat(service.findLatestActifDateFin(entrepriseId, abonnementId)).isEmpty();
    }

    @Test
    void findResponses_should_delegate_with_filter_pageable() {
        AbonnementFilter filter = new AbonnementFilter(entrepriseId, "ACTIF", null, null, null, null, 0, 10);
        Page<AbonnementResponse> page = new PageImpl<>(List.of());
        when(repository.findResponsesByFilter(eq(filter), any(Pageable.class))).thenReturn(page);

        Page<AbonnementResponse> result = service.findResponses(filter);

        assertThat(result).isSameAs(page);
        verify(repository).findResponsesByFilter(eq(filter), any(Pageable.class));
    }
}
