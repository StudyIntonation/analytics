package org.studyintonation.analytics.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.tools.agent.ReactorDebugAgent;

@SpringBootApplication
public class AnalyticsApp {
    public static void main(final String... args) {
        ReactorDebugAgent.init();
        SpringApplication.run(AnalyticsApp.class);
    }
}
