package org.studyintonation.analytics.pgclient;

import com.typesafe.config.Config;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.util.Locale;

import static io.r2dbc.spi.ConnectionFactoryOptions.DATABASE;
import static io.r2dbc.spi.ConnectionFactoryOptions.DRIVER;
import static io.r2dbc.spi.ConnectionFactoryOptions.HOST;
import static io.r2dbc.spi.ConnectionFactoryOptions.PASSWORD;
import static io.r2dbc.spi.ConnectionFactoryOptions.PORT;
import static io.r2dbc.spi.ConnectionFactoryOptions.PROTOCOL;
import static io.r2dbc.spi.ConnectionFactoryOptions.USER;


public final class PgClient {
    @NotNull
    private final ConnectionPool pool;

    public PgClient(@NotNull final Config config) {
        final var host = config.getString("host");
        final var port = config.getInt("port");
        final var user = config.getString("user");
        final var pass = config.getString("password");
        final var database = config.getString("database");

        final var poolMaxIdleTime = config.getDuration("pool.maxIdleTimeMillis");
        final var poolMaxSize = config.getInt("pool.maxSize");

        final var connectionFactory = ConnectionFactories.get(ConnectionFactoryOptions.builder()
                .option(DRIVER, "pool")
                .option(PROTOCOL, "postgresql")
                .option(HOST, host)
                .option(PORT, port)
                .option(USER, user)
                .option(PASSWORD, pass)
                .option(DATABASE, database)
                .build());

        final var poolConfiguration = ConnectionPoolConfiguration.builder(connectionFactory)
                .maxIdleTime(poolMaxIdleTime)
                .maxSize(poolMaxSize)
                .build();

        pool = new ConnectionPool(poolConfiguration);
    }

    public Mono<Long> register(@NotNull final String gender,
                                 final int age,
                                 @NotNull final Locale firstLanguage) {
        return Mono.just(1L);
    }

    public Mono<Boolean> addUserAttemptReport(@NotNull final String uid,
                                              @NotNull final String cid,
                                              @NotNull final String lid,
                                              @NotNull final String tid,
                                              @NotNull final float[] pitchSamples,
                                                       final float dtw) {
        //TODO:
        return Mono.just(true);
    }
}
