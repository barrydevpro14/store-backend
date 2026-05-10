package org.store.notification.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.notification.domain.model.Notification;

import java.util.UUID;

public interface NotificationJpaRepository extends JpaRepository<Notification, UUID> {
}
