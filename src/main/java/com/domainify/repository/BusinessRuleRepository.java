package com.domainify.repository;

import com.domainify.entity.BusinessRule;
import com.domainify.entity.BusinessRuleTrigger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusinessRuleRepository extends JpaRepository<BusinessRule, Long> {

    /**
     * Find all enabled rules for a specific trigger event, ordered by priority.
     */
    @Query("SELECT r FROM BusinessRule r WHERE r.enabled = true AND r.triggerEvent = :triggerEvent ORDER BY r.priority ASC, r.id ASC")
    List<BusinessRule> findEnabledRulesByTrigger(@Param("triggerEvent") BusinessRuleTrigger triggerEvent);

    /**
     * Find all enabled rules ordered by priority.
     */
    @Query("SELECT r FROM BusinessRule r WHERE r.enabled = true ORDER BY r.priority ASC, r.id ASC")
    List<BusinessRule> findAllEnabledRulesOrderByPriority();

    /**
     * Find rules by name (case-insensitive partial match).
     */
    @Query("SELECT r FROM BusinessRule r WHERE LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY r.priority ASC")
    List<BusinessRule> findByNameContainingIgnoreCase(@Param("name") String name);

    /**
     * Count enabled rules by trigger.
     */
    @Query("SELECT COUNT(r) FROM BusinessRule r WHERE r.enabled = true AND r.triggerEvent = :triggerEvent")
    long countEnabledRulesByTrigger(@Param("triggerEvent") BusinessRuleTrigger triggerEvent);
}