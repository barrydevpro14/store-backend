package org.store.notification.domain.model;

import jakarta.persistence.*;
import org.store.achat.domain.model.FactureAchat;
import org.store.common.base.AuditableEntity;
import org.store.notification.domain.enums.CanalNotification;
import org.store.notification.domain.enums.NotificationStatut;
import org.store.security.domain.model.Account;
import org.store.vente.domain.model.FactureClient;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Notification extends AuditableEntity {

    private String titre;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    private CanalNotification canal;

    @Enumerated(EnumType.STRING)
    private NotificationStatut statut;

    private LocalDateTime dateEnvoi;

    private LocalDate prochaineTentative;

    private int nombreTentatives = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    private Account destinataire;

    @ManyToOne(fetch = FetchType.LAZY)
    private FactureClient factureClient;

    @ManyToOne(fetch = FetchType.LAZY)
    private FactureAchat factureAchat;

    @ManyToOne(fetch = FetchType.LAZY)
    private Echeance echeance;


    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public CanalNotification getCanal() {
        return canal;
    }

    public void setCanal(CanalNotification canal) {
        this.canal = canal;
    }

    public NotificationStatut getStatut() {
        return statut;
    }

    public void setStatut(NotificationStatut statut) {
        this.statut = statut;
    }

    public LocalDateTime getDateEnvoi() {
        return dateEnvoi;
    }

    public void setDateEnvoi(LocalDateTime dateEnvoi) {
        this.dateEnvoi = dateEnvoi;
    }

    public LocalDate getProchaineTentative() {
        return prochaineTentative;
    }

    public void setProchaineTentative(LocalDate prochaineTentative) {
        this.prochaineTentative = prochaineTentative;
    }

    public int getNombreTentatives() {
        return nombreTentatives;
    }

    public void setNombreTentatives(int nombreTentatives) {
        this.nombreTentatives = nombreTentatives;
    }

    public Account getDestinataire() {
        return destinataire;
    }

    public void setDestinataire(Account destinataire) {
        this.destinataire = destinataire;
    }

    public FactureClient getFactureClient() {
        return factureClient;
    }

    public void setFactureClient(FactureClient factureClient) {
        this.factureClient = factureClient;
    }

    public FactureAchat getFactureAchat() {
        return factureAchat;
    }

    public void setFactureAchat(FactureAchat factureAchat) {
        this.factureAchat = factureAchat;
    }

    public Echeance getEcheance() {
        return echeance;
    }

    public void setEcheance(Echeance echeance) {
        this.echeance = echeance;
    }
}
