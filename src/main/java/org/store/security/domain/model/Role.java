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

    @ManyToMany
    @JoinTable(
            name = "role_permission",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permissions> permissions;
}
