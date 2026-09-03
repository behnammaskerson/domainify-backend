package com.domainify.service;

import com.domainify.entity.Ticket;
import com.domainify.entity.TicketMessage;
import com.domainify.entity.TicketMention;
import com.domainify.entity.User;
import com.domainify.repository.TicketMentionRepository;
import com.domainify.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TicketMentionService {

    private static final Pattern EMAIL_MENTION_PATTERN = Pattern.compile(
            "@([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern HANDLE_MENTION_PATTERN = Pattern.compile(
            "@([a-zA-Z][a-zA-Z0-9._-]{0,63})"
    );

    private final TicketMentionRepository ticketMentionRepository;
    private final UserRepository userRepository;

    public TicketMentionService(
            TicketMentionRepository ticketMentionRepository,
            UserRepository userRepository) {
        this.ticketMentionRepository = ticketMentionRepository;
        this.userRepository = userRepository;
    }

    public void syncMentions(Ticket ticket, TicketMessage message, String body, User author) {
        if (ticket == null || message == null || message.getId() == null) {
            return;
        }

        ticketMentionRepository.deleteByMessageId(message.getId());

        if (!message.isInternalNote() || !StringUtils.hasText(body)) {
            return;
        }

        Set<Long> mentionedUserIds = resolveMentionedUserIds(body);
        if (mentionedUserIds.isEmpty()) {
            return;
        }

        Long authorId = author != null ? author.getId() : null;
        for (Long userId : mentionedUserIds) {
            if (authorId != null && authorId.equals(userId)) {
                continue;
            }
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || user.getRole() != User.Role.ADMIN || !user.isEnabled()) {
                continue;
            }
            TicketMention mention = new TicketMention();
            mention.setTicket(ticket);
            mention.setMentionedUser(user);
            mention.setMessage(message);
            ticketMentionRepository.save(mention);
        }
    }

    private Set<Long> resolveMentionedUserIds(String body) {
        Set<Long> userIds = new HashSet<>();
        Set<String> seenEmails = new HashSet<>();

        Matcher emailMatcher = EMAIL_MENTION_PATTERN.matcher(body);
        while (emailMatcher.find()) {
            String email = emailMatcher.group(1).trim().toLowerCase(Locale.ROOT);
            if (!seenEmails.add(email)) {
                continue;
            }
            userRepository.findByEmailIgnoreCase(email).ifPresent(user -> userIds.add(user.getId()));
        }

        Map<String, Long> handleIndex = buildAdminHandleIndex();
        Matcher handleMatcher = HANDLE_MENTION_PATTERN.matcher(body);
        while (handleMatcher.find()) {
            String token = handleMatcher.group(1);
            if (token.contains("@")) {
                continue;
            }
            String handle = token.toLowerCase(Locale.ROOT);
            Long userId = handleIndex.get(handle);
            if (userId != null) {
                userIds.add(userId);
            }
        }

        return userIds;
    }

    private Map<String, Long> buildAdminHandleIndex() {
        List<User> admins = userRepository.findByRoleAndEnabledTrueOrderByFirstNameAscLastNameAsc(User.Role.ADMIN);
        Map<String, Long> index = new HashMap<>();
        for (User admin : admins) {
            String handle = toMentionHandle(displayName(admin));
            if (StringUtils.hasText(handle)) {
                index.putIfAbsent(handle, admin.getId());
            }
            String emailLocal = emailLocalPart(admin.getEmail());
            if (StringUtils.hasText(emailLocal)) {
                index.putIfAbsent(emailLocal, admin.getId());
            }
        }
        return index;
    }

    static String toMentionHandle(String displayName) {
        if (!StringUtils.hasText(displayName)) {
            return "";
        }
        String normalized = displayName.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", ".");
        return normalized.replaceAll("^\\.+|\\.+$", "");
    }

    private String emailLocalPart(String email) {
        if (!StringUtils.hasText(email)) {
            return "";
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "";
        }
        return email.substring(0, at).trim().toLowerCase(Locale.ROOT);
    }

    private String displayName(User user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String last = user.getLastName() == null ? "" : user.getLastName().trim();
        String full = (first + " " + last).trim();
        return StringUtils.hasText(full) ? full : user.getEmail();
    }
}
