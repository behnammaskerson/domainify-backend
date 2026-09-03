package com.domainify.service;

import com.domainify.dto.PagedResponse;
import com.domainify.dto.TicketAssigneeOptionDto;
import com.domainify.dto.TicketDto;
import com.domainify.dto.TicketInboxFilter;
import com.domainify.dto.TicketTagDto;
import com.domainify.entity.Ticket;
import com.domainify.entity.TicketInboxView;
import com.domainify.entity.TicketMention;
import com.domainify.entity.TicketPriority;
import com.domainify.entity.TicketStatus;
import com.domainify.entity.TicketTag;
import com.domainify.entity.User;
import com.domainify.repository.TicketRepository;
import com.domainify.repository.UserRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AdminTicketService {

    private static final Set<TicketStatus> ACTIVE_STATUSES = EnumSet.of(
            TicketStatus.NEW,
            TicketStatus.OPEN,
            TicketStatus.PENDING,
            TicketStatus.ON_HOLD
    );
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "subject", "status", "priority", "publicNumber", "dueAt", "id"
    );

    private final TicketRepository ticketRepository;
    private final TicketCategoryService ticketCategoryService;
    private final TicketTagService ticketTagService;
    private final UserRepository userRepository;

    public AdminTicketService(
            TicketRepository ticketRepository,
            TicketCategoryService ticketCategoryService,
            TicketTagService ticketTagService,
            UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.ticketCategoryService = ticketCategoryService;
        this.ticketTagService = ticketTagService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<TicketDto> listInbox(
            User agent,
            TicketInboxView view,
            String q,
            TicketInboxFilter filter,
            Pageable pageable) {
        if (agent == null || agent.getId() == null) {
            throw new IllegalArgumentException("Agent is required");
        }
        if (view == null) {
            view = TicketInboxView.ALL;
        }
        if (filter == null) {
            filter = new TicketInboxFilter();
        }

        Specification<Ticket> spec = buildInboxSpec(agent.getId(), view, q, filter);
        Pageable safePageable = sanitizePageable(pageable);
        Page<TicketDto> page = ticketRepository.findAll(spec, safePageable).map(ticket -> toListDto(ticket, Instant.now()));
        return PagedResponse.from(page);
    }

    @Transactional(readOnly = true)
    public List<TicketAssigneeOptionDto> listAssignees() {
        return userRepository.findByRoleAndEnabledTrueOrderByFirstNameAscLastNameAsc(User.Role.ADMIN).stream()
                .map(user -> new TicketAssigneeOptionDto(user.getId(), displayName(user), user.getEmail()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketTagDto> listTags() {
        return ticketTagService.listAll();
    }

    private Specification<Ticket> buildInboxSpec(
            Long agentId,
            TicketInboxView view,
            String q,
            TicketInboxFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Object, Object> requesterJoin = null;

            switch (view) {
                case UNASSIGNED -> {
                    predicates.add(cb.isNull(root.get("assignee")));
                    predicates.add(root.get("status").in(ACTIVE_STATUSES));
                }
                case MINE -> {
                    predicates.add(cb.equal(root.get("assignee").get("id"), agentId));
                    predicates.add(root.get("status").in(ACTIVE_STATUSES));
                }
                case MENTIONS -> {
                    Subquery<Long> mentionSubquery = query.subquery(Long.class);
                    var mentionRoot = mentionSubquery.from(TicketMention.class);
                    mentionSubquery.select(mentionRoot.get("ticket").get("id"))
                            .where(cb.equal(mentionRoot.get("mentionedUser").get("id"), agentId));
                    predicates.add(root.get("id").in(mentionSubquery));
                }
                case OVERDUE -> {
                    Instant now = Instant.now();
                    predicates.add(cb.isNotNull(root.get("dueAt")));
                    predicates.add(cb.lessThan(root.get("dueAt"), now));
                    predicates.add(root.get("status").in(ACTIVE_STATUSES));
                }
                case ALL -> {
                    // no view-specific filter
                }
                default -> {
                    // no view-specific filter
                }
            }

            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getPriority() != null) {
                predicates.add(cb.equal(root.get("priority"), filter.getPriority()));
            }
            if (filter.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), filter.getCategoryId()));
            }
            if (filter.isUnassignedOnly()) {
                predicates.add(cb.isNull(root.get("assignee")));
            } else if (filter.getAssigneeId() != null) {
                predicates.add(cb.equal(root.get("assignee").get("id"), filter.getAssigneeId()));
            }
            if (filter.getCreatedFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getCreatedFrom()));
            }
            if (filter.getCreatedTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getCreatedTo()));
            }
            if (filter.getTagId() != null) {
                Join<Object, Object> tagJoin = root.join("tags", JoinType.INNER);
                predicates.add(cb.equal(tagJoin.get("id"), filter.getTagId()));
            }
            if (StringUtils.hasText(filter.getCustomer())) {
                String pattern = "%" + filter.getCustomer().trim().toLowerCase(Locale.ROOT) + "%";
                requesterJoin = root.join("requester", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(requesterJoin.get("email")), pattern),
                        cb.like(cb.lower(requesterJoin.get("firstName")), pattern),
                        cb.like(cb.lower(requesterJoin.get("lastName")), pattern)
                ));
            }

            if (StringUtils.hasText(q)) {
                String pattern = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                if (requesterJoin == null) {
                    requesterJoin = root.join("requester", JoinType.LEFT);
                }
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("publicNumber")), pattern),
                        cb.like(cb.lower(root.get("subject")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(requesterJoin.get("email")), pattern),
                        cb.like(cb.lower(requesterJoin.get("firstName")), pattern),
                        cb.like(cb.lower(requesterJoin.get("lastName")), pattern)
                ));
            }

            query.distinct(true);
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Pageable sanitizePageable(Pageable pageable) {
        int page = Math.max(pageable.getPageNumber(), 0);
        int size = pageable.getPageSize() <= 0 ? 10 : Math.min(pageable.getPageSize(), 100);

        List<Sort.Order> orders = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                continue;
            }
            Sort.Order next = new Sort.Order(order.getDirection(), order.getProperty());
            if ("subject".equals(order.getProperty()) || "publicNumber".equals(order.getProperty())) {
                next = next.ignoreCase();
            }
            orders.add(next);
        }
        if (orders.isEmpty()) {
            orders.add(Sort.Order.desc("updatedAt"));
        }
        return PageRequest.of(page, size, Sort.by(orders));
    }

    private TicketDto toListDto(Ticket ticket, Instant now) {
        TicketDto dto = toDtoBase(ticket, now);
        dto.setAttachments(List.of());
        return dto;
    }

    private TicketDto toDtoBase(Ticket ticket, Instant now) {
        TicketDto dto = new TicketDto();
        dto.setId(ticket.getId());
        dto.setPublicNumber(ticket.getPublicNumber());
        dto.setSubject(ticket.getSubject());
        dto.setDescription(ticket.getDescription());
        if (ticket.getCategory() != null) {
            dto.setCategory(ticketCategoryService.toDto(ticket.getCategory()));
        }
        dto.setPriority(ticket.getPriority());
        dto.setStatus(ticket.getStatus());
        dto.setChannel(ticket.getChannel());
        dto.setDueAt(ticket.getDueAt());
        dto.setOverdue(isOverdue(ticket, now));
        if (ticket.getRequester() != null) {
            dto.setRequesterId(ticket.getRequester().getId());
            dto.setRequesterEmail(ticket.getRequester().getEmail());
            dto.setRequesterName(displayName(ticket.getRequester()));
        }
        if (ticket.getAssignee() != null) {
            dto.setAssigneeId(ticket.getAssignee().getId());
            dto.setAssigneeEmail(ticket.getAssignee().getEmail());
            dto.setAssigneeName(displayName(ticket.getAssignee()));
        }
        if (ticket.getTags() != null && !ticket.getTags().isEmpty()) {
            dto.setTags(ticket.getTags().stream()
                    .sorted(Comparator.comparing(TicketTag::getName, String.CASE_INSENSITIVE_ORDER))
                    .map(ticketTagService::toDto)
                    .toList());
        }
        dto.setCreatedAt(ticket.getCreatedAt());
        dto.setUpdatedAt(ticket.getUpdatedAt());
        return dto;
    }

    static boolean isOverdue(Ticket ticket, Instant now) {
        if (ticket.getDueAt() == null || now == null) {
            return false;
        }
        if (!ACTIVE_STATUSES.contains(ticket.getStatus())) {
            return false;
        }
        return ticket.getDueAt().isBefore(now);
    }

    static Instant computeDueAt(TicketPriority priority, Instant from) {
        if (priority == null || from == null) {
            return null;
        }
        long hours = switch (priority) {
            case URGENT -> 4;
            case HIGH -> 24;
            case MEDIUM -> 72;
            case LOW -> 168;
        };
        return from.plusSeconds(hours * 3600);
    }

    private String displayName(User user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String last = user.getLastName() == null ? "" : user.getLastName().trim();
        String full = (first + " " + last).trim();
        return StringUtils.hasText(full) ? full : user.getEmail();
    }
}
