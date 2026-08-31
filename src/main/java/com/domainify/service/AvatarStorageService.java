package com.domainify.service;

import com.domainify.entity.User;
import com.domainify.entity.UserAvatar;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.UserAvatarRepository;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

@Service
public class AvatarStorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_BYTES = 5 * 1024 * 1024;

    private final UserAvatarRepository userAvatarRepository;

    public AvatarStorageService(UserAvatarRepository userAvatarRepository) {
        this.userAvatarRepository = userAvatarRepository;
    }

    /**
     * Stores avatar bytes in the database and returns a stable public URL path.
     * Includes a cache-busting query param so browsers refresh after replacement.
     */
    @Transactional
    public String store(User user, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_AVATAR);
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ApiException(ErrorCode.INVALID_AVATAR);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new ApiException(ErrorCode.INVALID_AVATAR);
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ApiException(ErrorCode.AVATAR_UPLOAD_FAILED);
        }
        if (bytes.length == 0 || bytes.length > MAX_BYTES) {
            throw new ApiException(ErrorCode.INVALID_AVATAR);
        }

        UserAvatar avatar = userAvatarRepository.findById(user.getId()).orElseGet(() -> {
            UserAvatar created = new UserAvatar();
            created.setUserId(user.getId());
            return created;
        });
        avatar.setData(bytes);
        avatar.setContentType(contentType);
        userAvatarRepository.save(avatar);

        return "/files/avatars/" + user.getId() + "?v=" + System.currentTimeMillis();
    }

    @Transactional
    public void deleteForUser(Long userId) {
        if (userId != null && userAvatarRepository.existsById(userId)) {
            userAvatarRepository.deleteById(userId);
        }
    }

    @Transactional(readOnly = true)
    public Optional<StoredAvatar> load(Long userId) {
        return userAvatarRepository.findById(userId)
                .filter(avatar -> avatar.getData() != null && avatar.getData().length > 0)
                .map(avatar -> new StoredAvatar(
                        new ByteArrayResource(avatar.getData()),
                        resolveMediaType(avatar.getContentType())
                ));
    }

    private MediaType resolveMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.IMAGE_JPEG;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            return MediaType.IMAGE_JPEG;
        }
    }

    public record StoredAvatar(Resource resource, MediaType mediaType) {}
}
