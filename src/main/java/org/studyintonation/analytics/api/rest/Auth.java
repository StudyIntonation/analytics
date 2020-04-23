package org.studyintonation.analytics.api.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.studyintonation.analytics.api.util.Exceptions;
import org.studyintonation.analytics.api.util.RequestValidator;
import org.studyintonation.analytics.db.PgClient;
import org.studyintonation.analytics.model.User;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/v0/auth")
@RequiredArgsConstructor
public final class Auth implements Api {
    private final PgClient pgClient;
    private final RequestValidator requestValidator;

    @PostMapping(path = "/register", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(ACCEPTED)
    public Mono<RegisterResponse> register(@RequestBody final Mono<RegisterRequest> body) {
        return body
                .filter(requestValidator::isValid)
                .map(RegisterRequest::getUser)
                .flatMap(it -> pgClient
                        .addAnonymousUser(it.getGender().toString(), it.getAge(), it.getFirstLanguage().toLanguageTag()))
                .map(RegisterResponse::ok)
                .onErrorReturn(Exceptions::logging, RegisterResponse.ERROR);
    }

    @Getter
    @RequiredArgsConstructor(onConstructor = @__(@JsonCreator))
    private static final class RegisterRequest implements Request {
        @NotNull
        private final User.Input user;
    }

    @RequiredArgsConstructor
    private static final class RegisterResponse implements Response {
        private static final RegisterResponse ERROR = new RegisterResponse(Status.ERROR, null);

        @NotNull
        private final Status status;
        @Nullable
        private final Long id;

        @NotNull
        private static RegisterResponse ok(@NotNull final Long id) {
            return new RegisterResponse(Status.OK, id);
        }

        private enum Status {
            OK,
            ERROR,
        }
    }
}
