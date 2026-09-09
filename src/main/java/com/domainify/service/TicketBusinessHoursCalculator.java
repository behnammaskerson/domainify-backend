package com.domainify.service;

import com.domainify.dto.BusinessHolidayDto;
import com.domainify.dto.BusinessHoursDayDto;
import com.domainify.dto.BusinessHoursWeekDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class TicketBusinessHoursCalculator {

    private static final TypeReference<List<BusinessHolidayDto>> HOLIDAY_LIST_TYPE =
            new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public TicketBusinessHoursCalculator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Instant addBusinessHours(
            Instant from,
            long hours,
            ZoneId zone,
            BusinessHoursWeekDto week,
            Set<LocalDate> holidays) {
        if (from == null || hours <= 0) {
            return from;
        }
        return addBusinessMinutes(from, hours * 60L, zone, week, holidays);
    }

    public Instant addBusinessMinutes(
            Instant from,
            long minutes,
            ZoneId zone,
            BusinessHoursWeekDto week,
            Set<LocalDate> holidays) {
        if (from == null || minutes <= 0) {
            return from;
        }
        if (week == null || !hasAnyOpenDay(week)) {
            return from.plusSeconds(minutes * 60L);
        }

        long remainingMinutes = minutes;
        int maxIterations = (int) Math.max(1, minutes * 14L * 24L);
        LocalDateTime cursor = LocalDateTime.ofInstant(from, zone);
        Set<LocalDate> holidaySet = holidays != null ? holidays : Set.of();

        for (int i = 0; i < maxIterations && remainingMinutes > 0; i++) {
            LocalDate date = cursor.toLocalDate();
            if (holidaySet.contains(date)) {
                cursor = date.plusDays(1).atStartOfDay();
                continue;
            }

            BusinessHoursDayDto day = dayFor(week, date.getDayOfWeek());
            if (day == null || !isValidDayWindow(day)) {
                cursor = date.plusDays(1).atStartOfDay();
                continue;
            }

            LocalTime openStart = parseTime(day.getStart());
            LocalTime openEnd = parseTime(day.getEnd());
            if (openStart == null || openEnd == null || !openStart.isBefore(openEnd)) {
                cursor = date.plusDays(1).atStartOfDay();
                continue;
            }

            LocalDateTime windowStart = date.atTime(openStart);
            LocalDateTime windowEnd = date.atTime(openEnd);

            if (cursor.isBefore(windowStart)) {
                cursor = windowStart;
            }
            if (!cursor.isBefore(windowEnd)) {
                cursor = date.plusDays(1).atStartOfDay();
                continue;
            }

            long availableMinutes = Duration.between(cursor, windowEnd).toMinutes();
            if (availableMinutes <= 0) {
                cursor = date.plusDays(1).atStartOfDay();
                continue;
            }

            long consume = Math.min(remainingMinutes, availableMinutes);
            remainingMinutes -= consume;
            cursor = cursor.plusMinutes(consume);
        }

        if (remainingMinutes > 0) {
            return from.plusSeconds(minutes * 60L);
        }
        return cursor.atZone(zone).toInstant();
    }

    public long businessMinutesBetween(
            Instant from,
            Instant to,
            ZoneId zone,
            BusinessHoursWeekDto week,
            Set<LocalDate> holidays) {
        if (from == null || to == null || !to.isAfter(from)) {
            return 0;
        }
        if (week == null || !hasAnyOpenDay(week)) {
            return Math.max(0, Duration.between(from, to).toMinutes());
        }

        long totalMinutes = 0;
        int maxIterations = 365 * 24 * 60;
        LocalDateTime cursor = LocalDateTime.ofInstant(from, zone);
        LocalDateTime end = LocalDateTime.ofInstant(to, zone);
        Set<LocalDate> holidaySet = holidays != null ? holidays : Set.of();

        for (int i = 0; i < maxIterations && cursor.isBefore(end); i++) {
            LocalDate date = cursor.toLocalDate();
            if (holidaySet.contains(date)) {
                cursor = date.plusDays(1).atStartOfDay();
                continue;
            }

            BusinessHoursDayDto day = dayFor(week, date.getDayOfWeek());
            if (day == null || !isValidDayWindow(day)) {
                cursor = date.plusDays(1).atStartOfDay();
                continue;
            }

            LocalTime openStart = parseTime(day.getStart());
            LocalTime openEnd = parseTime(day.getEnd());
            if (openStart == null || openEnd == null || !openStart.isBefore(openEnd)) {
                cursor = date.plusDays(1).atStartOfDay();
                continue;
            }

            LocalDateTime windowStart = date.atTime(openStart);
            LocalDateTime windowEnd = date.atTime(openEnd);

            if (cursor.isBefore(windowStart)) {
                cursor = windowStart;
            }
            if (!cursor.isBefore(windowEnd)) {
                cursor = date.plusDays(1).atStartOfDay();
                continue;
            }

            LocalDateTime segmentEnd = end.isBefore(windowEnd) ? end : windowEnd;
            if (!cursor.isBefore(segmentEnd)) {
                cursor = date.plusDays(1).atStartOfDay();
                continue;
            }

            long minutes = Duration.between(cursor, segmentEnd).toMinutes();
            if (minutes > 0) {
                totalMinutes += minutes;
                cursor = segmentEnd;
            } else {
                cursor = date.plusDays(1).atStartOfDay();
            }
        }
        return totalMinutes;
    }

    public BusinessHoursWeekDto defaultWeek() {
        BusinessHoursWeekDto week = new BusinessHoursWeekDto();
        week.setMonday(new BusinessHoursDayDto("09:00", "17:00"));
        week.setTuesday(new BusinessHoursDayDto("09:00", "17:00"));
        week.setWednesday(new BusinessHoursDayDto("09:00", "17:00"));
        week.setThursday(new BusinessHoursDayDto("09:00", "17:00"));
        week.setFriday(new BusinessHoursDayDto("09:00", "17:00"));
        week.setSaturday(null);
        week.setSunday(null);
        return week;
    }

    public String serializeWeek(BusinessHoursWeekDto week) {
        try {
            return objectMapper.writeValueAsString(week != null ? week : defaultWeek());
        } catch (Exception ex) {
            return defaultWeekJson();
        }
    }

    public String serializeHolidays(List<BusinessHolidayDto> holidays) {
        try {
            return objectMapper.writeValueAsString(holidays != null ? holidays : List.of());
        } catch (Exception ex) {
            return "[]";
        }
    }

    public BusinessHoursWeekDto parseWeek(String json) {
        if (!StringUtils.hasText(json)) {
            return defaultWeek();
        }
        try {
            BusinessHoursWeekDto week = objectMapper.readValue(json, BusinessHoursWeekDto.class);
            return week != null ? week : defaultWeek();
        } catch (Exception ex) {
            return defaultWeek();
        }
    }

    public List<BusinessHolidayDto> parseHolidays(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<BusinessHolidayDto> holidays = objectMapper.readValue(json, HOLIDAY_LIST_TYPE);
            return holidays != null ? holidays : List.of();
        } catch (Exception ex) {
            return List.of();
        }
    }

    public Set<LocalDate> toHolidayDates(List<BusinessHolidayDto> holidays) {
        Set<LocalDate> dates = new HashSet<>();
        if (holidays == null) {
            return dates;
        }
        for (BusinessHolidayDto holiday : holidays) {
            if (holiday == null || !StringUtils.hasText(holiday.getDate())) {
                continue;
            }
            try {
                dates.add(LocalDate.parse(holiday.getDate().trim()));
            } catch (DateTimeParseException ignored) {
                // skip invalid dates
            }
        }
        return dates;
    }

    public static String defaultWeekJson() {
        return """
                {"monday":{"start":"09:00","end":"17:00"},"tuesday":{"start":"09:00","end":"17:00"},\
                "wednesday":{"start":"09:00","end":"17:00"},"thursday":{"start":"09:00","end":"17:00"},\
                "friday":{"start":"09:00","end":"17:00"},"saturday":null,"sunday":null}\
                """;
    }

    public boolean hasAnyOpenDay(BusinessHoursWeekDto week) {
        if (week == null) {
            return false;
        }
        return isValidDayWindow(week.getMonday())
                || isValidDayWindow(week.getTuesday())
                || isValidDayWindow(week.getWednesday())
                || isValidDayWindow(week.getThursday())
                || isValidDayWindow(week.getFriday())
                || isValidDayWindow(week.getSaturday())
                || isValidDayWindow(week.getSunday());
    }

    public boolean isValidDayWindow(BusinessHoursDayDto day) {
        if (day == null) {
            return false;
        }
        LocalTime start = parseTime(day.getStart());
        LocalTime end = parseTime(day.getEnd());
        return start != null && end != null && start.isBefore(end);
    }

    public List<String> validateWeek(BusinessHoursWeekDto week) {
        List<String> errors = new ArrayList<>();
        if (week == null) {
            errors.add("businessHours");
            return errors;
        }
        validateDay("monday", week.getMonday(), errors);
        validateDay("tuesday", week.getTuesday(), errors);
        validateDay("wednesday", week.getWednesday(), errors);
        validateDay("thursday", week.getThursday(), errors);
        validateDay("friday", week.getFriday(), errors);
        validateDay("saturday", week.getSaturday(), errors);
        validateDay("sunday", week.getSunday(), errors);
        return errors;
    }

    private void validateDay(String name, BusinessHoursDayDto day, List<String> errors) {
        if (day == null) {
            return;
        }
        boolean hasStart = StringUtils.hasText(day.getStart());
        boolean hasEnd = StringUtils.hasText(day.getEnd());
        if (!hasStart && !hasEnd) {
            return;
        }
        LocalTime start = parseTime(day.getStart());
        LocalTime end = parseTime(day.getEnd());
        if (start == null || end == null || !start.isBefore(end)) {
            errors.add(name);
        }
    }

    private BusinessHoursDayDto dayFor(BusinessHoursWeekDto week, DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> week.getMonday();
            case TUESDAY -> week.getTuesday();
            case WEDNESDAY -> week.getWednesday();
            case THURSDAY -> week.getThursday();
            case FRIDAY -> week.getFriday();
            case SATURDAY -> week.getSaturday();
            case SUNDAY -> week.getSunday();
        };
    }

    private LocalTime parseTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalTime.parse(value.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
