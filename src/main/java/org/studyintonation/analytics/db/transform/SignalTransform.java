package org.studyintonation.analytics.db.transform;

import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.studyintonation.analytics.model.Signal;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.IOException;
import java.nio.ByteBuffer;

import static javax.sound.sampled.AudioFormat.Encoding.PCM_FLOAT;

@Slf4j
public final class SignalTransform {
    private static final int BITS_IN_SAMPLE = 32;
    private static final int CHANNELS = 1;
    private static final int BYTES_IN_FRAME = 4;

    private final int targetAudioSampleRate;
    private final AudioFormat targetFormat;

    public SignalTransform(final int targetAudioSampleRate) {
        this.targetAudioSampleRate = targetAudioSampleRate;
        this.targetFormat = new AudioFormat(PCM_FLOAT, (float) targetAudioSampleRate, BITS_IN_SAMPLE, CHANNELS, BYTES_IN_FRAME, (float) targetAudioSampleRate, true);
    }

    @NotNull
    public Tuple2<ByteBuffer, Integer> transform(@NotNull final Signal signal) {
        return Tuples.of(toByteBuf(signal.getSamples()), signal.getSampleRate());
    }

    @NotNull
    public Tuple2<ByteBuffer, Integer> transformAudio(@NotNull final Signal signal) {
        if (signal.getSampleRate() <= targetAudioSampleRate) {
            return transform(signal);
        }

        final var resampled = resample(signal, targetFormat);
        if (resampled == null) {
            return transform(signal);
        }

        return Tuples.of(resampled, targetAudioSampleRate);
    }

    @NotNull
    public Signal transform(@NotNull final ByteBuffer byteBuf, @NotNull final Integer sampleRate) {
        final var floats = new float[byteBuf.limit() >> 2];

        byteBuf.asFloatBuffer().get(floats);

        return new Signal(floats, sampleRate);
    }

    @NotNull
    private static ByteBuffer toByteBuf(@NotNull final float[] floats) {
        final var byteBuf = ByteBuffer.allocate(floats.length << 2);
        for (final var sample : floats) {
            byteBuf.putFloat(sample);
        }

        return byteBuf.flip();
    }

    @Nullable
    private static ByteBuffer resample(@NotNull final Signal signal, @NotNull final AudioFormat toFormat) {
        final int sourceSampleRate = signal.getSampleRate();
        final var sourceFormat = new AudioFormat(PCM_FLOAT, sourceSampleRate, BITS_IN_SAMPLE, CHANNELS, BYTES_IN_FRAME, sourceSampleRate, true);
        if (!AudioSystem.isConversionSupported(toFormat, sourceFormat)) {
            return null;
        }

        final var sourceSamples = signal.getSamples();
        final var byteBuf = toByteBuf(sourceSamples);

        try (final var targetAudioInputStream = AudioSystem.getAudioInputStream(toFormat, new AudioInputStream(
                new ByteBufferBackedInputStream(byteBuf), sourceFormat, sourceSamples.length))) {

            return ByteBuffer.wrap(targetAudioInputStream.readAllBytes());

        } catch (IOException e) {
            log.error("downsampling error: ", e);
        }

        return null;
    }
}
