package com.domainify.controller;

import com.domainify.dto.*;
import com.domainify.entity.User;
import com.domainify.service.MessageService;
import com.domainify.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final MessageService messageService;

    public UserController(UserService userService, MessageService messageService) {
        this.userService = userService;
        this.messageService = messageService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.getById(user.getId()));
    }

    @PostMapping("/me/send-verification-email")
    public ResponseEntity<ApiResponse<Void>> sendVerificationEmail(@AuthenticationPrincipal User user) {
        userService.sendVerificationEmail(user);
        return ResponseEntity.ok(ApiResponse.success(messageService.get("auth.verification_email_sent"), null));
    }

    @PostMapping("/me/send-phone-verification")
    public ResponseEntity<ApiResponse<Void>> sendPhoneVerification(@AuthenticationPrincipal User user) {
        userService.sendPhoneVerificationCode(user);
        return ResponseEntity.ok(ApiResponse.success(messageService.get("auth.verification_sms_sent"), null));
    }

    @PostMapping("/me/verify-phone")
    public ResponseEntity<UserDto> verifyPhone(@AuthenticationPrincipal User user,
                                             @Valid @RequestBody VerifyPhoneRequest request) {
        return ResponseEntity.ok(userService.verifyPhone(user, request.getCode()));
    }

    @PatchMapping("/me/email-notifications")
    public ResponseEntity<UserDto> setEmailNotifications(@AuthenticationPrincipal User user,
                                                         @Valid @RequestBody UpdateEmailNotificationsRequest request) {
        return ResponseEntity.ok(userService.setEmailNotificationsEnabled(user, Boolean.TRUE.equals(request.getEnabled())));
    }

    @PatchMapping("/me/sms-notifications")
    public ResponseEntity<UserDto> setSmsNotifications(@AuthenticationPrincipal User user,
                                                       @Valid @RequestBody UpdateEmailNotificationsRequest request) {
        return ResponseEntity.ok(userService.setSmsNotificationsEnabled(user, Boolean.TRUE.equals(request.getEnabled())));
    }

    @PatchMapping("/me/preferred-language")
    public ResponseEntity<UserDto> setPreferredLanguage(@AuthenticationPrincipal User user,
                                                        @Valid @RequestBody UpdatePreferredLanguageRequest request) {
        return ResponseEntity.ok(userService.setPreferredLanguage(user, request.getLanguage()));
    }

    @PutMapping("/me")
    public ResponseEntity<UpdateProfileResponse> updateProfile(@AuthenticationPrincipal User user,
                                                               @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(user, request));
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@AuthenticationPrincipal User user,
                                                            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(user, request);
        return ResponseEntity.ok(ApiResponse.success(messageService.get("auth.password_changed"), null));
    }

    @PostMapping(value = "/me/avatar", consumes = "multipart/form-data")
    public ResponseEntity<UserDto> uploadAvatar(@AuthenticationPrincipal User user,
                                                @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(userService.updateAvatar(user, file));
    }

    @DeleteMapping("/me/avatar")
    public ResponseEntity<UserDto> removeAvatar(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.removeAvatar(user));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<UserDto>> listUsers(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) User.Role role,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) User.CreateMethod createMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @PageableDefault(size = 10, sort = {"firstName", "lastName"}, direction = Sort.Direction.ASC)
            Pageable pageable) {
        return ResponseEntity.ok(userService.list(
                firstName, lastName, email, role, enabled, createMethod, createdFrom, createdTo, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request,
                                              @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request, currentUser));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id,
                                              @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> adminSetPassword(@PathVariable Long id,
                                                              @Valid @RequestBody AdminSetPasswordRequest request) {
        userService.adminSetPassword(id, request);
        return ResponseEntity.ok(ApiResponse.success(messageService.get("auth.password_changed"), null));
    }

    @PatchMapping("/{id}/email-notifications")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> setUserEmailNotifications(@PathVariable Long id,
                                                             @Valid @RequestBody UpdateEmailNotificationsRequest request) {
        return ResponseEntity.ok(userService.setEmailNotificationsEnabled(id, Boolean.TRUE.equals(request.getEnabled())));
    }

    @PatchMapping("/{id}/sms-notifications")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> setUserSmsNotifications(@PathVariable Long id,
                                                           @Valid @RequestBody UpdateEmailNotificationsRequest request) {
        return ResponseEntity.ok(userService.setSmsNotificationsEnabled(id, Boolean.TRUE.equals(request.getEnabled())));
    }

    @PatchMapping("/{id}/enabled")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> setEnabled(@PathVariable Long id,
                                              @Valid @RequestBody UpdateEnabledRequest request,
                                              @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userService.setEnabled(id, request.getEnabled(), currentUser));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> setRole(@PathVariable Long id,
                                           @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(userService.setRole(id, request.getRole()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id,
                                                        @AuthenticationPrincipal User currentUser) {
        userService.delete(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(messageService.get("users.deleted"), null));
    }
}
