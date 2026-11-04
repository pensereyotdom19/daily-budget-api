package com.pensereyotdom19.dailybudget.expense;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

/**
 * Same categories as the iPhone app. Java names are UPPER_CASE; JSON and the database use
 * the app's names ("food", "skinCare", ...).
 */
public enum Category {
    FOOD("food"), COFFEE("coffee"), TRANSPORT("transport"), SPORT("sport"), SKIN_CARE("skinCare"),
    PHONE("phone"), SHOPPING("shopping"), HEALTH("health"), FUN("fun"),
    BILLS("bills"), ENGAGEMENT("engagement"), OTHER("other");

    private final String raw;

    Category(String raw) {
        this.raw = raw;
    }

    @JsonValue
    public String raw() {
        return raw;
    }

    @JsonCreator
    public static Category fromRaw(String raw) {
        return Arrays.stream(values()).filter(c -> c.raw.equals(raw)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown category: " + raw));
    }

    /** Living money is everything except Bills and Engagement (same rule as the app). */
    public boolean isLiving() {
        return this != BILLS && this != ENGAGEMENT;
    }
}
