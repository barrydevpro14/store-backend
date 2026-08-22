package org.store.security.application.service;

import org.store.security.domain.model.Account;
import org.store.security.application.dto.ForgotPasswordRequest;
import org.store.security.application.dto.ResetPasswordConfirmRequest;

public interface IPasswordResetService {

    /** Generates a token, persists it, and sends the reset link by email — always silent (204). */
    void requestReset(ForgotPasswordRequest forgotPasswordRequest);

    /** Validates the token, resets the password, and marks the token as consumed. */
    void confirmReset(ResetPasswordConfirmRequest resetPasswordConfirmRequest);

    /** Returns the email address linked to the account, or null if none. */
    String resolveEmail(Account account);

    /** Returns a display name for the account (prenom + nom, or username as fallback). */
    String resolveRecipientName(Account account);
}
