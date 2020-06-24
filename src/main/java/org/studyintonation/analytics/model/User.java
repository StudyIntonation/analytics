package org.studyintonation.analytics.model;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.Locale;

import static lombok.AccessLevel.PRIVATE;

public final class User {
    private User() {
    }

    @Value
    @RequiredArgsConstructor
    @NoArgsConstructor(force = true, access = PRIVATE)
    public static class Input {
        Gender gender;
        Integer age;
        Locale firstLanguage;
    }

    @Value
    public static class Output {
        Long id;
        String gender;
        Integer age;
        String firstLanguage;
    }

    @SuppressWarnings("unused")
    public enum Gender {
        MALE,
        FEMALE,
        NON_BINARY
    }
}
