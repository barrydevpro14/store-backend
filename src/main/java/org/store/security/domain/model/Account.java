package org.store.security.domain.model;

import jakarta.persistence.*;
import org.store.common.base.AuditableEntity;
import org.store.users.domain.model.Utilisateur;

import java.time.LocalDateTime;

@Entity
@Table(name = Account.TABLE_NAME)
public class Account extends AuditableEntity {
    public final static String TABLE_NAME = "account";
    private String username;
    private String password;
    private boolean enabled;
    private boolean locked;
    private LocalDateTime creationDate = LocalDateTime.now();

    @OneToOne(mappedBy = "account")
    private Utilisateur user;

    @ManyToOne(fetch = FetchType.LAZY)
    private Role role;

    public Utilisateur getUser() {
        return user;
    }

    public void setUser(Utilisateur user) {
        this.user = user;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
