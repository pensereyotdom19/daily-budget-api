package com.pensereyotdom19.dailybudget.common;

/** Month keys look like "2026-11", the same format the iPhone app uses. */
public final class MonthKey {
    /** Regex for validation annotations: @Pattern(regexp = MonthKey.PATTERN). */
    public static final String PATTERN = "^\\d{4}-(0[1-9]|1[0-2])$";

    private MonthKey() {}
}
