package org.store.abonnement.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.store.common.base.BaseEntity;
import org.store.magasin.domain.model.Entreprise;
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

    public Coupon getCoupon() {
        return coupon;
    }

    public void setCoupon(Coupon coupon) {
        this.coupon = coupon;
    }

    public Entreprise getEntreprise() {
        return entreprise;
    }

    public void setEntreprise(Entreprise entreprise) {
        this.entreprise = entreprise;
    }

    public Abonnement getAbonnement() {
        return abonnement;
    }

    public void setAbonnement(Abonnement abonnement) {
        this.abonnement = abonnement;
    }
}
