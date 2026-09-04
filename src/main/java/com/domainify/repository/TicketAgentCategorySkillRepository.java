package com.domainify.repository;

import com.domainify.entity.TicketAgentCategorySkill;
import com.domainify.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketAgentCategorySkillRepository extends JpaRepository<TicketAgentCategorySkill, Long> {

    List<TicketAgentCategorySkill> findByCategoryId(Long categoryId);

    @Query("""
            SELECT s.user FROM TicketAgentCategorySkill s
            WHERE s.category.id = :categoryId
              AND s.user.role = :role
              AND s.user.enabled = true
            ORDER BY s.user.firstName ASC, s.user.lastName ASC, s.user.id ASC
            """)
    List<User> findEnabledAgentsByCategoryId(
            @Param("categoryId") Long categoryId,
            @Param("role") User.Role role);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM TicketAgentCategorySkill s WHERE s.category.id = :categoryId")
    void deleteByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT s.user.id FROM TicketAgentCategorySkill s WHERE s.category.id = :categoryId")
    List<Long> findUserIdsByCategoryId(@Param("categoryId") Long categoryId);
}
