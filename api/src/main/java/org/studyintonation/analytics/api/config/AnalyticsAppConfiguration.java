package org.studyintonation.analytics.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.studyintonation.analytics.api.util.Parser;
import org.studyintonation.analytics.pgclient.PgClient;

@SpringBootConfiguration
public class AnalyticsAppConfiguration {
    @Bean
    @NotNull
    public Config appConfig() {
        return ConfigFactory.load("AnalyticsApp.conf");
    }

    @Bean
    @NotNull
    public Config pgClientConfig(@NotNull final Config appConfig) {
        return appConfig.getConfig("analyticsApp.pgclient");
    }

    @Bean
    @NotNull
    public PgClient pgClient(@NotNull final Config pgClientConfig) {
        return new PgClient(pgClientConfig);
    }

    @Bean
    @NotNull
    public Parser jsonEncoder(@NotNull final ObjectMapper objectMapper) {
        return new Parser(objectMapper);
    }
}
