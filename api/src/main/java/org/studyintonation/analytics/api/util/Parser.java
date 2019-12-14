package org.studyintonation.analytics.api.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@RequiredArgsConstructor
public final class Parser {
    @NotNull
    private final ObjectMapper objectMapper;

    @NotNull
    public <T> T validatedObject(@NotNull final T o) {
        if (isValid(o)) {
            return o;
        }

        throw InvalidError.instance();
    }

    @NotNull
    public <T> T validatedPrimitive(@Nullable T p) {
        if (p != null && (!(p instanceof String) || !((String) p).isBlank())) {
            return p;
        }

        throw InvalidError.instance();
    }

    @NotNull
    public <T> String validatedJson(@NotNull final T json) {
        try {
            return objectMapper.writeValueAsString(validatedObject(json));
        } catch (Throwable t) {
            throw Exceptions.uncheckedWrappingChecked(t);
        }
    }

    private static <T> boolean isValid(@NotNull final T o) {
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
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    final static class InvalidError extends RuntimeException {
        private InvalidError() {
            super();
        }

        private static final class Holder {
            @NotNull
            public static final InvalidError OUTER_INSTANCE = new InvalidError();
        }

        @NotNull
        public static InvalidError instance() {
            return Holder.OUTER_INSTANCE;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface OptionalField {}
}
