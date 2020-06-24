package org.studyintonation.analytics.model;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

import static lombok.AccessLevel.PRIVATE;

public final class AttemptReport {
    private AttemptReport() {
    }

    @Value
    @RequiredArgsConstructor
    @NoArgsConstructor(force = true, access = PRIVATE)
    public static class Input {
        Long uid;
        String cid;
        String lid;
        String tid;
        Signal audio;
        Signal rawPitch;
        Signal processedPitch;
        Float dtw;
    }

    @Value
    public static class Output {
        long id;
        long uid;
        String cid;
        String lid;
        String tid;
        @Nullable
        Signal audio;
        @Nullable
        Signal rawPitch;
        @Nullable
        Signal processedPitch;
        float dtw;
        Instant ts;
    }
}
