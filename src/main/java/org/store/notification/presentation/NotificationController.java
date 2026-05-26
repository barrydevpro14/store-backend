package org.store.notification.presentation;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.store.notification.application.dto.NotificationFilter;
import org.store.notification.application.dto.NotificationResponse;
import org.store.notification.application.service.INotificationService;
import org.store.notification.domain.enums.NotificationStatut;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(NotificationController.BASE_PATH)
public class NotificationController {

    public static final String BASE_PATH = "/api/v1/notifications";

    private final INotificationService notificationService;

    public NotificationController(INotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<NotificationResponse>> list(
            @RequestParam(required = false) NotificationStatut statut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdStartDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdEndDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(notificationService.findAllForCurrentUser(
                new NotificationFilter(statut, createdStartDate, createdEndDate, page, size)));
    }

    @GetMapping("/count-unread")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> countUnread() {
        return ResponseEntity.ok(Map.of("count", notificationService.countUnreadForCurrentUser()));
    }

    @PatchMapping("/{id}/lue")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable UUID id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @PatchMapping("/lue-tout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.noContent().build();
    }
}
