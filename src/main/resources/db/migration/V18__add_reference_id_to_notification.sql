-- FK to the contact_message that triggered this notification (nullable — only set for contact events).
ALTER TABLE notification ADD COLUMN IF NOT EXISTS contact_message_id UUID;
ALTER TABLE notification ADD CONSTRAINT fk_notification_contact_message
    FOREIGN KEY (contact_message_id) REFERENCES contact_message(id) ON DELETE SET NULL;
