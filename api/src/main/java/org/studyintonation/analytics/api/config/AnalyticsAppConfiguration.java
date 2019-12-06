package org.studyintonation.analytics.api.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.studyintonation.analytics.pgclient.PgClient;

@SpringBootConfiguration
public class AnalyticsAppConfiguration {
    @NotNull
    @Bean
    public Config appConfig() {
        return ConfigFactory.load("AnalyticsApp.conf");
    }

    @NotNull
    @Bean
    public Config pgClientConfig(@NotNull final Config appConfig) {
        return appConfig.getConfig("analyticsApp.pgclient");
    }

    @NotNull
    @Bean
    public PgClient pgClient(@NotNull final Config pgClientConfig) {
        return new PgClient(pgClientConfig);
    }
}
