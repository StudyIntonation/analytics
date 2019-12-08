package org.studyintonation.analytics.api.util;

import org.jetbrains.annotations.NotNull;

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
}
