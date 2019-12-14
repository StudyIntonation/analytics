package org.studyintonation.analytics.pgclient;

import com.typesafe.config.Config;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.postgresql.codec.Json;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Statement;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.studyintonation.analytics.pgclient.domain.AttemptReport;
import org.studyintonation.analytics.pgclient.domain.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.List;
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
            log.info("Failed to parse heroku postgres url", e);

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

                    return Mono.from(connection.close()).then(uid);
                });
    }

    @NotNull
    public Mono<Boolean> addUserAttemptReport(@NotNull final Long uid,
                                              @NotNull final String cid,
                                              @NotNull final String lid,
                                              @NotNull final String tid,
                                              @NotNull final String rawPitchJson,
                                              @NotNull final Float dtw) {
        return pool.create()
                .flatMap(connection -> {
                    final var success = Mono.from(connection
                            .createStatement("INSERT INTO attempt_report (uid, cid, lid, tid, raw_pitch, dtw, ts) VALUES ($1, $2, $3, $4, $5, $6, $7)")
                            .bind("$1", uid)
                            .bind("$2", cid)
                            .bind("$3", lid)
                            .bind("$4", tid)
                            .bind("$5", Json.of(rawPitchJson))
                            .bind("$6", dtw)
                            .bind("$7", Instant.now())
                            .execute())
                            .flatMap(result -> Mono.from(result.getRowsUpdated())
                                    .map(Integer.valueOf(1)::equals));

                    return Mono.from(connection.close()).then(success);
                });
    }

    @NotNull
    public Mono<List<User>> getUsers(@NotNull final String adminToken) {
        return pool.create().flatMap(connection -> {
            final var users = Mono.from(connection
                    .createStatement("SELECT * FROM \"user\" WHERE (SELECT * FROM admin_token WHERE token = $1) IS NOT NULL")
                    .bind("$1", adminToken)
                    .execute())
                    .flatMap(result -> Flux.from(result.map((row, __) -> new User(
                            requireNonNull(row.get("id", Long.class)),
                            requireNonNull(row.get("gender", String.class)),
                            requireNonNull(row.get("age", Integer.class)),
                            requireNonNull(row.get("first_language", String.class))
                    ))).collectList());

            return Mono.from(connection.close()).then(users);
        });
    }

    @NotNull
    public Mono<List<AttemptReport>> getAttemptReports(@NotNull final String adminToken,
                                                       @Nullable final Long uid,
                                                       @Nullable final Instant from,
                                                       @Nullable final Instant to) {
        return pool.create().flatMap(connection -> {
            final var attempts = Mono.from(getAttemptReportsStatement(connection, adminToken, uid, from, to)
                    .execute())
                    .flatMap(result -> Flux.from(result.map((row, __) -> new AttemptReport(
                            requireNonNull(row.get("id", Long.class)),
                            requireNonNull(row.get("uid", Long.class)),
                            requireNonNull(row.get("cid", String.class)),
                            requireNonNull(row.get("lid", String.class)),
                            requireNonNull(row.get("tid", String.class)),
                            requireNonNull(row.get("raw_pitch", Json.class)).asString(),
                            requireNonNull(row.get("dtw", Float.class)),
                            requireNonNull(row.get("ts", Instant.class))
                    ))).collectList());

            return Mono.from(connection.close()).then(attempts);
        });
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
