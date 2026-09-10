package com.domainify.service;

import com.domainify.dto.ContactRequest;
import com.domainify.entity.EmailConfig;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PublicContactService {

    private static final Logger log = LoggerFactory.getLogger(PublicContactService.class);

    private final EmailConfigService emailConfigService;
    private final MailService mailService;
    private final String contactInbox;

    public PublicContactService(
            EmailConfigService emailConfigService,
            MailService mailService,
            @Value("${domainify.contact.inbox:}") String contactInbox) {
        this.emailConfigService = emailConfigService;
        this.mailService = mailService;
        this.contactInbox = contactInbox;
    }

    public void submit(ContactRequest request) {
        EmailConfig config = emailConfigService.getOrCreate();
        if (!config.isEnabled()) {
            throw new ApiException(ErrorCode.EMAIL_CONFIG_DISABLED);
        }
        if (!StringUtils.hasText(config.getHost()) || !StringUtils.hasText(config.getFromEmail())) {
            throw new ApiException(ErrorCode.EMAIL_CONFIG_INVALID);
        }

        String inbox = StringUtils.hasText(contactInbox) ? contactInbox.trim() : config.getFromEmail();
        String company = StringUtils.hasText(request.getCompany()) ? request.getCompany().trim() : "-";
        String body = """
                New Domainify landing contact message

                Name: %s
                Email: %s
                Company: %s
                Subject: %s

                Message:
                %s
                """.formatted(
                request.getName().trim(),
                request.getEmail().trim(),
                company,
                request.getSubject().trim(),
                request.getMessage().trim()
        );

        try {
            mailService.sendEmail(
                    config,
                    inbox,
                    "[Domainify Contact] " + request.getSubject().trim(),
                    body
            );
            log.info("Landing contact message forwarded to {}", inbox);
        } catch (MessagingException ex) {
            log.warn("Failed to send landing contact email: {}", ex.getMessage());
            throw new ApiException(ErrorCode.CONTACT_SEND_FAILED);
        }
    }
}
