package org.studyintonation.analytics.app.db;

import com.typesafe.config.Config;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Statement;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.studyintonation.analytics.app.db.transform.SignalTransform;
import org.studyintonation.analytics.app.model.AttemptReport;
import org.studyintonation.analytics.app.model.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Optional;

import static io.r2dbc.pool.PoolingConnectionFactoryProvider.INITIAL_SIZE;
import static io.r2dbc.pool.PoolingConnectionFactoryProvider.MAX_SIZE;
import static io.r2dbc.spi.ConnectionFactoryOptions.DATABASE;
import static io.r2dbc.spi.ConnectionFactoryOptions.DRIVER;
import static io.r2dbc.spi.ConnectionFactoryOptions.HOST;
import static io.r2dbc.spi.ConnectionFactoryOptions.PASSWORD;
import static io.r2dbc.spi.ConnectionFactoryOptions.PORT;
import static io.r2dbc.spi.ConnectionFactoryOptions.PROTOCOL;
import static io.r2dbc.spi.ConnectionFactoryOptions.USER;
import static java.util.Objects.requireNonNull;

@Slf4j
public final class PgClient {
    @NotNull
    private final ConnectionPool pool;

    public PgClient(@NotNull final Config config) {
        String host;
        int port;
        String user;
        String pass;
        String database;

        try {
            /// Heroku-postgres
            final var rawUrl = System.getenv("DATABASE_URL");
            final var dbUri = new URI(rawUrl);

            host = dbUri.getHost();
            port = dbUri.getPort();
            user = dbUri.getUserInfo().split(":")[0];
            pass = dbUri.getUserInfo().split(":")[1];
            database = dbUri.getPath().replace("/", "");
        } catch (Exception e) {
            log.error("Failed to parse heroku postgres url", e);

            host = config.getString("host");
            port = config.getInt("port");
            user = config.getString("user");
            pass = config.getString("password");
            database = config.getString("database");
        }

        final var poolMaxIdleTime = config.getDuration("pool.maxIdleTimeMillis");
        final var poolMinSize = config.getInt("pool.minSize");
        final var poolMaxSize = config.getInt("pool.maxSize");

        final var connectionFactory = ConnectionFactories.get(ConnectionFactoryOptions.builder()
                .option(DRIVER, "pool")
                .option(PROTOCOL, "postgresql")
                .option(HOST, host)
                .option(PORT, port)
                .option(USER, user)
                .option(PASSWORD, pass)
                .option(DATABASE, database)
                .option(INITIAL_SIZE, poolMinSize)
                .option(MAX_SIZE, poolMaxSize)
                .build());

        final var poolConfiguration = ConnectionPoolConfiguration.builder(connectionFactory)
                .maxIdleTime(poolMaxIdleTime)
                .build();

        pool = new ConnectionPool(poolConfiguration);
    }

    @NotNull
    public Mono<Long> addAnonymousUser(@NotNull final String gender,
                                       @NotNull final Integer age,
                                       @NotNull final String firstLanguage) {
        return pool.create()
                .flatMap(connection -> {
                    final var uid = Mono.from(connection
                            .createStatement("INSERT INTO \"user\" (gender, age, first_language) VALUES ($1, $2, $3)")
                            .bind("$1", gender)
                            .bind("$2", age)
                            .bind("$3", firstLanguage)
                            .returnGeneratedValues("id")
                            .execute())
                            .flatMap(result -> Mono.from(result.map((row, __) -> row.get("id", Long.class))));

                    return Mono.from(connection.close())
                            .then(uid);
                });
    }

