package com.domainify.service;

import com.domainify.entity.Ticket;
import com.domainify.entity.TicketAutoAssignMode;
import com.domainify.entity.TicketCategory;
import com.domainify.entity.TicketSettings;
import com.domainify.entity.User;
import com.domainify.repository.TicketAgentCategorySkillRepository;
import com.domainify.repository.TicketSettingsRepository;
import com.domainify.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TicketAutoAssignService {

    private final TicketSettingsRepository ticketSettingsRepository;
    private final UserRepository userRepository;
    private final TicketAgentCategorySkillRepository skillRepository;

    public TicketAutoAssignService(
            TicketSettingsRepository ticketSettingsRepository,
            UserRepository userRepository,
            TicketAgentCategorySkillRepository skillRepository) {
        this.ticketSettingsRepository = ticketSettingsRepository;
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
    }

    /**
     * Picks an assignee for a newly created ticket according to settings.
     * Updates the round-robin cursor when that mode (or fallback) is used.
     */
    @Transactional
    public User assignIfConfigured(Ticket ticket) {
        if (ticket == null) {
            return null;
        }
        TicketSettings settings = getSettingsForUpdate();
        TicketAutoAssignMode mode = settings.getAutoAssignMode() != null
                ? settings.getAutoAssignMode()
                : TicketAutoAssignMode.OFF;
        if (mode == TicketAutoAssignMode.OFF) {
            return null;
        }

        List<User> pool;
        if (mode == TicketAutoAssignMode.CATEGORY_SKILL) {
            TicketCategory category = ticket.getCategory();
            pool = category != null && category.getId() != null
                    ? skillRepository.findEnabledAgentsByCategoryId(category.getId(), User.Role.ADMIN)
                    : List.of();
            if (pool.isEmpty()) {
                if (!settings.isAutoAssignFallbackRoundRobin()) {
                    return null;
                }
                pool = listEnabledAdmins();
            }
        } else {
            pool = listEnabledAdmins();
        }

        if (pool.isEmpty()) {
            return null;
        }

        User next = pickNext(pool, settings.getRoundRobinLastUserId());
        if (next == null) {
            return null;
        }
        ticket.setAssignee(next);
        settings.setRoundRobinLastUserId(next.getId());
        ticketSettingsRepository.save(settings);
        return next;
    }

    private List<User> listEnabledAdmins() {
        return userRepository.findByRoleAndEnabledTrueOrderByFirstNameAscLastNameAsc(User.Role.ADMIN);
    }

    private User pickNext(List<User> pool, Long lastUserId) {
        if (pool == null || pool.isEmpty()) {
            return null;
        }
        if (lastUserId == null) {
            return pool.get(0);
        }
        int index = -1;
        for (int i = 0; i < pool.size(); i++) {
            if (lastUserId.equals(pool.get(i).getId())) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return pool.get(0);
        }
        return pool.get((index + 1) % pool.size());
    }

    private TicketSettings getSettingsForUpdate() {
        return ticketSettingsRepository.findByIdForUpdate(TicketSettings.SINGLETON_ID)
                .map(settings -> {
                    settings.normalize();
                    return settings;
                })
                .orElseGet(() -> ticketSettingsRepository.save(TicketSettings.defaults()));
    }
}
