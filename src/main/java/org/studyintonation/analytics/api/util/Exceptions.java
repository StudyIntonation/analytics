package org.studyintonation.analytics.api.util;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public final class Exceptions {
    private Exceptions() {
    }

    public static <T extends Throwable> boolean logging(@NotNull final T throwable) {
        log.info("Error occurred", throwable);
        return true;
    }
}
