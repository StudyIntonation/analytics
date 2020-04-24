package org.studyintonation.analytics.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
@JsonAutoDetect(fieldVisibility = ANY)

@SuppressWarnings("unused")
public class User {
    private User() {
    }

    @JsonInclude(ALWAYS)
    @JsonAutoDetect(fieldVisibility = ANY)
    @Getter
    @RequiredArgsConstructor(onConstructor = @__(@JsonCreator))
    public static final class Input {
        @NotNull
        private final Gender gender;
        @NotNull
        private final Integer age;
        @NotNull
        private final Locale firstLanguage;
    }

    @JsonInclude(NON_NULL)
    @JsonAutoDetect(fieldVisibility = ANY)
    @RequiredArgsConstructor
    public static final class Output {
        @NotNull
        private final Long id;
        @NotNull
        private final String gender;
        @NotNull
        private final Integer age;
        @NotNull
        private final String firstLanguage;
    }

    public enum Gender {
        MALE,
        FEMALE,
        NON_BINARY
    }
}
