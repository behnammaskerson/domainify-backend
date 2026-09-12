package com.domainify.service;

import com.domainify.dto.ContactRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class PublicContactService {

    private final GuestTicketService guestTicketService;

    public PublicContactService(GuestTicketService guestTicketService) {
        this.guestTicketService = guestTicketService;
    }

    public void submit(ContactRequest request, String captchaToken, String captchaAnswer, HttpServletRequest httpRequest) {
        guestTicketService.createFromContact(
                request.getName(),
                request.getEmail(),
                request.getCompany(),
                request.getSubject(),
                request.getMessage(),
                captchaToken,
                captchaAnswer,
                httpRequest
        );
    }
}
