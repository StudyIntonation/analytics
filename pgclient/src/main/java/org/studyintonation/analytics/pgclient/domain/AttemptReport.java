package org.studyintonation.analytics.pgclient.domain;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
@JsonAutoDetect(fieldVisibility = ANY)
@RequiredArgsConstructor
public final class AttemptReport {
    private final long id;
    private final long uid;
    @NotNull
    private final String cid;
    @NotNull
    private final String lid;
    @NotNull
    private final String tid;
    @NotNull
    @JsonRawValue
    private final String pitch;
    private final float dtw;
    @NotNull
    private final Instant ts;
}
