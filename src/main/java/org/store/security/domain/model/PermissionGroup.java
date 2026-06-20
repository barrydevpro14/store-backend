package org.store.security.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = PermissionGroup.TABLE_NAME)
public class PermissionGroup extends BaseEntity {

    public static final String TABLE_NAME = "permission_group";

    private String libelle;

    private String description;

    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    @OrderBy("code ASC")
    private List<Permissions> permissions = new ArrayList<>();
}
