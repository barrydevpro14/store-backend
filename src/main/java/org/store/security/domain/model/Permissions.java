package org.store.security.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.store.common.base.BaseEntity;
@Entity
@Table(name = Permissions.TABLE_NAME)
public class Permissions extends BaseEntity {
    public final static String TABLE_NAME = "permissions";
    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
