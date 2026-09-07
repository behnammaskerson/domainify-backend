package com.domainify.service;

import com.domainify.dto.DomainRenewalTestRequest;
import com.domainify.dto.DomainRenewalTestResultDto;
import com.domainify.dto.SmsBulkSendRequest;
import com.domainify.dto.SmsBulkSendResultDto;
import com.domainify.entity.Domain;
import com.domainify.entity.DomainRenewalReminder;
import com.domainify.entity.DomainSettings;
import com.domainify.entity.DomainStatus;
import com.domainify.entity.EmailConfig;
import com.domainify.entity.User;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.DomainRenewalReminderRepository;
import com.domainify.repository.DomainRepository;
import com.domainify.util.PhoneSmsUtil;
import com.domainify.util.UserPreferredLanguage;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Sends domain renewal reminders (in-app + email + SMS) at configured day windows before expiry.
 * Supports per-domain custom windows; deduped per domain / window / expiry date.
 */
@Service
public class DomainRenewalReminderService {

    private static final Logger log = LoggerFactory.getLogger(DomainRenewalReminderService.class);

    private static final Set<DomainStatus> REMINDABLE_STATUSES = EnumSet.of(
            DomainStatus.ACTIVE,
            DomainStatus.PENDING
    );

    private final DomainRepository domainRepository;
    private final DomainRenewalReminderRepository reminderRepository;
    private final DomainSettingsService domainSettingsService;
    private final NotificationService notificationService;
    private final EmailConfigService emailConfigService;
    private final MailService mailService;
    private final SmsService smsService;
    private final SmsConfigService smsConfigService;
    private final MessageService messageService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public DomainRenewalReminderService(
            DomainRepository domainRepository,
            DomainRenewalReminderRepository reminderRepository,
            DomainSettingsService domainSettingsService,
            NotificationService notificationService,
            EmailConfigService emailConfigService,
            MailService mailService,
            SmsService smsService,
            SmsConfigService smsConfigService,
            MessageService messageService) {
        this.domainRepository = domainRepository;
        this.reminderRepository = reminderRepository;
        this.domainSettingsService = domainSettingsService;
        this.notificationService = notificationService;
        this.emailConfigService = emailConfigService;
        this.mailService = mailService;
        this.smsService = smsService;
        this.smsConfigService = smsConfigService;
        this.messageService = messageService;
    }

    @Transactional
    public int processDueReminders() {
        DomainSettings settings = domainSettingsService.getOrCreate();
        boolean inApp = settings.isRenewalInAppEnabled();
        boolean email = settings.isRenewalEmailEnabled();
        boolean sms = settings.isRenewalSmsEnabled();
        if (!inApp && !email && !sms) {
            return 0;
        }

        LocalDate today = LocalDate.now();
        List<Domain> customDomains = domainRepository.findWithCustomRenewalWindows(REMINDABLE_STATUSES);
        int horizonDays = domainSettingsService.maxHorizonDays(settings, customDomains);
        LocalDate horizon = today.plusDays(horizonDays);
        List<Domain> candidates = domainRepository.findExpiringBetweenAndStatusIn(
                today, horizon, REMINDABLE_STATUSES);

        int sent = 0;
        for (Domain domain : candidates) {
            if (domain.getExpiresAt() == null) {
                continue;
            }
            long daysUntil = ChronoUnit.DAYS.between(today, domain.getExpiresAt());
            if (daysUntil < 1 || daysUntil > Integer.MAX_VALUE) {
                continue;
            }
            int windowDays = (int) daysUntil;
            List<Integer> effective = domainSettingsService.resolveWindowsForDomain(domain, settings);
            if (!effective.contains(windowDays)) {
                continue;
            }
            if (sendReminderIfNeeded(domain, windowDays, inApp, email, sms)) {
                sent++;
            }
        }
        return sent;
    }

