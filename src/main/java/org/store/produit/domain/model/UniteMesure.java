package org.store.produit.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = UniteMesure.TABLE_NAME)
public class UniteMesure extends BaseEntity {

    public static final String TABLE_NAME = "unites_mesure";

    @Column(nullable = false, length = 20, unique = true, updatable = false)
    private String code;

    @Column(nullable = false, length = 100)
    private String libelle;

    @Column(nullable = false, length = 10)
    private String symbole;


}
