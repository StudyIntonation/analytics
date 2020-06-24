package org.studyintonation.analytics.db;

import com.typesafe.config.Config;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.postgresql.api.PostgresqlResult;
import io.r2dbc.postgresql.api.PostgresqlStatement;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.Statement;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.studyintonation.analytics.db.transform.SignalTransform;
import org.studyintonation.analytics.model.AttemptReport;
import org.studyintonation.analytics.model.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Slf4j
public final class PgClient {
    private final ConnectionPool pool;
    private final SignalTransform signalTransform;
    private final Scheduler transformScheduler = Schedulers.boundedElastic();

    public PgClient(final ConnectionPool connectionPool, final Config config) {
        this.pool = connectionPool;
        this.signalTransform = new SignalTransform(config.getInt("transform.targetAudioSampleRate"));
    }

    public Mono<Long> addAnonymousUser(final String gender,
                                       final Integer age,
                                       final String firstLanguage) {
        return Mono.usingWhen(
                pool.create(),
                connection -> Mono
                        .from(connection
                                .createStatement("""
                                        INSERT INTO "user" (gender, age, first_language) 
                                        VALUES ($1, $2, $3)
                                        """)
                                .bind("$1", gender)
                                .bind("$2", age)
                                .bind("$3", firstLanguage)
                                .returnGeneratedValues("id")
                                .execute())
                        .flatMap(PgClient::rowIdMono),
                Connection::close
        ).doOnError(e -> log.error("PgClient error: ", e));
    }

    public Mono<Boolean> addUserAttemptReport(final AttemptReport.Input attemptReport) {
        return Mono.usingWhen(
                pool.create(),
                connection -> Mono
                        .zip(Mono.fromSupplier(() -> signalTransform.transformAudio(attemptReport.getAudio()))
                                        .subscribeOn(transformScheduler),
                                Mono.fromSupplier(() -> signalTransform.transform(attemptReport.getRawPitch()))
                                        .subscribeOn(transformScheduler),
                                Mono.fromSupplier(() -> signalTransform.transform(attemptReport.getProcessedPitch()))
                                        .subscribeOn(transformScheduler))
                        .flatMap(tuple -> {
                            final var audio = tuple.getT1();
                            final var rawPitch = tuple.getT2();
                            final var processedPitch = tuple.getT3();
                            return Mono
                                    .from(((PostgresqlStatement) connection
                                            .createStatement("""
                                                    INSERT INTO attempt_report (uid,
                                                                                cid,
                                                                                lid,
                                                                                tid,
                                                                                audio_samples,
                                                                                audio_sample_rate,
                                                                                raw_pitch_samples,
                                                                                raw_pitch_sample_rate,
                                                                                processed_pitch_samples,
                                                                                processed_pitch_sample_rate,
                                                                                dtw,
                                                                                ts)
                                                    VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
                                                    """))
                                            .bind("$1", attemptReport.getUid())
                                            .bind("$2", attemptReport.getCid())
                                            .bind("$3", attemptReport.getLid())
                                            .bind("$4", attemptReport.getTid())
                                            .bind("$5", audio.getT1())
                                            .bind("$6", audio.getT2())
                                            .bind("$7", rawPitch.getT1())
                                            .bind("$8", rawPitch.getT2())
                                            .bind("$9", processedPitch.getT1())
                                            .bind("$10", processedPitch.getT2())
                                            .bind("$11", attemptReport.getDtw())
                                            .bind("$12", Instant.now())
                                            .execute())
                                    .flatMap(PostgresqlResult::getRowsUpdated)
                                    .map(Integer.valueOf(1)::equals);
                        }),
                Connection::close
        ).doOnError(e -> log.error("PgClient error: ", e));
    }

    public Flux<User.Output> getUsers(final String adminToken) {
        return Flux.usingWhen(
                pool.create(),
                connection -> Mono
                        .from(connection
                                .createStatement("""
                                        SELECT * FROM "user" 
                                        WHERE (SELECT * 
                                               FROM admin_token
                                               WHERE token = $1) IS NOT NULL
                                        """)
                                .bind("$1", adminToken)
                                .execute())
                        .flatMapMany(PgClient::rowFlux)
                        .map(row -> new User.Output(
                                requireNonNull(row.get("id", Long.class)),
                                requireNonNull(row.get("gender", String.class)),
                                requireNonNull(row.get("age", Integer.class)),
                                requireNonNull(row.get("first_language", String.class))
                        )),
                Connection::close
        ).doOnError(e -> log.error("PgClient error: ", e));
    }

