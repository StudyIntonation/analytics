package org.studyintonation.analytics.app.api.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class RequestValidator {
    public <T> boolean isValid(@NotNull final T o) {
        for (final var field : o.getClass().getDeclaredFields()) {
            if (field.getAnnotation(OptionalField.class) != null) {
                continue;
            }
            try {
                if (!field.canAccess(o)) {
                    field.setAccessible(true);
                }
                if (field.get(o) == null) {
                    return false;
                }
            } catch (Exception __) {
                return false;
            }
        }
        return true;
    }

    public <T> boolean isValidPrimitive(@Nullable T p) {
        return p != null && (!(p instanceof String) || !((String) p).isBlank());
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface OptionalField {
    }
}
