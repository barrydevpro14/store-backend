package org.store.security.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.store.common.base.AuditableEntity;
import org.store.users.domain.model.Utilisateur;

import java.time.LocalDateTime;
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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public String getReplacedByToken() {
        return replacedByToken;
    }

    public void setReplacedByToken(String replacedByToken) {
        this.replacedByToken = replacedByToken;
    }

    public Utilisateur getUser() {
        return user;
    }

    public void setUser(Utilisateur user) {
        this.user = user;
    }
}
