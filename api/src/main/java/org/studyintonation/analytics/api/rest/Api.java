package org.studyintonation.analytics.api.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

public interface Api {
    @Retention(RetentionPolicy.RUNTIME)
    @interface Optional {}

    @JsonInclude(ALWAYS)
    @JsonAutoDetect(fieldVisibility = ANY)
    interface Request {
        default boolean isValid() {
            for (final var field : getClass().getDeclaredFields()) {
                if (field.getAnnotation(Optional.class) != null) {
                    continue;
                }

                try {
                    if (!field.canAccess(this)) {
                        field.setAccessible(true);
                    }
                    if (field.get(this) == null) {
                        return false;
                    }
                } catch (Exception e) {
                    return false;
                }
            }
            return true;
        }

        @NotNull
        default Request validate() {
            if (isValid()) {
                return this;
            }

            throw InvalidError.instance();
        }

        final class InvalidError extends RuntimeException {
            private InvalidError() {
                super();
            }

            private static final class Holder {
                public static final InvalidError OUTER_INSTANCE = new InvalidError();
            }

            public static InvalidError instance() {
                return Holder.OUTER_INSTANCE;
            }
        }
    }

    @JsonInclude(NON_NULL)
    @JsonAutoDetect(fieldVisibility = ANY)
    interface Response {}
}
