package org.store.users.domain.model;

import jakarta.persistence.*;
import org.store.common.model.Person;
import org.store.security.domain.model.Account;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = Utilisateur.TABLE_NAME)
public class Utilisateur extends Person {
    public final static String TABLE_NAME = "utilisateur";

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }
}
