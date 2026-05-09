package org.store.common.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;

@Getter
@Setter
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = Person.TABLE_NAME)
public abstract class Person extends AuditableEntity {
    public final static String TABLE_NAME = "person";
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String adresse;
}
