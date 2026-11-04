package com.pensereyotdom19.dailybudget.settings;

import java.util.Map;
import java.util.TreeMap;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Category limits are saved in one text column as JSON, e.g. {"coffee":1500,"food":3000} (cents).
 * This turns that text into a Map and back, with Jackson (the JSON library Spring already uses).
 */
final class CategoryLimits {
    private CategoryLimits() {}

    /** Categories that may have a limit: the living ones. Bills and Engagement have their own plan. */
    static final String ALLOWED_KEYS = "food|coffee|transport|sport|skinCare|phone|shopping|health|fun|other";
    static final long MAX_CENTS = 100_000_000L;
    static final int MAX_ENTRIES = 20;

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final TypeReference<TreeMap<String, Long>> MAP_TYPE = new TypeReference<>() {};

    /** Map → JSON text. An empty map is saved as NULL ("no limits"). TreeMap keeps the keys in order. */
    static String toJson(Map<String, Long> limits) {
        return limits == null || limits.isEmpty() ? null : JSON.writeValueAsString(new TreeMap<>(limits));
    }

    /** JSON text → Map. NULL gives an empty map, so the reply always has an object ({}). */
    static Map<String, Long> fromJson(String json) {
        return json == null || json.isBlank() ? Map.of() : JSON.readValue(json, MAP_TYPE);
    }
}
