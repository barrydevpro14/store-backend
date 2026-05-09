package org.store.security.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;
import org.store.users.domain.model.Utilisateur;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = RefreshToken.TABLE_NAME)
public class RefreshToken extends AuditableEntity {
    public final static String TABLE_NAME = "refresh_token";

    private String token;
    private LocalDateTime expiryDate;
    private boolean revoked;
    private String replacedByToken;

    @ManyToOne
    private Utilisateur user;
}
