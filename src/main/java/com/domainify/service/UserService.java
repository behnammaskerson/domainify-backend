package com.domainify.service;

import com.domainify.dto.*;
import com.domainify.entity.User;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.PasswordHistoryRepository;
import com.domainify.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AvatarStorageService avatarStorageService;
    private final PasswordPolicyService passwordPolicyService;
    private final PasswordHistoryRepository passwordHistoryRepository;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AvatarStorageService avatarStorageService,
                       PasswordPolicyService passwordPolicyService,
                       PasswordHistoryRepository passwordHistoryRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.avatarStorageService = avatarStorageService;
        this.passwordPolicyService = passwordPolicyService;
        this.passwordHistoryRepository = passwordHistoryRepository;
    }

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "firstName", "lastName", "email", "phoneCountryCode", "phoneNumber", "role", "enabled", "id",
            "createdAt", "updatedAt", "creatorUsername", "createMethod"
    );

    public PagedResponse<UserDto> list(String firstName,
                                       String lastName,
                                       String email,
                                       User.Role role,
                                       Boolean enabled,
                                       User.CreateMethod createMethod,
                                       Instant createdFrom,
                                       Instant createdTo,
                                       Pageable pageable) {
        Specification<User> spec = buildListSpec(
                firstName, lastName, email, role, enabled, createMethod, createdFrom, createdTo);
        Pageable safePageable = sanitizePageable(pageable);
        Page<UserDto> page = userRepository.findAll(spec, safePageable).map(UserDto::fromUser);
        return PagedResponse.from(page);
    }

    private Specification<User> buildListSpec(String firstName,
                                              String lastName,
                                              String email,
                                              User.Role role,
                                              Boolean enabled,
                                              User.CreateMethod createMethod,
                                              Instant createdFrom,
                                              Instant createdTo) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(firstName)) {
                predicates.add(cb.like(cb.lower(root.get("firstName")),
                        "%" + firstName.trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (StringUtils.hasText(lastName)) {
                predicates.add(cb.like(cb.lower(root.get("lastName")),
                        "%" + lastName.trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (StringUtils.hasText(email)) {
                predicates.add(cb.like(cb.lower(root.get("email")),
                        "%" + email.trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            if (enabled != null) {
                predicates.add(cb.equal(root.get("enabled"), enabled));
            }
            if (createMethod != null) {
                predicates.add(cb.equal(root.get("createMethod"), createMethod));
            }
            if (createdFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            }
            if (createdTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdTo));
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Pageable sanitizePageable(Pageable pageable) {
        int page = Math.max(pageable.getPageNumber(), 0);
        int size = pageable.getPageSize() <= 0 ? 10 : Math.min(pageable.getPageSize(), 100);

        List<Sort.Order> orders = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                continue;
            }
            Sort.Order next = new Sort.Order(order.getDirection(), order.getProperty());
            if (!"createdAt".equals(order.getProperty())
                    && !"updatedAt".equals(order.getProperty())
                    && !"createMethod".equals(order.getProperty())
                    && !"enabled".equals(order.getProperty())
                    && !"id".equals(order.getProperty())) {
                next = next.ignoreCase();
            }
            orders.add(next);
            if ("phoneCountryCode".equals(order.getProperty())) {
                orders.add(new Sort.Order(order.getDirection(), "phoneNumber").ignoreCase());
            }
        }
        if (orders.isEmpty()) {
            orders.add(Sort.Order.asc("firstName").ignoreCase());
            orders.add(Sort.Order.asc("lastName").ignoreCase());
        }
        return PageRequest.of(page, size, Sort.by(orders));
    }

    public UserDto getById(Long id) {
        return UserDto.fromUser(findUser(id));
    }

    @Transactional
    public UserDto create(CreateUserRequest request, User creator) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(ErrorCode.EMAIL_EXISTS);
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
        passwordPolicyService.applyNewPassword(user, request.getPassword());
        user.setRole(request.getRole());
        user.setEnabled(true);
        user.setCreateMethod(User.CreateMethod.ADMIN);
        user.setCreatorUsername(creator != null ? creator.getUsername() : null);

        return UserDto.fromUser(userRepository.save(user));
    }

    @Transactional
    public UserDto update(Long id, UpdateUserRequest request) {
        User user = findUser(id);
        applyIdentity(user, request.getFirstName(), request.getLastName(), request.getEmail());
        return UserDto.fromUser(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        if (currentUser.getId().equals(id)) {
            throw new ApiException(ErrorCode.CANNOT_DELETE_SELF);
        }
        if (!userRepository.existsById(id)) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        avatarStorageService.deleteForUser(id);
        passwordHistoryRepository.deleteByUserId(id);
        userRepository.deleteById(id);
    }

    @Transactional
    public UserDto setEnabled(Long id, boolean enabled, User currentUser) {
        if (currentUser.getId().equals(id) && !enabled) {
            throw new ApiException(ErrorCode.CANNOT_DISABLE_SELF);
        }
        User user = findUser(id);
        user.setEnabled(enabled);
        return UserDto.fromUser(userRepository.save(user));
    }

    @Transactional
    public UserDto setRole(Long id, User.Role role) {
        User user = findUser(id);
        user.setRole(role);
        return UserDto.fromUser(userRepository.save(user));
    }

    @Transactional
    public UserDto updateProfile(User currentUser, UpdateProfileRequest request) {
        User user = findUser(currentUser.getId());
        applyIdentity(user, request.getFirstName(), request.getLastName(), request.getEmail());
        applyPhone(user, request.getPhoneCountryCode(), request.getPhoneNumber());
        return UserDto.fromUser(userRepository.save(user));
    }

    @Transactional
    public void changePassword(User currentUser, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ApiException(ErrorCode.PASSWORDS_MISMATCH);
        }

        User user = findUser(currentUser.getId());
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ApiException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }

        passwordPolicyService.applyNewPassword(user, request.getNewPassword());
        userRepository.save(user);
    }

    @Transactional
    public UserDto updateAvatar(User currentUser, MultipartFile file) {
        User user = findUser(currentUser.getId());
        String avatarUrl = avatarStorageService.store(user, file);
        user.setAvatarUrl(avatarUrl);
        return UserDto.fromUser(userRepository.save(user));
    }

    @Transactional
    public UserDto removeAvatar(User currentUser) {
        User user = findUser(currentUser.getId());
        avatarStorageService.deleteForUser(user.getId());
        user.setAvatarUrl(null);
        return UserDto.fromUser(userRepository.save(user));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }

    private void applyIdentity(User user, String firstName, String lastName, String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (!user.getEmail().equalsIgnoreCase(normalizedEmail) && userRepository.existsByEmail(normalizedEmail)) {
            throw new ApiException(ErrorCode.EMAIL_EXISTS);
        }
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(normalizedEmail);
    }

    private void applyPhone(User user, String phoneCountryCode, String phoneNumber) {
        String country = phoneCountryCode == null ? "" : phoneCountryCode.trim().toUpperCase(Locale.ROOT);
        String number = phoneNumber == null ? "" : phoneNumber.replaceAll("\\D", "");

        if (!StringUtils.hasText(country) || !StringUtils.hasText(number)) {
            user.setPhoneCountryCode(null);
            user.setPhoneNumber(null);
            return;
        }

        user.setPhoneCountryCode(country);
        user.setPhoneNumber(number);
    }
}
