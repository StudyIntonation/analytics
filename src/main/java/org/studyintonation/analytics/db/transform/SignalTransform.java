package org.studyintonation.analytics.db.transform;

import org.jetbrains.annotations.NotNull;
import org.studyintonation.analytics.model.Signal;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.nio.ByteBuffer;

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

        return new Signal(floats, sampleRate);
    }
}
