package org.studyintonation.analytics.app.api.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

public interface Api {
    @JsonInclude(ALWAYS)
    @JsonAutoDetect(fieldVisibility = ANY)
    interface Request {
    }

    @JsonInclude(NON_NULL)
    @JsonAutoDetect(fieldVisibility = ANY)
    interface Response {
    }
}
