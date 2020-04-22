package org.studyintonation.analytics.app.db.transform;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.studyintonation.analytics.app.model.Signal;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.nio.ByteBuffer;

@Slf4j
public final class SignalTransform {
    private SignalTransform() {
    }

    @NotNull
    public static Tuple2<ByteBuffer, Integer> transform(@NotNull final Signal signal) {
        final var samples = signal.getSamples();
        final var bb = ByteBuffer.allocate(samples.length << 2);
        for (final var sample : samples) {
            bb.putFloat(sample);
        }

        return Tuples.of(bb.flip(), signal.getSampleRate());
    }

    @NotNull
    public static Signal transform(@NotNull final ByteBuffer byteBuf, @NotNull final Integer sampleRate) {
        final var floats = new float[byteBuf.limit() >> 2];

        byteBuf.asFloatBuffer().get(floats);
        log.info(Thread.currentThread().getName());
        return new Signal(floats, sampleRate);
    }
}
