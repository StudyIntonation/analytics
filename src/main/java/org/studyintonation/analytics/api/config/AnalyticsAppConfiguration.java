package org.studyintonation.analytics.api.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.postgresql.client.SSLMode;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.studyintonation.analytics.api.util.RequestValidator;
import org.studyintonation.analytics.db.PgClient;

import java.net.URI;

@SpringBootConfiguration
public class AnalyticsAppConfiguration {
    @Bean
    public Config appConfig() {
        return ConfigFactory.load("AnalyticsApp.conf");
    }

    @Bean
    public Config pgClientConfig(final Config appConfig) {
        return appConfig.getConfig("analyticsApp.pgclient");
    }

    @Bean
    public PgClient pgClient(final ConnectionPool connectionPool, final Config pgClientConfig) {
        return new PgClient(connectionPool, pgClientConfig);
    }

    @Bean
    public RequestValidator requestValidator() {
        return new RequestValidator();
    }

    @Bean
    public ConnectionPool connectionFactory(final Config pgClientConfig) {
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
        } catch (final Exception e) {
            host = pgClientConfig.getString("host");
            port = pgClientConfig.getInt("port");
            user = pgClientConfig.getString("user");
            pass = pgClientConfig.getString("password");
            database = pgClientConfig.getString("database");
        }

        final var poolMaxIdleTime = pgClientConfig.getDuration("pool.maxIdleTimeMillis");
        final var poolMinSize = pgClientConfig.getInt("pool.minSize");
        final var poolMaxSize = pgClientConfig.getInt("pool.maxSize");

        final var connectionFactory = new PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
            .host(host)
            .port(port)
            .username(user)
            .password(pass)
            .database(database)
            .sslMode(SSLMode.REQUIRE)
            .build());

        final var poolConfiguration = ConnectionPoolConfiguration.builder()
            .connectionFactory(connectionFactory)
            .initialSize(poolMinSize)
            .maxSize(poolMaxSize)
            .maxIdleTime(poolMaxIdleTime)
            .build();

        return new ConnectionPool(poolConfiguration);
    }
}
