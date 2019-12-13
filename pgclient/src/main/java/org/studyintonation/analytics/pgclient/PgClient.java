package org.studyintonation.analytics.pgclient;

import com.typesafe.config.Config;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.postgresql.codec.Json;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactoryOptions;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;

import static io.r2dbc.pool.PoolingConnectionFactoryProvider.INITIAL_SIZE;
import static io.r2dbc.pool.PoolingConnectionFactoryProvider.MAX_SIZE;
import static io.r2dbc.spi.ConnectionFactoryOptions.DATABASE;
import static io.r2dbc.spi.ConnectionFactoryOptions.DRIVER;
import static io.r2dbc.spi.ConnectionFactoryOptions.HOST;
import static io.r2dbc.spi.ConnectionFactoryOptions.PASSWORD;
import static io.r2dbc.spi.ConnectionFactoryOptions.PORT;
import static io.r2dbc.spi.ConnectionFactoryOptions.PROTOCOL;
import static io.r2dbc.spi.ConnectionFactoryOptions.USER;

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
                                       final int age,
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
    public Mono<Boolean> addUserAttemptReport(final long uid,
                                              @NotNull final String cid,
                                              @NotNull final String lid,
                                              @NotNull final String tid,
                                              @NotNull final String rawPitchJson,
                                              final float dtw) {
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
}
