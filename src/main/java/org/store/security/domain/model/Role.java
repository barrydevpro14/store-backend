package org.store.security.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.BaseEntity;
import org.store.entreprise.domain.model.Entreprise;

import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = Role.TABLE_NAME)
public class Role extends BaseEntity {
    public final static String TABLE_NAME = "role";

    private String libelle;
    private String description;

    /**
     * Marqueur explicite : ce rôle peut-il être attribué à un employé ?
     * `true` pour MANAGER, SELLER et tous les rôles personnalisés ;
     * `false` pour OWNER et ADMIN (rôles système non délégables).
     */
    @Column(name = "assignable_to_employe", nullable = false)
    private boolean assignableToEmploye;

    /** `null` = rôle système global. Non-null = rôle personnalisé scoped à une entreprise. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id")
    private Entreprise entreprise;

    /** Seuls les rôles personnalisés peuvent être désactivés. Les rôles système sont toujours actifs. */
    @Column(nullable = false)
    private boolean actif = true;

    @ManyToMany
    @JoinTable(
            name = "role_permission",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permissions> permissions;

    /** Returns true if this is a system (global) role — entreprise is null. */
    public boolean isSystemRole() {
        return entreprise == null;
    }
}
