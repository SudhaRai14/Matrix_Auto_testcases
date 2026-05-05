package utils;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import models.FrequencyConfig;

public class RecurrenceUtils {

    public static List<LocalDate> generateDates(
            FrequencyConfig config,
            LocalDate start,
            LocalDate end) {

        return switch (config.getType()) {
            case DAILY -> generateDaily(config, start, end);
            case WEEKLY -> generateWeekly(config, start, end);
            case MONTHLY -> generateMonthly(config, start, end);
        };
    }

    private static List<LocalDate> generateDaily(
            FrequencyConfig config,
            LocalDate start,
            LocalDate end) {

        List<LocalDate> dates = new ArrayList<>();

        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(config.getEvery())) {
            dates.add(d);
        }

        return dates;
    }

    private static List<LocalDate> generateWeekly(
            FrequencyConfig config,
            LocalDate start,
            LocalDate end) {

        List<LocalDate> dates = new ArrayList<>();

        if (config.getDaysOfWeek() == null) return dates;

        Set<DayOfWeek> days = config.getDaysOfWeek()
                .stream()
                .map(d -> DayOfWeek.valueOf(d.toUpperCase()))
                .collect(Collectors.toSet());

        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {

            long weeksBetween = ChronoUnit.WEEKS.between(start, d);

            if (weeksBetween % config.getEvery() == 0
                    && days.contains(d.getDayOfWeek())) {

                dates.add(d);
            }
        }

        return dates;
    }

    private static List<LocalDate> generateMonthly(
            FrequencyConfig config,
            LocalDate start,
            LocalDate end) {

        List<LocalDate> dates = new ArrayList<>();

        if (config.getDates() == null) return dates;

        LocalDate current = start.withDayOfMonth(1);

        while (!current.isAfter(end)) {

            for (Integer day : config.getDates()) {

                if (day <= current.lengthOfMonth()) {

                    LocalDate candidate = current.withDayOfMonth(day);

                    if (!candidate.isBefore(start) && !candidate.isAfter(end)) {
                        dates.add(candidate);
                    }
                }
            }

            current = current.plusMonths(config.getEvery());
        }

        return dates;
    }
}