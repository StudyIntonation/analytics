package org.studyintonation.analytics.app.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
@JsonAutoDetect(fieldVisibility = ANY)
@Getter
@RequiredArgsConstructor(onConstructor = @__(@JsonCreator))
public final class Signal {
    @NotNull
    private final float[] samples;
    @NotNull
    private final Integer sampleRate;
}
