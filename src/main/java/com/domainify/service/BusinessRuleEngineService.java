package com.domainify.service;

import com.domainify.dto.BusinessRuleAction;
import com.domainify.dto.BusinessRuleCondition;
import com.domainify.entity.*;
import com.domainify.repository.BusinessRuleRepository;
import com.domainify.repository.TicketRepository;
import com.domainify.repository.UserRepository;
import com.domainify.repository.TicketQueueRepository;
import com.domainify.repository.TicketCategoryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Service for evaluating and executing business rules.
 */
@Service
@Transactional
public class BusinessRuleEngineService {

    private static final Logger log = LoggerFactory.getLogger(BusinessRuleEngineService.class);

    private final BusinessRuleRepository businessRuleRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketQueueRepository queueRepository;
    private final TicketCategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;

    public BusinessRuleEngineService(BusinessRuleRepository businessRuleRepository,
                                   TicketRepository ticketRepository,
                                   UserRepository userRepository,
                                   TicketQueueRepository queueRepository,
                                   TicketCategoryRepository categoryRepository,
                                   ObjectMapper objectMapper) {
        this.businessRuleRepository = businessRuleRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.queueRepository = queueRepository;
        this.categoryRepository = categoryRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Process all applicable rules for a ticket and trigger event.
     */
    public void processRules(Ticket ticket, BusinessRuleTrigger trigger) {
        List<BusinessRule> rules = businessRuleRepository.findEnabledRulesByTrigger(trigger);
        
        if (rules.isEmpty()) {
            return;
        }

        log.debug("Processing {} rule(s) for ticket {} on trigger {}", rules.size(), ticket.getId(), trigger);

        for (BusinessRule rule : rules) {
            try {
                if (evaluateConditions(ticket, rule)) {
                    log.info("Executing rule '{}' for ticket {}", rule.getName(), ticket.getId());
                    executeActions(ticket, rule);
                    rule.incrementExecutionCount();
                    businessRuleRepository.save(rule);
                } else {
                    log.debug("Rule '{}' conditions not met for ticket {}", rule.getName(), ticket.getId());
                }
            } catch (Exception ex) {
                log.error("Error processing rule '{}' for ticket {}: {}", rule.getName(), ticket.getId(), ex.getMessage());
            }
        }
    }

    /**
     * Evaluate all conditions for a rule against a ticket.
     * All conditions must be true (AND logic).
     */
    private boolean evaluateConditions(Ticket ticket, BusinessRule rule) {
        try {
            List<BusinessRuleCondition> conditions = objectMapper.readValue(
                rule.getConditions(), new TypeReference<List<BusinessRuleCondition>>() {}
            );

            return conditions.stream().allMatch(condition -> evaluateCondition(ticket, condition));
        } catch (Exception ex) {
            log.error("Error parsing conditions for rule '{}': {}", rule.getName(), ex.getMessage());
            return false;
        }
    }

    /**
     * Evaluate a single condition against a ticket.
     */
    private boolean evaluateCondition(Ticket ticket, BusinessRuleCondition condition) {
        Object fieldValue = getTicketFieldValue(ticket, condition.getField());
        Object conditionValue = condition.getValue();

        return switch (condition.getOperator()) {
            case EQUALS -> Objects.equals(fieldValue, conditionValue);
            case NOT_EQUALS -> !Objects.equals(fieldValue, conditionValue);
            case GREATER_THAN -> compareNumbers(fieldValue, conditionValue) > 0;
            case LESS_THAN -> compareNumbers(fieldValue, conditionValue) < 0;
            case GREATER_THAN_OR_EQUAL -> compareNumbers(fieldValue, conditionValue) >= 0;
            case LESS_THAN_OR_EQUAL -> compareNumbers(fieldValue, conditionValue) <= 0;
            case CONTAINS -> fieldValue != null && fieldValue.toString().contains(conditionValue.toString());
            case NOT_CONTAINS -> fieldValue == null || !fieldValue.toString().contains(conditionValue.toString());
            case STARTS_WITH -> fieldValue != null && fieldValue.toString().startsWith(conditionValue.toString());
            case ENDS_WITH -> fieldValue != null && fieldValue.toString().endsWith(conditionValue.toString());
            case IS_EMPTY -> fieldValue == null || fieldValue.toString().trim().isEmpty();
            case IS_NOT_EMPTY -> fieldValue != null && !fieldValue.toString().trim().isEmpty();
            case IN -> conditionValue instanceof Collection<?> collection && collection.contains(fieldValue);
            case NOT_IN -> !(conditionValue instanceof Collection<?> collection && collection.contains(fieldValue));
        };
    }

    /**
     * Execute all actions for a rule.
     */
    private void executeActions(Ticket ticket, BusinessRule rule) {
        try {
            List<BusinessRuleAction> actions = objectMapper.readValue(
                rule.getActions(), new TypeReference<List<BusinessRuleAction>>() {}
            );

            for (BusinessRuleAction action : actions) {
                executeAction(ticket, action);
            }

            // Save ticket if any changes were made
            ticketRepository.save(ticket);
        } catch (Exception ex) {
            log.error("Error executing actions for rule '{}': {}", rule.getName(), ex.getMessage());
        }
    }

    /**
     * Execute a single action.
     */
    private void executeAction(Ticket ticket, BusinessRuleAction action) {
        Map<String, Object> params = action.getParameters();
        
        switch (action.getActionType()) {
            case SET_STATUS -> {
                String status = (String) params.get("status");
                if (status != null) {
                    try {
                        TicketStatus newStatus = TicketStatus.valueOf(status.toUpperCase());
                        ticket.setStatus(newStatus);
                        // Note: Status change logic will be handled by the calling service
                    } catch (IllegalArgumentException ex) {
                        log.warn("Invalid status '{}' in rule action", status);
                    }
                }
            }
            
            case SET_PRIORITY -> {
                String priority = (String) params.get("priority");
                if (priority != null) {
                    try {
                        TicketPriority newPriority = TicketPriority.valueOf(priority.toUpperCase());
                        ticket.setPriority(newPriority);
                    } catch (IllegalArgumentException ex) {
                        log.warn("Invalid priority '{}' in rule action", priority);
                    }
                }
            }
            
            case ASSIGN_TO_USER -> {
                Object assigneeId = params.get("assigneeId");
                String assigneeEmail = (String) params.get("assigneeEmail");
                
                User assignee = null;
                if (assigneeId instanceof Number) {
                    assignee = userRepository.findById(((Number) assigneeId).longValue()).orElse(null);
                } else if (assigneeEmail != null) {
                    assignee = userRepository.findByEmail(assigneeEmail).orElse(null);
                }
                
                if (assignee != null) {
                    ticket.setAssignee(assignee);
                }
            }
            
            case ASSIGN_TO_QUEUE -> {
                Object queueId = params.get("queueId");
                String queueCode = (String) params.get("queueCode");
                
                TicketQueue queue = null;
                if (queueId instanceof Number) {
                    queue = queueRepository.findById(((Number) queueId).longValue()).orElse(null);
                } else if (queueCode != null) {
                    queue = queueRepository.findByCodeIgnoreCase(queueCode).orElse(null);
                }
                
                if (queue != null) {
                    ticket.setQueue(queue);
                }
            }
            
            case ADD_TAGS -> {
                Object tagsObj = params.get("tags");
                if (tagsObj instanceof List<?> tagsList) {
                    // Note: Tag management will be handled by the calling service
                    // This would require integration with TicketTagService
                    log.info("Would add tags to ticket {}: {}", ticket.getId(), tagsList);
                }
            }
            
            case REMOVE_TAGS -> {
                Object tagsObj = params.get("tags");
                if (tagsObj instanceof List<?> tagsList) {
                    // Note: Tag management will be handled by the calling service
                    log.info("Would remove tags from ticket {}: {}", ticket.getId(), tagsList);
                }
            }
            
            case SET_CATEGORY -> {
                Object categoryId = params.get("categoryId");
                
                TicketCategory category = null;
                if (categoryId instanceof Number) {
                    category = categoryRepository.findById(((Number) categoryId).longValue()).orElse(null);
                }
                // Note: findByName method doesn't exist, would need to be added to repository
                
                if (category != null) {
                    ticket.setCategory(category);
                }
            }
            
            case ADD_INTERNAL_NOTE -> {
                String note = (String) params.get("note");
                if (note != null && !note.trim().isEmpty()) {
                    // Create an internal note through the ticket service
                    // This would require extending TicketService or creating a separate method
                    log.info("Would add internal note to ticket {}: {}", ticket.getId(), note);
                }
            }
            
            case SET_DUE_DATE -> {
                Object dueDate = params.get("dueDate");
                Object daysFromNow = params.get("daysFromNow");
                
                Instant newDueDate = null;
                if (dueDate instanceof String) {
                    try {
                        newDueDate = Instant.parse((String) dueDate);
                    } catch (Exception ex) {
                        log.warn("Invalid due date format: {}", dueDate);
                    }
                } else if (daysFromNow instanceof Number) {
                    int days = ((Number) daysFromNow).intValue();
                    newDueDate = Instant.now().plus(days, ChronoUnit.DAYS);
                }
                
                if (newDueDate != null) {
                    ticket.setDueAt(newDueDate);
                }
            }
            
            case SEND_EMAIL_NOTIFICATION, SEND_SMS_NOTIFICATION -> {
                // These would require integration with NotificationService
                String recipientType = (String) params.get("recipientType");
                String message = (String) params.get("message");
                log.info("Would send {} notification to {} for ticket {}: {}",
                    action.getActionType(), recipientType, ticket.getId(), message);
            }
            
            case ESCALATE_TICKET -> {
                Object escalateTo = params.get("escalateTo");
                String reason = (String) params.get("reason");
                log.info("Would escalate ticket {} to {}: {}", ticket.getId(), escalateTo, reason);
            }
        }
    }

    /**
     * Get a field value from a ticket for condition evaluation.
     */
    private Object getTicketFieldValue(Ticket ticket, String field) {
        return switch (field.toLowerCase()) {
            case "status" -> ticket.getStatus() != null ? ticket.getStatus().name() : null;
            case "priority" -> ticket.getPriority() != null ? ticket.getPriority().name() : null;
            case "assigneeid" -> ticket.getAssignee() != null ? ticket.getAssignee().getId() : null;
            case "assigneeemail" -> ticket.getAssignee() != null ? ticket.getAssignee().getEmail() : null;
            case "queueid" -> ticket.getQueue() != null ? ticket.getQueue().getId() : null;
            case "queuename" -> ticket.getQueue() != null ? ticket.getQueue().getName() : null;
            case "categoryid" -> ticket.getCategory() != null ? ticket.getCategory().getId() : null;
            case "categoryname" -> ticket.getCategory() != null ? ticket.getCategory().getName() : null;
            case "customerid" -> ticket.getRequester() != null ? ticket.getRequester().getId() : null;
            case "customeremail" -> ticket.getRequester() != null ? ticket.getRequester().getEmail() : null;
            case "subject" -> ticket.getSubject();
            case "description" -> ticket.getDescription();
            case "tags" -> ticket.getTags();
            case "createdat" -> ticket.getCreatedAt();
            case "updatedat" -> ticket.getUpdatedAt();
            case "duedate" -> ticket.getDueAt();
            case "age" -> ticket.getCreatedAt() != null ? 
                ChronoUnit.HOURS.between(ticket.getCreatedAt(), Instant.now()) : null;
            default -> {
                log.warn("Unknown field '{}' in business rule condition", field);
                yield null;
            }
        };
    }

    /**
     * Compare two values as numbers.
     */
    private int compareNumbers(Object value1, Object value2) {
        if (value1 == null || value2 == null) {
            return 0;
        }

        try {
            double num1 = ((Number) value1).doubleValue();
            double num2 = ((Number) value2).doubleValue();
            return Double.compare(num1, num2);
        } catch (Exception ex) {
            return 0;
        }
    }
}