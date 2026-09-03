package com.domainify.repository;

import com.domainify.entity.InAppNotification;
import com.domainify.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InAppNotificationRepository extends JpaRepository<InAppNotification, Long> {

    Page<InAppNotification> findByRecipientOrderByCreatedAtDesc(User recipient, Pageable pageable);

    Page<InAppNotification> findByRecipientAndReadFalseOrderByCreatedAtDesc(User recipient, Pageable pageable);

    long countByRecipientAndReadFalse(User recipient);

    @Modifying
    @Query("UPDATE InAppNotification n SET n.read = true, n.readAt = CURRENT_TIMESTAMP "
            + "WHERE n.recipient = :recipient AND n.read = false")
    int markAllReadForRecipient(@Param("recipient") User recipient);
}
