package utils;

import java.util.*;
import org.testng.Assert;

import models.FrequencyConfig;



public class FrequencyValidator {

    public static void validate(String summary, FrequencyConfig config) {

        String normalized = normalize(summary);

        switch (config.getType()) {

            case DAILY:
                validateDaily(normalized, config);
                break;

            case WEEKLY:
                validateWeekly(normalized, config);
                break;

            case MONTHLY:
                validateMonthly(normalized, config);
                break;

            default:
                throw new IllegalArgumentException("Unsupported frequency type");
        }
    }

    // =========================
    // DAILY
    // =========================
    private static void validateDaily(String summary, FrequencyConfig config) {
        int every = config.getEvery();

        Assert.assertTrue(
            summary.contains("day"),
            "Expected daily frequency but got: " + summary
        );

        if (every > 1) {
            Assert.assertTrue(
                summary.contains(String.valueOf(every)),
                "Expected 'every " + every + " days' but got: " + summary
            );
            return;
        }

        Assert.assertTrue(
            summary.contains("daily")
                    || summary.contains("every day")
                    || summary.contains("every 1 day")
                    || summary.matches(".*\\b1\\s*day(s)?\\b.*"),
            "Expected daily frequency but got: " + summary
        );
    }

    // =========================
    // WEEKLY
    // =========================
    private static void validateWeekly(String summary, FrequencyConfig config) {
        int every = config.getEvery();

        Assert.assertTrue(
            summary.contains("week"),
            "Expected weekly frequency but got: " + summary
        );

        if (every > 1) {
            Assert.assertTrue(
                summary.contains(String.valueOf(every)),
                "Expected 'every " + every + " weeks' but got: " + summary
            );
        }

        if (config.getDaysOfWeek() != null) {
            for (String day : config.getDaysOfWeek()) {
                Assert.assertTrue(
                    summary.contains(day.toLowerCase()),
                    "Expected day '" + day + "' in summary: " + summary
                );
            }
        }
    }

    // =========================
    // MONTHLY
    // =========================
    private static void validateMonthly(String summary, FrequencyConfig config) {
    int every = config.getEvery();

    Assert.assertTrue(
        summary.contains("month"),
        "Expected monthly frequency but got: " + summary
    );

    if (every > 1) {
        Assert.assertTrue(
            summary.contains(String.valueOf(every)),
            "Expected 'every " + every + " months' but got: " + summary
        );
    }

    if (config.getDates() != null) {
        for (Integer date : config.getDates()) {
            Assert.assertTrue(
                summary.contains(String.valueOf(date)),
                "Expected date '" + date + "' in summary: " + summary
            );
        }
    }
}

    // =========================
    // NORMALIZATION
    // =========================
    private static String normalize(String text) {
        return text.toLowerCase(Locale.ENGLISH).trim();
    }
}
