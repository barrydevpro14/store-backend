package org.store.security.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = Permissions.TABLE_NAME)
public class Permissions extends BaseEntity {
    public final static String TABLE_NAME = "permissions";
    private String code;
}
