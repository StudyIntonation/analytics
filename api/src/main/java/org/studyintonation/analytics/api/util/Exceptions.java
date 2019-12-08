package org.studyintonation.analytics.api.util;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public final class Exceptions {
    private Exceptions() {
        throw new RuntimeException("Exceptions can not be instantiated");
    }

    @NotNull
    public static UncheckedException uncheckedWrappingChecked(@NotNull final Throwable throwable) {
        return new UncheckedException(throwable);
    }

    public static final class UncheckedException extends RuntimeException {
        public UncheckedException(@NotNull final Throwable throwable) {
            super(throwable);
        }
    }

    public static <T extends Throwable> boolean logging(@NotNull final T throwable) {
        log.info("Error occurred", throwable);
        return true;
    }
}
