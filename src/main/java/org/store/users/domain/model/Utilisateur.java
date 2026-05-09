package org.store.users.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.model.Person;
import org.store.security.domain.model.Account;

@Getter
@Setter
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = Utilisateur.TABLE_NAME)
public class Utilisateur extends Person {
    public final static String TABLE_NAME = "utilisateur";

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
}
