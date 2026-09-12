package com.domainify.service;

import com.domainify.dto.GuestTicketLookupRequest;
import com.domainify.dto.GuestTicketPublicConfigDto;
import com.domainify.dto.GuestTicketSummaryDto;
import com.domainify.dto.TicketAttachmentPolicyDto;
import com.domainify.dto.TicketCategoryDto;
import com.domainify.dto.TicketDetailDto;
import com.domainify.entity.EmailConfig;
import com.domainify.entity.Ticket;
import com.domainify.entity.TicketCategory;
import com.domainify.entity.TicketChannel;
import com.domainify.entity.TicketPriority;
import com.domainify.entity.TicketSettings;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.TicketCategoryRepository;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GuestTicketService {

    private static final Logger log = LoggerFactory.getLogger(GuestTicketService.class);
    private static final int MAX_PER_EMAIL_PER_HOUR = 5;
    private static final int MAX_PER_IP_PER_HOUR = 10;

    private final TicketService ticketService;
    private final TicketCategoryService ticketCategoryService;
    private final TicketCategoryRepository ticketCategoryRepository;
    private final TicketSettingsService ticketSettingsService;
    private final EmailConfigService emailConfigService;
    private final MailService mailService;
    private final CaptchaService captchaService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    private final ConcurrentHashMap<String, RateWindow> emailWindows = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, RateWindow> ipWindows = new ConcurrentHashMap<>();

    public GuestTicketService(
            TicketService ticketService,
            TicketCategoryService ticketCategoryService,
            TicketCategoryRepository ticketCategoryRepository,
            TicketSettingsService ticketSettingsService,
            EmailConfigService emailConfigService,
            MailService mailService,
            CaptchaService captchaService) {
        this.ticketService = ticketService;
        this.ticketCategoryService = ticketCategoryService;
        this.ticketCategoryRepository = ticketCategoryRepository;
        this.ticketSettingsService = ticketSettingsService;
        this.emailConfigService = emailConfigService;
        this.mailService = mailService;
        this.captchaService = captchaService;
    }

    @Transactional(readOnly = true)
    public List<TicketCategoryDto> listCategories() {
        return ticketCategoryService.listActive();
    }

    @Transactional(readOnly = true)
    public TicketAttachmentPolicyDto attachmentPolicy() {
        return ticketSettingsService.getAttachmentPolicy();
    }

    @Transactional(readOnly = true)
    public GuestTicketPublicConfigDto publicConfig() {
        TicketSettings settings = ticketSettingsService.getOrCreate();
        return new GuestTicketPublicConfigDto(
                settings.isGuestTicketCreateEnabled(),
                settings.isGuestTicketAttachmentsEnabled()
        );
    }

    @Transactional
    public void createFromWeb(
            String name,
            String email,
            String subject,
            String description,
            Long categoryId,
            TicketPriority priority,
            MultipartFile[] attachments,
            String captchaToken,
            String captchaAnswer,
            HttpServletRequest request) {
        enforceRateLimit(email, clientIp(request));
        requireGuestCreateEnabled();
        requireCaptchaValidation(captchaToken, captchaAnswer, request);
        requireMailReady();
        MultipartFile[] safeAttachments = requireGuestAttachmentsAllowed(attachments);
        TicketService.GuestTicketCreateResult result = ticketService.createGuest(
                name,
                email,
                subject,
                description,
                categoryId,
                priority != null ? priority : TicketPriority.MEDIUM,
                TicketChannel.WEB,
                safeAttachments
        );
        sendAccessEmail(email, name, result.publicNumber(), result.rawAccessToken());
    }

    @Transactional
    public void createFromContact(
            String name,
            String email,
            String company,
            String subject,
            String message,
            String captchaToken,
            String captchaAnswer,
            HttpServletRequest request) {
        enforceRateLimit(email, clientIp(request));
        requireGuestCreateEnabled();
        requireCaptchaValidation(captchaToken, captchaAnswer, request);
        requireMailReady();
        Long categoryId = resolveContactCategoryId();
        String description = message == null ? "" : message.trim();
        if (StringUtils.hasText(company)) {
            description = description + "\n\n—\nCompany: " + company.trim();
        }
        TicketService.GuestTicketCreateResult result = ticketService.createGuest(
                name,
                email,
                subject,
                description,
                categoryId,
                TicketPriority.MEDIUM,
                TicketChannel.CONTACT,
                null
        );
        sendAccessEmail(email, name, result.publicNumber(), result.rawAccessToken());
    }

    @Transactional
    public TicketDetailDto verifyAndOpen(String rawToken) {
        Ticket ticket = ticketService.requireGuestTicketByToken(rawToken);
        if (!ticket.isGuestEmailVerified()) {
            return ticketService.activateGuestTicket(ticket);
        }
        return ticketService.getDetailForGuest(ticket);
    }

    @Transactional
    public TicketDetailDto getByToken(String rawToken) {
        Ticket ticket = ticketService.requireGuestTicketByToken(rawToken);
        if (!ticket.isGuestEmailVerified()) {
            return ticketService.activateGuestTicket(ticket);
        }
        return ticketService.getDetailForGuest(ticket);
    }

    @Transactional
    public TicketDetailDto reply(String rawToken, String body, MultipartFile[] attachments) {
        Ticket ticket = ticketService.requireGuestTicketByToken(rawToken);
        if (!ticket.isGuestEmailVerified()) {
            ticketService.activateGuestTicket(ticket);
            ticket = ticketService.requireGuestTicketByToken(rawToken);
        }
        MultipartFile[] safeAttachments = requireGuestAttachmentsAllowed(attachments);
        return ticketService.replyAsGuest(ticket, body, safeAttachments);
    }

    /**
     * Email a guest their tickets with a fresh per-ticket magic link each.
     * Always returns the same generic message (no email enumeration).
     */
    @Transactional
    public void requestListByEmail(GuestTicketLookupRequest request, HttpServletRequest httpRequest) {
        requireGuestCreateEnabled();
        String email = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            throw new ApiException(ErrorCode.TICKET_GUEST_EMAIL_INVALID);
        }
        enforceRateLimit(email, clientIp(httpRequest));
        requireCaptchaValidation(request.getCaptchaToken(), request.getCaptchaAnswer(), httpRequest);
        requireMailReady();

        List<Ticket> tickets = ticketService.findActiveGuestTicketsByEmail(email);
        if (tickets.isEmpty()) {
            // Do not reveal whether the email has tickets.
            return;
        }

        String guestName = tickets.stream()
                .map(Ticket::getGuestName)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(email);

        StringBuilder links = new StringBuilder();
        for (Ticket ticket : tickets) {
            String rawToken = ticketService.renewGuestAccessToken(ticket);
            String number = StringUtils.hasText(ticket.getPublicNumber()) ? ticket.getPublicNumber() : ("#" + ticket.getId());
            String subject = StringUtils.hasText(ticket.getSubject()) ? ticket.getSubject() : "Support request";
            links.append("- ")
                    .append(number)
                    .append(" · ")
                    .append(subject)
                    .append(" (")
                    .append(ticket.getStatus())
                    .append(")\n  ")
                    .append(buildAccessLink(rawToken))
                    .append("\n\n");
        }

        sendListEmail(email, guestName, links.toString(), tickets.size());
    }

    /**
     * List all guest tickets for the email bound to a valid single-ticket access token.
     * Sibling rows do not include tokens; use {@link #openRelatedTicket} to mint one.
     */
    @Transactional
    public List<GuestTicketSummaryDto> listByAccessToken(String rawToken) {
        Ticket current = ticketService.requireGuestTicketByToken(rawToken);
        if (!current.isGuestEmailVerified()) {
            ticketService.activateGuestTicket(current);
            current = ticketService.requireGuestTicketByToken(rawToken);
        }
        String email = current.getGuestEmail();
        List<Ticket> tickets = ticketService.findActiveGuestTicketsByEmail(email);
        List<GuestTicketSummaryDto> result = new ArrayList<>(tickets.size());
        for (Ticket ticket : tickets) {
            GuestTicketSummaryDto dto = toSummary(ticket);
            boolean isCurrent = ticket.getId().equals(current.getId());
            dto.setCurrent(isCurrent);
            if (isCurrent) {
                dto.setAccessToken(rawToken.trim());
            }
            result.add(dto);
        }
        return result;
    }

    /**
     * Mint a fresh per-ticket access token for a sibling guest ticket owned by the same email.
     */
    @Transactional
    public GuestTicketSummaryDto openRelatedTicket(String rawToken, Long relatedTicketId) {
        Ticket current = ticketService.requireGuestTicketByToken(rawToken);
        if (!current.isGuestEmailVerified()) {
            ticketService.activateGuestTicket(current);
            current = ticketService.requireGuestTicketByToken(rawToken);
        }
        if (relatedTicketId == null) {
            throw new ApiException(ErrorCode.TICKET_NOT_FOUND);
        }
        if (current.getId().equals(relatedTicketId)) {
            GuestTicketSummaryDto dto = toSummary(current);
            dto.setCurrent(true);
            dto.setAccessToken(rawToken.trim());
            return dto;
        }

        Ticket related = ticketService.findActiveGuestTicketsByEmail(current.getGuestEmail()).stream()
                .filter(t -> relatedTicketId.equals(t.getId()))
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_NOT_FOUND));

        if (!related.isGuestEmailVerified()) {
            // Opening from a verified sibling proves email ownership.
            related.setGuestEmailVerified(true);
        }
        String newToken = ticketService.renewGuestAccessToken(related);
        GuestTicketSummaryDto dto = toSummary(related);
        dto.setCurrent(false);
        dto.setAccessToken(newToken);
        return dto;
    }

    private GuestTicketSummaryDto toSummary(Ticket ticket) {
        GuestTicketSummaryDto dto = new GuestTicketSummaryDto();
        dto.setId(ticket.getId());
        dto.setPublicNumber(ticket.getPublicNumber());
        dto.setSubject(ticket.getSubject());
        dto.setStatus(ticket.getStatus());
        dto.setPriority(ticket.getPriority());
        dto.setCreatedAt(ticket.getCreatedAt());
        dto.setUpdatedAt(ticket.getUpdatedAt());
        return dto;
    }

    private void sendListEmail(String email, String name, String linksBlock, int count) {
        EmailConfig config = emailConfigService.getOrCreate();
        String display = StringUtils.hasText(name) ? name.trim() : email;
        String subject = "Your Domainify support tickets";
        String body = """
                Hello %s,

                Here are your open support tickets (%d). Each link opens only that ticket:

                %s
                If you did not request this list, you can ignore this email.

                — Domainify
                """.formatted(display, count, linksBlock);
        try {
            mailService.sendEmail(config, email, subject, body);
        } catch (MessagingException ex) {
            log.warn("Failed to send guest ticket list email to {}: {}", email, ex.getMessage());
            throw new ApiException(ErrorCode.EMAIL_CONFIG_INVALID);
        }
    }

    private void requireGuestCreateEnabled() {
        if (!ticketSettingsService.getOrCreate().isGuestTicketCreateEnabled()) {
            throw new ApiException(ErrorCode.TICKET_GUEST_CREATE_DISABLED);
        }
    }

    private MultipartFile[] requireGuestAttachmentsAllowed(MultipartFile[] attachments) {
        if (attachments == null || attachments.length == 0) {
            return attachments;
        }
        boolean hasFile = false;
        for (MultipartFile file : attachments) {
            if (file != null && !file.isEmpty()) {
                hasFile = true;
                break;
            }
        }
        if (!hasFile) {
            return attachments;
        }
        if (!ticketSettingsService.getOrCreate().isGuestTicketAttachmentsEnabled()) {
            throw new ApiException(ErrorCode.TICKET_GUEST_ATTACHMENTS_DISABLED);
        }
        return attachments;
    }

    private void requireCaptchaValidation(String captchaToken, String captchaAnswer, HttpServletRequest request) {
        if (!captchaService.isCaptchaEnabled()) {
            return;
        }
        if (!StringUtils.hasText(captchaToken) || !StringUtils.hasText(captchaAnswer)) {
            throw new ApiException(ErrorCode.CAPTCHA_REQUIRED);
        }
        if (!captchaService.validateCaptcha(captchaToken, captchaAnswer, request)) {
            throw new ApiException(ErrorCode.CAPTCHA_INVALID);
        }
    }

    private Long resolveContactCategoryId() {
        TicketSettings settings = ticketSettingsService.getOrCreate();
        if (settings.getContactDefaultCategoryId() != null) {
            try {
                return ticketCategoryService.requireActiveCategory(settings.getContactDefaultCategoryId()).getId();
            } catch (ApiException ignored) {
                // fall through
            }
        }
        List<TicketCategory> active = ticketCategoryRepository.findByActiveTrueOrderBySortOrderAscNameAsc();
        if (active.isEmpty()) {
            throw new ApiException(ErrorCode.TICKET_GUEST_CATEGORY_UNAVAILABLE);
        }
        return active.get(0).getId();
    }

    private void requireMailReady() {
        if (!emailConfigService.isEnabled()) {
            throw new ApiException(ErrorCode.EMAIL_VERIFICATION_DISABLED);
        }
        EmailConfig config = emailConfigService.getOrCreate();
        if (!StringUtils.hasText(config.getHost()) || !StringUtils.hasText(config.getFromEmail())) {
            throw new ApiException(ErrorCode.EMAIL_CONFIG_INVALID);
        }
    }

    private void sendAccessEmail(String email, String name, String publicNumber, String rawToken) {
        EmailConfig config = emailConfigService.getOrCreate();
        String link = buildAccessLink(rawToken);
        String display = StringUtils.hasText(name) ? name.trim() : email;
        String subject = "Confirm your Domainify support request";
        String body = """
                Hello %s,

                We received your support request%s.

                Open this link to confirm your email and view your ticket:

                %s

                If you did not submit this request, you can ignore this email.

                — Domainify
                """.formatted(
                display,
                StringUtils.hasText(publicNumber) ? " (" + publicNumber + ")" : "",
                link
        );
        try {
            mailService.sendEmail(config, email.trim().toLowerCase(), subject, body);
        } catch (MessagingException ex) {
            log.warn("Failed to send guest ticket access email to {}: {}", email, ex.getMessage());
            throw new ApiException(ErrorCode.EMAIL_CONFIG_INVALID);
        }
    }

    private String buildAccessLink(String rawToken) {
        String base = frontendUrl == null ? "http://localhost:4200" : frontendUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/support/t/" + rawToken;
    }

    private void enforceRateLimit(String email, String ip) {
        Instant now = Instant.now();
        pruneWindows(emailWindows, now);
        pruneWindows(ipWindows, now);

        String emailKey = email == null ? "" : email.trim().toLowerCase();
        if (StringUtils.hasText(emailKey)) {
            RateWindow window = emailWindows.computeIfAbsent(emailKey, k -> new RateWindow(now));
            if (!window.tryAcquire(now, MAX_PER_EMAIL_PER_HOUR)) {
                throw new ApiException(ErrorCode.TICKET_GUEST_RATE_LIMITED);
            }
        }
        if (StringUtils.hasText(ip)) {
            RateWindow window = ipWindows.computeIfAbsent(ip, k -> new RateWindow(now));
            if (!window.tryAcquire(now, MAX_PER_IP_PER_HOUR)) {
                throw new ApiException(ErrorCode.TICKET_GUEST_RATE_LIMITED);
            }
        }
    }

    private static void pruneWindows(ConcurrentHashMap<String, RateWindow> map, Instant now) {
        Iterator<Map.Entry<String, RateWindow>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, RateWindow> entry = it.next();
            if (entry.getValue().isExpired(now)) {
                it.remove();
            }
        }
    }

    private static String clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }

    private static final class RateWindow {
        private Instant windowStart;
        private int count;

        private RateWindow(Instant now) {
            this.windowStart = now;
            this.count = 0;
        }

        private synchronized boolean tryAcquire(Instant now, int max) {
            if (now.isAfter(windowStart.plusSeconds(3600))) {
                windowStart = now;
                count = 0;
            }
            if (count >= max) {
                return false;
            }
            count++;
            return true;
        }

        private boolean isExpired(Instant now) {
            return now.isAfter(windowStart.plusSeconds(7200));
        }
    }
}
