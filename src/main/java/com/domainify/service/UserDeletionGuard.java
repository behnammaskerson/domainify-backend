package com.domainify.service;

import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.TicketMentionRepository;
import com.domainify.repository.TicketMessageRepository;
import com.domainify.repository.TicketMessageRevisionRepository;
import com.domainify.repository.TicketReplyDraftRepository;
import com.domainify.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDeletionGuard {

    private final TicketRepository ticketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final TicketMessageRevisionRepository ticketMessageRevisionRepository;
    private final TicketMentionRepository ticketMentionRepository;
    private final TicketReplyDraftRepository ticketReplyDraftRepository;

    public UserDeletionGuard(
            TicketRepository ticketRepository,
            TicketMessageRepository ticketMessageRepository,
            TicketMessageRevisionRepository ticketMessageRevisionRepository,
            TicketMentionRepository ticketMentionRepository,
            TicketReplyDraftRepository ticketReplyDraftRepository) {
        this.ticketRepository = ticketRepository;
        this.ticketMessageRepository = ticketMessageRepository;
        this.ticketMessageRevisionRepository = ticketMessageRevisionRepository;
        this.ticketMentionRepository = ticketMentionRepository;
        this.ticketReplyDraftRepository = ticketReplyDraftRepository;
    }

    @Transactional(readOnly = true)
    public void assertDeletable(Long userId) {
        if (isReferenced(userId)) {
            throw new ApiException(ErrorCode.USER_IN_USE);
        }
    }

    private boolean isReferenced(Long userId) {
        return ticketRepository.existsByRequesterId(userId)
                || ticketRepository.existsByAssigneeId(userId)
                || ticketMessageRepository.existsByAuthorId(userId)
                || ticketMessageRevisionRepository.existsByActorId(userId)
                || ticketMentionRepository.existsByMentionedUserId(userId)
                || ticketReplyDraftRepository.existsByAuthorId(userId);
    }
}