    /**
     * Admin test send — does not write dedupe rows. In-app/SMS go to the admin; email to {@code toEmail} or admin email.
     */
    @Transactional
    public DomainRenewalTestResultDto sendTest(User admin, DomainRenewalTestRequest request) {
        if (admin == null || admin.getId() == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        if (request == null) {
            throw new ApiException(ErrorCode.DOMAIN_RENEWAL_SETTINGS_INVALID);
        }
        boolean wantInApp = Boolean.TRUE.equals(request.getSendInApp());
        boolean wantEmail = Boolean.TRUE.equals(request.getSendEmail());
        boolean wantSms = Boolean.TRUE.equals(request.getSendSms());
        if (!wantInApp && !wantEmail && !wantSms) {
            return new DomainRenewalTestResultDto(false, "Select at least one channel", List.of());
        }

        int windowDays = request.getWindowDays() != null ? request.getWindowDays() : 30;
        String domainName = StringUtils.hasText(request.getDomainName())
                ? request.getDomainName().trim().toLowerCase(Locale.ROOT)
                : "example.com";

        Domain sample = new Domain();
        sample.setName(domainName);
        sample.setExpiresAt(LocalDate.now().plusDays(windowDays));
        sample.setOwner(admin);

        List<String> channels = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        if (wantInApp) {
            try {
                notificationService.notifyDomainRenewal(admin, sample, windowDays);
                channels.add("IN_APP");
            } catch (Exception ex) {
                errors.add("IN_APP: " + ex.getMessage());
            }
        }
        if (wantEmail) {
            String to = StringUtils.hasText(request.getToEmail())
                    ? request.getToEmail().trim()
                    : admin.getEmail();
            if (!StringUtils.hasText(to)) {
                errors.add("EMAIL: recipient required");
            } else if (!emailConfigService.isEnabled()) {
                errors.add("EMAIL: SMTP not enabled");
            } else {
                try {
                    Locale locale = UserPreferredLanguage.toLocale(admin.getPreferredLanguage());
                    EmailConfig config = emailConfigService.getOrCreate();
                    String subject = "[TEST] " + messageService.get(
                            "notification.email.subject.domain_renewal",
                            new Object[]{sample.getName()},
                            locale);
                    String body = buildEmailBody(admin, sample, windowDays, locale)
                            + "\n\n(This is a test renewal reminder from Domainify admin settings.)";
                    mailService.sendEmail(config, to, subject, body);
                    channels.add("EMAIL");
                } catch (Exception ex) {
                    errors.add("EMAIL: " + ex.getMessage());
                }
            }
        }
        if (wantSms) {
            if (!admin.hasPhoneNumber() || !admin.isPhoneVerified()) {
                errors.add("SMS: verified phone required on your account");
            } else if (!isSmsProviderConfigured()) {
                errors.add("SMS: provider not configured");
            } else {
                try {
                    String mobile = PhoneSmsUtil.toSmsMobile(admin.getPhoneCountryCode(), admin.getPhoneNumber());
                    Locale locale = UserPreferredLanguage.toLocale(admin.getPreferredLanguage());
                    String text = "[TEST] " + messageService.get(
                            "notification.sms.domain_renewal",
                            new Object[]{sample.getName(), windowDays, sample.getExpiresAt().toString()},
                            locale);
                    SmsBulkSendRequest smsRequest = new SmsBulkSendRequest();
                    smsRequest.setMobiles(List.of(mobile));
                    smsRequest.setMessageText(text);
                    SmsBulkSendResultDto result = smsService.sendBulk(smsRequest);
                    if (result != null && result.isSuccess()) {
                        channels.add("SMS");
                    } else {
                        errors.add("SMS: provider rejected send");
                    }
                } catch (Exception ex) {
                    errors.add("SMS: " + ex.getMessage());
                }
            }
        }

        boolean success = !channels.isEmpty();
        String errorMessage = errors.isEmpty() ? null : String.join("; ", errors);
        return new DomainRenewalTestResultDto(success, errorMessage, channels);
    }

    private boolean sendReminderIfNeeded(
            Domain domain,
            int windowDays,
            boolean inAppEnabled,
            boolean emailEnabled,
            boolean smsEnabled) {
        if (domain == null || domain.getId() == null || domain.getExpiresAt() == null) {
            return false;
        }
        if (reminderRepository.existsByDomainIdAndWindowDaysAndExpiresAt(
                domain.getId(), windowDays, domain.getExpiresAt())) {
            return false;
        }

        User owner = domain.getOwner();
        if (owner == null || owner.getId() == null || !owner.isEnabled()) {
            return false;
        }

        List<String> channels = new ArrayList<>();
        if (inAppEnabled) {
            try {
                notificationService.notifyDomainRenewal(owner, domain, windowDays);
                channels.add("IN_APP");
            } catch (Exception ex) {
                log.warn("Domain renewal in-app notification failed for domain {}: {}",
                        domain.getId(), ex.getMessage());
            }
        }
        if (emailEnabled && trySendEmail(owner, domain, windowDays)) {
            channels.add("EMAIL");
        }
        if (smsEnabled && trySendSms(owner, domain, windowDays)) {
            channels.add("SMS");
        }
        if (channels.isEmpty()) {
            return false;
        }

        DomainRenewalReminder reminder = new DomainRenewalReminder();
        reminder.setDomain(domain);
        reminder.setWindowDays(windowDays);
        reminder.setExpiresAt(domain.getExpiresAt());
        reminder.setChannelsSent(String.join(",", channels));
        reminder.setSentAt(Instant.now());
        reminderRepository.save(reminder);
        return true;
    }

    private boolean trySendEmail(User owner, Domain domain, int windowDays) {
        if (!owner.isEmailNotificationsEnabled() || !StringUtils.hasText(owner.getEmail())) {
            return false;
        }
        if (!emailConfigService.isEnabled()) {
            return false;
        }
        try {
            Locale locale = UserPreferredLanguage.toLocale(owner.getPreferredLanguage());
            EmailConfig config = emailConfigService.getOrCreate();
            String subject = messageService.get(
                    "notification.email.subject.domain_renewal",
                    new Object[]{domain.getName()},
                    locale);
            String body = buildEmailBody(owner, domain, windowDays, locale);
            mailService.sendEmail(config, owner.getEmail(), subject, body);
            return true;
        } catch (MessagingException ex) {
            log.warn("Domain renewal email failed for user {}: {}", owner.getId(), ex.getMessage());
            return false;
        } catch (Exception ex) {
            log.warn("Domain renewal email skipped for user {}: {}", owner.getId(), ex.getMessage());
            return false;
        }
    }

    private boolean trySendSms(User owner, Domain domain, int windowDays) {
        if (!owner.isSmsNotificationsEnabled()) {
            return false;
        }
        if (!owner.hasPhoneNumber() || !owner.isPhoneVerified()) {
            return false;
        }
        if (!isSmsProviderConfigured()) {
            return false;
        }
        String mobile = PhoneSmsUtil.toSmsMobile(owner.getPhoneCountryCode(), owner.getPhoneNumber());
        if (!StringUtils.hasText(mobile)) {
            return false;
        }
        try {
            Locale locale = UserPreferredLanguage.toLocale(owner.getPreferredLanguage());
            String text = messageService.get(
                    "notification.sms.domain_renewal",
                    new Object[]{domain.getName(), windowDays, domain.getExpiresAt().toString()},
                    locale);
            SmsBulkSendRequest request = new SmsBulkSendRequest();
            request.setMobiles(List.of(mobile));
            request.setMessageText(text);
            SmsBulkSendResultDto result = smsService.sendBulk(request);
            if (result == null || !result.isSuccess()) {
                log.warn("Domain renewal SMS failed for user {}: provider rejected send", owner.getId());
                return false;
            }
            return true;
        } catch (Exception ex) {
            log.warn("Domain renewal SMS skipped for user {}: {}", owner.getId(), ex.getMessage());
            return false;
        }
    }

    private String buildEmailBody(User owner, Domain domain, int windowDays, Locale locale) {
        String helloName = StringUtils.hasText(owner.getFirstName())
                ? owner.getFirstName().trim()
                : owner.getEmail();
        String domainsUrl = trimTrailingSlash(frontendUrl) + "/domains";
        return messageService.get("notification.email.hello", new Object[]{helloName}, locale)
                + "\n\n"
                + messageService.get(
                        "notification.email.event.domain_renewal",
                        new Object[]{domain.getName(), windowDays, domain.getExpiresAt().toString()},
                        locale)
                + "\n\n"
                + messageService.get("notification.email.open_domains", locale)
                + "\n"
                + domainsUrl
                + "\n\n"
                + messageService.get("notification.email.footer.domain_renewal", locale)
                + "\n"
                + messageService.get("notification.email.signature", locale);
    }

    private boolean isSmsProviderConfigured() {
        return StringUtils.hasText(smsConfigService.getApiKey())
                && StringUtils.hasText(smsConfigService.getDefaultLine());
    }

    private String trimTrailingSlash(String url) {
        if (!StringUtils.hasText(url)) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
