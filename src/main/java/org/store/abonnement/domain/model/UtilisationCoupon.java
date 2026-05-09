package org.store.abonnement.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.BaseEntity;
import org.store.magasin.domain.model.Entreprise;

@Getter
@Setter
@Entity
@Table(name = UtilisationCoupon.TABLE_NAME)
public class UtilisationCoupon extends BaseEntity {
    public static final String TABLE_NAME = "utilisation_coupon";

    @ManyToOne(fetch = FetchType.LAZY)
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY)
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.LAZY)
    private Abonnement abonnement;
}
