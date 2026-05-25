package org.store.contact.presentation;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.store.contact.application.dto.ContactMessageRequest;
import org.store.contact.application.dto.ContactMessageResponse;
import org.store.contact.application.dto.ContactReplyRequest;
import org.store.contact.application.service.IContactMessageService;

import java.util.UUID;

@RestController
@RequestMapping(ContactMessageController.BASE_PATH)
public class ContactMessageController {

    public static final String BASE_PATH = "/api/v1/contact";

    private final IContactMessageService contactMessageService;

    public ContactMessageController(IContactMessageService contactMessageService) {
        this.contactMessageService = contactMessageService;
    }

    /** Public endpoint — no authentication required. */
    @PostMapping
    public ResponseEntity<ContactMessageResponse> submit(
            @Valid @RequestBody ContactMessageRequest contactMessageRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(contactMessageService.submit(contactMessageRequest));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CONTACT_READ')")
    public ResponseEntity<Page<ContactMessageResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(contactMessageService.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CONTACT_READ')")
    public ResponseEntity<ContactMessageResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(contactMessageService.findById(id));
    }

    @PatchMapping("/{id}/reply")
    @PreAuthorize("hasAuthority('CONTACT_RESPOND')")
    public ResponseEntity<ContactMessageResponse> reply(@PathVariable UUID id,
                                                        @Valid @RequestBody ContactReplyRequest contactReplyRequest) {
        return ResponseEntity.ok(contactMessageService.reply(id, contactReplyRequest));
    }
}