    @NotNull
    public Mono<Boolean> addUserAttemptReport(@NotNull final AttemptReport.Input attemptReport) {
        //@formatter:off
        return Mono
                .zip(pool.create(),
                     Mono.fromSupplier(() -> SignalTransform.transform(attemptReport.getAudio()))
                         .subscribeOn(Schedulers.boundedElastic()),
                     Mono.fromSupplier(() -> SignalTransform.transform(attemptReport.getRawPitch()))
                         .subscribeOn(Schedulers.boundedElastic()),
                     Mono.fromSupplier(() -> SignalTransform.transform(attemptReport.getProcessedPitch()))
                         .subscribeOn(Schedulers.boundedElastic()))
                .flatMap(tuple -> {
                    final var connection = tuple.getT1();
                    final var audio = tuple.getT2();
                    final var rawPitch = tuple.getT3();
                    final var processedPitch = tuple.getT4();

                    final var success = Mono.from(connection
                            .createStatement("INSERT INTO attempt_report (" +
                                                    "uid, " +
                                                    "cid, " +
                                                    "lid, " +
                                                    "tid, " +
                                                    "audio_samples, " +
                                                    "audio_sample_rate, " +
                                                    "raw_pitch_samples, " +
                                                    "raw_pitch_sample_rate, " +
                                                    "processed_pitch_samples, " +
                                                    "processed_pitch_sample_rate, " +
                                                    "dtw, " +
                                                    "ts) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)")
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
                            .flatMap(result -> Mono
                                    .from(result.getRowsUpdated())
                                    .map(Integer.valueOf(1)::equals));

                    return Mono.from(connection.close())
                            .then(success);
                });
        //@formatter:on
    }

    @NotNull
    public Flux<User> getUsers(@NotNull final String adminToken) {
        return pool.create()
                .flatMapMany(connection -> {
                    final var users = Mono
                            .from(connection
                                    .createStatement("SELECT * FROM \"user\" WHERE (SELECT * FROM admin_token WHERE token = $1) IS NOT NULL")
                                    .bind("$1", adminToken)
                                    .execute())
                            .flatMapMany(result -> Flux.from(result.map((row, __) -> new User(
                                    requireNonNull(row.get("id", Long.class)),
                                    requireNonNull(row.get("gender", String.class)),
                                    requireNonNull(row.get("age", Integer.class)),
                                    requireNonNull(row.get("first_language", String.class))
                            ))));

                    return Mono.from(connection.close())
                            .thenMany(users);
                });
    }

    @NotNull
    public Flux<AttemptReport.Output> getAttemptReports(@NotNull final String adminToken,
                                                        @Nullable final Long uid,
                                                        @Nullable final Instant from,
                                                        @Nullable final Instant to) {
        //@formatter:off
        return pool.create()
                .flatMapMany(connection -> {
                    final var attempts = Mono
                            .from(getAttemptReportsStatement(connection, adminToken, uid, from, to).execute())
                            .flatMapMany(result -> Flux.from(result.map((row, __) -> row)))
                            .flatMap(row -> Mono
                                    .zip(Mono.fromSupplier(() -> SignalTransform.transform(requireNonNull(row.get("audio_samples", ByteBuffer.class)),
                                                                                           requireNonNull(row.get("audio_sample_rate", Integer.class))))
                                             /*.subscribeOn(Schedulers.boundedElastic())*/,
                                         Mono.fromSupplier(() -> SignalTransform.transform(requireNonNull(row.get("raw_pitch_samples", ByteBuffer.class)),
                                                                                           requireNonNull(row.get("raw_pitch_sample_rate", Integer.class))))
                                             /*.subscribeOn(Schedulers.boundedElastic())*/,
                                         Mono.fromSupplier(() -> SignalTransform.transform(requireNonNull(row.get("processed_pitch_samples", ByteBuffer.class)),
                                                                                           requireNonNull(row.get("processed_pitch_sample_rate", Integer.class))))
                                             /*.subscribeOn(Schedulers.boundedElastic())*/
                                    )
                                    .map(signals -> new AttemptReport.Output(
                                            requireNonNull(row.get("id", Long.class)),
                                            requireNonNull(row.get("uid", Long.class)),
                                            requireNonNull(row.get("cid", String.class)),
                                            requireNonNull(row.get("lid", String.class)),
                                            requireNonNull(row.get("tid", String.class)),
                                            signals.getT1(),
                                            signals.getT2(),
                                            signals.getT3(),
                                            requireNonNull(row.get("dtw", Float.class)),
                                            requireNonNull(row.get("ts", Instant.class))
                                    ))
                            );

                    return Mono.from(connection.close())
                            .thenMany(attempts);
                });
        //@formatter:on
    }

    @NotNull
    private Statement getAttemptReportsStatement(@NotNull final Connection connection,
                                                 @NotNull final String adminToken,
                                                 @Nullable final Long uid,
                                                 @Nullable final Instant from,
                                                 @Nullable final Instant to) {
        if (uid != null) {
            return connection.createStatement(
                    "SELECT * FROM attempt_report " +
                            "WHERE (SELECT * FROM admin_token WHERE token = $1) IS NOT NULL " +
                            "AND uid = $2 " +
                            "AND ts BETWEEN $3 AND $4")
                    .bind("$1", adminToken)
                    .bind("$2", uid)
                    .bind("$3", Optional.ofNullable(from).orElse(Instant.EPOCH))
                    .bind("$4", Optional.ofNullable(to).orElse(Instant.now()));
        }

        return connection.createStatement(
                "SELECT * FROM attempt_report " +
                        "WHERE (SELECT * FROM admin_token WHERE token = $1) IS NOT NULL " +
                        "AND ts BETWEEN $2 AND $3")
                .bind("$1", adminToken)
                .bind("$2", Optional.ofNullable(from).orElse(Instant.EPOCH))
                .bind("$3", Optional.ofNullable(to).orElse(Instant.now()));
    }
}
