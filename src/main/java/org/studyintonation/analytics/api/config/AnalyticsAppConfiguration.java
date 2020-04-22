package org.studyintonation.analytics.api.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.studyintonation.analytics.api.util.RequestValidator;
import org.studyintonation.analytics.db.PgClient;

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
    public PgClient pgClient(final Config pgClientConfig) {
        return new PgClient(pgClientConfig);
    }

    @Bean
    public RequestValidator requestValidator() {
        return new RequestValidator();
    }
}
