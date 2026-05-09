package org.store.notification.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.store.common.base.AuditableEntity;
import org.store.notification.domain.enums.CanalNotification;

public class NotificationTemplate extends AuditableEntity {

        @Column(unique = true)
        private String code;

        private String sujet;

        @Column(columnDefinition = "TEXT")
        private String contenu;

        @Enumerated(EnumType.STRING)
        private CanalNotification canal;

        private boolean actif = true;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getSujet() {
        return sujet;
    }

    public void setSujet(String sujet) {
        this.sujet = sujet;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public CanalNotification getCanal() {
        return canal;
    }

    public void setCanal(CanalNotification canal) {
        this.canal = canal;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }
}
