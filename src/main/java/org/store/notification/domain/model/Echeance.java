package org.store.notification.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.abonnement.domain.model.Abonnement;
import org.store.common.base.AuditableEntity;
import org.store.notification.domain.enums.EcheanceStatut;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = Echeance.TABLE_NAME)
public class Echeance extends AuditableEntity {
    public static final String TABLE_NAME = "echeance";

    @ManyToOne(fetch = FetchType.LAZY)
    private Abonnement abonnement;

    private LocalDate dateEcheance;

    @Column(precision = 19, scale = 2)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    private EcheanceStatut statut;

    private boolean notificationEnvoyee;

    private boolean rappelEnvoye;

    private int nombreRelances;

    private LocalDate dernierRappel;
}
