package com.domainify.repository;

import com.domainify.entity.TicketInboxSavedView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketInboxSavedViewRepository extends JpaRepository<TicketInboxSavedView, Long> {

    List<TicketInboxSavedView> findByUserIdOrderBySortOrderAscIdAsc(Long userId);

    Optional<TicketInboxSavedView> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(Long userId, String name, Long id);

    Optional<TicketInboxSavedView> findFirstByUserIdAndIsDefaultTrue(Long userId);

    @Modifying
    @Query("update TicketInboxSavedView v set v.isDefault = false where v.user.id = :userId and v.isDefault = true")
    void clearDefaultsForUser(@Param("userId") Long userId);

    long countByUserId(Long userId);
}
