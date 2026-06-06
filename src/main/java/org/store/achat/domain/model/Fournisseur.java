package org.store.achat.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.model.Person;
import org.store.entreprise.domain.model.Entreprise;

@Getter
@Setter
@Entity
@Table(name = Fournisseur.TABLE_NAME)
public class Fournisseur extends Person {
    public final static String TABLE_NAME = "fournisseur";

    public static final String ANONYMOUS_REFERENCE = "__ANONYMOUS__";

    private String reference;

    private String origine;

    /** System-seeded supplier — visible to all companies, not editable or deletable. */
    @Column(nullable = false)
    private boolean systeme = false;

    /** null = system/global supplier shared across all companies. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id")
    private Entreprise entreprise;
}
