package com.domainify.repository;

import com.domainify.entity.TicketMessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketMessageAttachmentRepository extends JpaRepository<TicketMessageAttachment, Long> {

    Optional<TicketMessageAttachment> findByIdAndMessageId(Long id, Long messageId);
}