    public Flux<AttemptReport.Output> getAttemptReports(final String adminToken,
                                                        @Nullable final Long uid,
                                                        @Nullable final Instant from,
                                                        @Nullable final Instant to) {
        return Flux.usingWhen(
                pool.create(),
                connection -> Mono
                        .from(getAttemptReportsStatement(connection, adminToken, uid, from, to).execute())
                        .flatMapMany(PgClient::rowFlux)
                        .map(FlatAttemptReport::from)
                        .flatMap(flatReport -> Mono
                                //@formatter:off
                                .zip(Mono.fromSupplier(() -> signalTransform.transform(flatReport.audioSamples, flatReport.audioSampleRate))
                                        .subscribeOn(transformScheduler),
                                     Mono.fromSupplier(() -> signalTransform.transform(flatReport.rawPitchSamples, flatReport.rawPitchSampleRate))
                                        .subscribeOn(transformScheduler),
                                    Mono.fromSupplier(() -> signalTransform.transform(flatReport.processedPitchSamples, flatReport.processedPitchSampleRate))
                                        .subscribeOn(transformScheduler))
                                //@formatter:on
                                .map(signals -> {
                                    final var audioSamples = signals.getT1();
                                    final var rawPitchSamples = signals.getT2();
                                    final var processedPitchSamples = signals.getT3();
                                    return new AttemptReport.Output(
                                            flatReport.id,
                                            flatReport.uid,
                                            flatReport.cid,
                                            flatReport.lid,
                                            flatReport.tid,
                                            audioSamples,
                                            rawPitchSamples,
                                            processedPitchSamples,
                                            flatReport.dtw,
                                            flatReport.ts
                                    );
                                })),
                Connection::close
        ).doOnError(e -> log.error("PgClient error: ", e));
    }

    private static Mono<Long> rowIdMono(final Result result) {
        return Mono.from(result.map((row, __) -> row.get("id", Long.class)));
    }

    private static Flux<Row> rowFlux(final Result result) {
        return Flux.from(result.map((row, __) -> row));
    }

    private static Statement getAttemptReportsStatement(final Connection connection,
                                                        final String adminToken,
                                                        @Nullable final Long uid,
                                                        @Nullable final Instant from,
                                                        @Nullable final Instant to) {
        //@formatter:off
        final var statement = connection
                .createStatement("""
                                 SELECT * FROM attempt_report
                                 WHERE (SELECT * 
                                         FROM admin_token 
                                         WHERE token = $1) IS NOT NULL 
                                 """ +
                 (uid != null ? "AND uid = $4 " : "") +
                                "AND ts BETWEEN $2 AND $3")
                .bind("$1", adminToken)
                .bind("$2", Optional.ofNullable(from).orElse(Instant.EPOCH))
                .bind("$3", Optional.ofNullable(to).orElse(Instant.now()));
        //@formatter:on

        if (uid != null) {
            statement.bind("$4", uid);
        }

        return statement;
    }

    @Value
    private static class FlatAttemptReport {
        long id;
        long uid;
        String cid;
        String lid;
        String tid;
        ByteBuffer audioSamples;
        int audioSampleRate;
        ByteBuffer rawPitchSamples;
        int rawPitchSampleRate;
        ByteBuffer processedPitchSamples;
        int processedPitchSampleRate;
        float dtw;
        Instant ts;

        private static FlatAttemptReport from(final Row row) {
            return new FlatAttemptReport(
                    requireNonNull(row.get("id", Long.class)),
                    requireNonNull(row.get("uid", Long.class)),
                    requireNonNull(row.get("cid", String.class)),
                    requireNonNull(row.get("lid", String.class)),
                    requireNonNull(row.get("tid", String.class)),
                    requireNonNull(row.get("audio_samples", ByteBuffer.class)),
                    requireNonNull(row.get("audio_sample_rate", Integer.class)),
                    requireNonNull(row.get("raw_pitch_samples", ByteBuffer.class)),
                    requireNonNull(row.get("raw_pitch_sample_rate", Integer.class)),
                    requireNonNull(row.get("processed_pitch_samples", ByteBuffer.class)),
                    requireNonNull(row.get("processed_pitch_sample_rate", Integer.class)),
                    requireNonNull(row.get("dtw", Float.class)),
                    requireNonNull(row.get("ts", Instant.class))
            );
        }
    }
}
