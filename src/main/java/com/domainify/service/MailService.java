package com.domainify.service;

import com.domainify.entity.EmailConfig;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Service
public class MailService {

    public void sendTestEmail(EmailConfig config, String to) throws MessagingException {
        JavaMailSenderImpl sender = buildMailSender(config);
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
        helper.setTo(to);
        helper.setFrom(resolveFromAddress(config));
        helper.setSubject("Domainify email test");
        helper.setText("This is a test email from your Domainify email server configuration.", false);
        sender.send(message);
    }

    private InternetAddress resolveFromAddress(EmailConfig config) throws MessagingException {
        try {
            if (StringUtils.hasText(config.getFromName())) {
                return new InternetAddress(config.getFromEmail(), config.getFromName(), StandardCharsets.UTF_8.name());
            }
            return new InternetAddress(config.getFromEmail());
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new MessagingException("Unsupported from name encoding", ex);
        }
    }

    JavaMailSenderImpl buildMailSender(EmailConfig config) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(config.getHost());
        mailSender.setPort(config.getPort());
        if (StringUtils.hasText(config.getUsername())) {
            mailSender.setUsername(config.getUsername());
            mailSender.setPassword(config.getPassword());
        }

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        boolean auth = StringUtils.hasText(config.getUsername());
        props.put("mail.smtp.auth", String.valueOf(auth));
        if (config.isUseTls()) {
            props.put("mail.smtp.starttls.enable", "true");
        }
        if (config.getPort() == 465) {
            props.put("mail.smtp.ssl.enable", "true");
        }
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        return mailSender;
    }
}
