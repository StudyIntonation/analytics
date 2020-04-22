package org.studyintonation.analytics.app.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
@JsonAutoDetect(fieldVisibility = ANY)
@RequiredArgsConstructor
public class User {
    private final long id;
    @NotNull
    private final String gender;
    private final int age;
    @NotNull
    private final String firstLanguage;
}
