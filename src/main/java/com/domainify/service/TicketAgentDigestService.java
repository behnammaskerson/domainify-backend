package com.domainify.service;

import com.domainify.dto.TicketInboxFilter;
import com.domainify.entity.EmailConfig;
import com.domainify.entity.TicketInboxView;
import com.domainify.entity.TicketSettings;
import com.domainify.entity.User;
import com.domainify.repository.TicketSettingsRepository;
import com.domainify.repository.UserRepository;
import com.domainify.util.UserPreferredLanguage;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * Sends a once-daily ticket summary email to opted-in agents.
 * Gate order: SMTP → org digest switch → user email + digest opt-in.
 */
@Service
public class TicketAgentDigestService {

    private static final Logger log = LoggerFactory.getLogger(TicketAgentDigestService.class);

    private final EmailConfigService emailConfigService;
    private final MailService mailService;
    private final MessageService messageService;
    private final TicketSettingsRepository ticketSettingsRepository;
    private final UserRepository userRepository;
    private final AdminTicketService adminTicketService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public TicketAgentDigestService(
            EmailConfigService emailConfigService,
            MailService mailService,
            MessageService messageService,
            TicketSettingsRepository ticketSettingsRepository,
            UserRepository userRepository,
            AdminTicketService adminTicketService) {
        this.emailConfigService = emailConfigService;
        this.mailService = mailService;
        this.messageService = messageService;
        this.ticketSettingsRepository = ticketSettingsRepository;
        this.userRepository = userRepository;
        this.adminTicketService = adminTicketService;
    }

    @Transactional(readOnly = true)
    public int processDailyDigest() {
        if (!emailConfigService.isEnabled()) {
            return 0;
        }
        if (!isOrgDigestEnabled()) {
            return 0;
        }

        EmailConfig config;
        try {
            config = emailConfigService.getOrCreate();
        } catch (Exception ex) {
            log.warn("Agent digest skipped: email config unavailable: {}", ex.getMessage());
            return 0;
        }

        List<User> agents = userRepository.findByRoleAndEnabledTrueOrderByFirstNameAscLastNameAsc(User.Role.ADMIN);
        int sent = 0;
        for (User agent : agents) {
            if (sendDigestIfEligible(config, agent)) {
                sent++;
            }
        }
        return sent;
    }

    private boolean isOrgDigestEnabled() {
        return ticketSettingsRepository.findById(TicketSettings.SINGLETON_ID)
                .map(TicketSettings::isAgentDigestEnabled)
                .orElse(false);
    }

    private boolean sendDigestIfEligible(EmailConfig config, User agent) {
        if (agent == null || !agent.isEnabled() || agent.getRole() != User.Role.ADMIN) {
            return false;
        }
        if (!agent.isEmailNotificationsEnabled() || !agent.isTicketDigestEmailEnabled()) {
            return false;
        }
        if (!StringUtils.hasText(agent.getEmail())) {
            return false;
        }

        try {
            Locale locale = UserPreferredLanguage.toLocale(agent.getPreferredLanguage());
            long mine = adminTicketService.countInbox(agent, TicketInboxView.MINE, new TicketInboxFilter());
            TicketInboxFilter overdueMine = new TicketInboxFilter();
            overdueMine.setAssigneeId(agent.getId());
            long overdue = adminTicketService.countInbox(agent, TicketInboxView.OVERDUE, overdueMine);
            long mentions = adminTicketService.countInbox(agent, TicketInboxView.MENTIONS, new TicketInboxFilter());
            long watching = adminTicketService.countInbox(agent, TicketInboxView.WATCHING, new TicketInboxFilter());
            long unassigned = adminTicketService.countInbox(agent, TicketInboxView.UNASSIGNED, new TicketInboxFilter());

            String subject = messageService.get("notification.email.subject.agent_digest", locale);
            String body = buildBody(agent, locale, mine, overdue, mentions, watching, unassigned);
            mailService.sendEmail(config, agent.getEmail(), subject, body);
            return true;
        } catch (MessagingException ex) {
            log.warn("Agent digest email failed for user {}: {}", agent.getId(), ex.getMessage());
            return false;
        } catch (Exception ex) {
            log.warn("Agent digest email skipped for user {}: {}", agent.getId(), ex.getMessage());
            return false;
        }
    }

    private String buildBody(
            User agent,
            Locale locale,
            long mine,
            long overdue,
            long mentions,
            long watching,
            long unassigned) {
        String helloName = displayName(agent, locale);
        String inboxLink = inboxLink();
        return messageService.get("notification.email.hello", new Object[]{helloName}, locale)
                + "\n\n"
                + messageService.get("notification.email.event.agent_digest_intro", locale)
                + "\n\n"
                + messageService.get("notification.email.agent_digest.mine", new Object[]{mine}, locale)
                + "\n"
                + messageService.get("notification.email.agent_digest.overdue", new Object[]{overdue}, locale)
                + "\n"
                + messageService.get("notification.email.agent_digest.mentions", new Object[]{mentions}, locale)
                + "\n"
                + messageService.get("notification.email.agent_digest.watching", new Object[]{watching}, locale)
                + "\n"
                + messageService.get("notification.email.agent_digest.unassigned", new Object[]{unassigned}, locale)
                + "\n\n"
                + messageService.get("notification.email.agent_digest.open_inbox", locale)
                + "\n"
                + inboxLink
                + "\n\n"
                + messageService.get("notification.email.footer", locale)
                + "\n\n"
                + messageService.get("notification.email.signature", locale);
    }

    private String inboxLink() {
        String base = frontendUrl == null ? "http://localhost:4200" : frontendUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/admin/tickets/inbox";
    }

    private String displayName(User user, Locale locale) {
        if (user == null) {
            return messageService.get("notification.someone", locale);
        }
        String first = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String last = user.getLastName() != null ? user.getLastName().trim() : "";
        String combined = (first + " " + last).trim();
        if (StringUtils.hasText(combined)) {
            return combined;
        }
        if (StringUtils.hasText(user.getEmail())) {
            return user.getEmail();
        }
        return messageService.get("notification.someone", locale);
    }
}
