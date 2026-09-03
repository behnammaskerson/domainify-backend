package com.domainify.service;

import com.domainify.entity.Ticket;
import com.domainify.entity.TicketMessage;
import com.domainify.entity.TicketMention;
import com.domainify.entity.User;
import com.domainify.repository.TicketMentionRepository;
import com.domainify.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TicketMentionService {

    private static final Pattern MENTION_PATTERN = Pattern.compile(
            "@([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})",
            Pattern.CASE_INSENSITIVE
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
        if (ticket == null || message == null || !StringUtils.hasText(body)) {
            return;
        }

        Set<String> emails = extractMentionEmails(body);
        if (emails.isEmpty()) {
            return;
        }

        Long authorId = author != null ? author.getId() : null;
        for (String email : emails) {
            userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
                if (authorId != null && authorId.equals(user.getId())) {
                    return;
                }
                if (ticketMentionRepository.existsByTicketIdAndMentionedUserIdAndMessageId(
                        ticket.getId(), user.getId(), message.getId())) {
                    return;
                }
                TicketMention mention = new TicketMention();
                mention.setTicket(ticket);
                mention.setMentionedUser(user);
                mention.setMessage(message);
                ticketMentionRepository.save(mention);
            });
        }
    }

    private Set<String> extractMentionEmails(String body) {
        Set<String> emails = new HashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(body);
        while (matcher.find()) {
            emails.add(matcher.group(1).trim().toLowerCase(Locale.ROOT));
        }
        return emails;
    }
}
