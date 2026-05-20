package org.store.security.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.BaseEntity;

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
     * Marqueur explicite : ce rôle peut-il être attribué à un employé via
     * `EmployeServiceImpl.create` ? `true` pour MANAGER, VENDEUR ; `false`
     * pour PROPRIETAIRE (créé par l'inscription) et ADMIN (super-admin SaaS).
     * Mappé sur la colonne `assignable_to_employe` (DEFAULT FALSE).
     */
    @Column(name = "assignable_to_employe", nullable = false)
    private boolean assignableToEmploye;

    @ManyToMany
    @JoinTable(
            name = "role_permission",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permissions> permissions;
}
