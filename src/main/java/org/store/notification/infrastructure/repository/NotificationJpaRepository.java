package org.store.notification.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.notification.domain.model.Notification;
import org.store.notification.domain.repository.NotificationRepository;

import java.util.UUID;

@Repository
public interface NotificationJpaRepository extends JpaRepository<Notification, UUID>, NotificationRepository {
}
