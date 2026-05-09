package org.store.security.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;
import org.store.users.domain.model.Utilisateur;

import java.time.LocalDateTime;

@Getter
@Setter
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
}
