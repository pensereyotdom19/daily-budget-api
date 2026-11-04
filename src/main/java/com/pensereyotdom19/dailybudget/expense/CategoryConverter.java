package com.pensereyotdom19.dailybudget.expense;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Saves Category.SKIN_CARE as "skinCare" in the database, and reads it back. */
@Converter(autoApply = true)
public class CategoryConverter implements AttributeConverter<Category, String> {
    @Override
    public String convertToDatabaseColumn(Category c) {
        return c == null ? null : c.raw();
    }

    @Override
    public Category convertToEntityAttribute(String raw) {
        return raw == null ? null : Category.fromRaw(raw);
    }
}
