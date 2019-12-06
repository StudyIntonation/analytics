package org.studyintonation.analytics.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public interface AnalyticsApp {
    static void main(String[] args) {
        SpringApplication.run(AnalyticsApp.class);
    }
}
