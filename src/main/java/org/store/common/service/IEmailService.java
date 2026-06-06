package org.store.common.service;

import org.store.notification.application.event.ContactMessageRepliedEvent;

/** Outbound email operations. Implementations are conditional on SMTP configuration. */
public interface IEmailService {
    void sendContactReply(ContactMessageRepliedEvent event);

    /** Envoie le lien de réinitialisation du mot de passe à l'adresse indiquée. */
    void sendPasswordReset(String toEmail, String recipientName, String resetLink);

    /** Envoie les identifiants de connexion générés au nouvel employé. */
    void sendWelcomeEmploye(String toEmail, String recipientName, String username, String password);

    /** Envoie un email de bienvenue au propriétaire qui vient de s'inscrire. */
    void sendWelcomeOwner(String toEmail, String recipientName, String username, String entrepriseName, String loginUrl);
}
