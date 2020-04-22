package org.studyintonation.analytics.app.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

public final class AttemptReport {
    private AttemptReport() {
    }

    @JsonInclude(ALWAYS)
    @JsonAutoDetect(fieldVisibility = ANY)
    @Getter
    @RequiredArgsConstructor(onConstructor = @__(@JsonCreator))
    public static final class Input {
        @NotNull
        private final Long uid;
        @NotNull
        private final String cid;
        @NotNull
        private final String lid;
        @NotNull
        private final String tid;
        @NotNull
        private final Signal audio;
        @NotNull
        private final Signal rawPitch;
        @NotNull
        private final Signal processedPitch;
        @NotNull
        private final Float dtw;
    }

    @JsonInclude(NON_NULL)
    @JsonAutoDetect(fieldVisibility = ANY)
    @RequiredArgsConstructor
    public static final class Output {
        private final long id;
        private final long uid;
        @NotNull
        private final String cid;
        @NotNull
        private final String lid;
        @NotNull
        private final String tid;
        @Nullable
        private final Signal audio;
        @Nullable
        private final Signal rawPith;
        @Nullable
        private final Signal processedPith;
        private final float dtw;
        @NotNull
        private final Instant ts;
    }
}
