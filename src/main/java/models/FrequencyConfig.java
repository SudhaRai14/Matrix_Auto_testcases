package models;

import java.util.List;

public class FrequencyConfig {

    public enum Type {
        DAILY,
        WEEKLY,
        MONTHLY
    }

    private Type type;
    private int every;
    private List<String> daysOfWeek;
    private List<Integer> dates;

    private FrequencyConfig(Type type) {
        this.type = type;
    }

    // ===== FACTORY METHODS =====

    public static FrequencyConfig daily(int every) {
        FrequencyConfig config = new FrequencyConfig(Type.DAILY);
        config.every = every;
        return config;
    }

    public static FrequencyConfig weekly(int every, List<String> days) {
        FrequencyConfig config = new FrequencyConfig(Type.WEEKLY);
        config.every = every;
        config.daysOfWeek = days;
        return config;
    }

    public static FrequencyConfig monthlyOnDates(int every, List<Integer> dates) {
        FrequencyConfig config = new FrequencyConfig(Type.MONTHLY);
        config.every = every;
        config.dates = dates;
        return config;
    }

    // ===== GETTERS =====

    public Type getType() { return type; }
    public int getEvery() { return every; }
    public List<String> getDaysOfWeek() { return daysOfWeek; }
    public List<Integer> getDates() { return dates; }
}