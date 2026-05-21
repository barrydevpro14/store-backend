package org.store.abonnement.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.abonnement.domain.enums.AbonnementStatut;
import org.store.common.base.AuditableEntity;
import org.store.entreprise.domain.model.Entreprise;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = Abonnement.TABLE_NAME)
public class Abonnement extends AuditableEntity {
    public static final String TABLE_NAME = "abonnement";

    @ManyToOne(fetch = FetchType.LAZY)
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "type_plan_abonnement_id", nullable = false)
    private TypePlanAbonnement typePlanAbonnement;

    private LocalDate dateDebut;

    private LocalDate dateFin;

    private boolean actif = true;

    private boolean renouvellementAuto = false;

    @Enumerated(EnumType.STRING)
    private AbonnementStatut statut;
}
