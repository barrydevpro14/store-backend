package org.store.country.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = Country.TABLE_NAME)
public class Country extends BaseEntity {

    public static final String TABLE_NAME = "country";

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 5, unique = true)
    private String countryCode;

    @Column(nullable = false, length = 5)
    private String currency;

    private boolean actif = true;
}
