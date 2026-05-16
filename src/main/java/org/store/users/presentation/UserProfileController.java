package org.store.users.presentation;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.store.common.dto.ImageDownloadResponse;
import org.store.security.application.dto.ChangePasswordRequest;
import org.store.users.application.dto.UserProfileResponse;
import org.store.users.application.dto.UserProfileUpdateRequest;
import org.store.users.application.service.IUserProfileService;

@RestController
@RequestMapping(UserProfileController.BASE_PATH)
public class UserProfileController {

    public static final String BASE_PATH = "/api/v1/users/me";

    private final IUserProfileService userProfileService;

    public UserProfileController(IUserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> getCurrentProfile() {
        return ResponseEntity.ok(userProfileService.getCurrentProfile());
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> updateCurrentProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        return ResponseEntity.ok(userProfileService.updateCurrentProfile(request));
    }

    @PostMapping("/change-password")
    @PreAuthorize("hasAuthority('AUTH_CHANGE_PASSWORD')")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userProfileService.changePassword(request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> uploadPhoto(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(userProfileService.uploadPhoto(file));
    }

    @GetMapping("/photo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> getPhoto() {
        ImageDownloadResponse download = userProfileService.getPhoto();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .body(download.content());
    }

    @DeleteMapping("/photo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deletePhoto() {
        userProfileService.deletePhoto();
        return ResponseEntity.noContent().build();
    }
}
