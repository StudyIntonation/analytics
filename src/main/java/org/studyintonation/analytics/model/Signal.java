package org.studyintonation.analytics.model;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import static lombok.AccessLevel.PRIVATE;

@Value
@RequiredArgsConstructor
@NoArgsConstructor(force = true, access = PRIVATE)
public class Signal {
    float[] samples;
    Integer sampleRate;
}
