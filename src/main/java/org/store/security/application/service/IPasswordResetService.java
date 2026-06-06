package org.store.security.application.service;

import org.store.security.application.dto.ForgotPasswordRequest;
import org.store.security.application.dto.ResetPasswordConfirmRequest;

public interface IPasswordResetService {

    /** Génère un token, le persiste et envoie le lien par e-mail.
     *  Toujours silencieux (204) — ne révèle pas l'existence du compte. */
    void requestReset(ForgotPasswordRequest request);

    /** Valide le token, réinitialise le mot de passe, marque le token consommé.
     *  Lève BadArgumentException si token invalide, expiré ou déjà utilisé. */
    void confirmReset(ResetPasswordConfirmRequest request);
}
