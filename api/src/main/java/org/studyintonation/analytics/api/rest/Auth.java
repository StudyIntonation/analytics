package org.studyintonation.analytics.api.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.studyintonation.analytics.api.util.Exceptions;
import org.studyintonation.analytics.pgclient.PgClient;
import reactor.core.publisher.Mono;

import java.util.Locale;

import static io.netty.handler.codec.http.HttpHeaders.Values.APPLICATION_JSON;
import static org.springframework.http.HttpStatus.ACCEPTED;

@RestController
@RequestMapping("/v0/auth")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public final class Auth implements Api {
    @NotNull
    private final PgClient pgClient;

    @PostMapping(path = "/register", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @ResponseStatus(ACCEPTED)
    @NotNull
    public Mono<? extends Response> register(@RequestBody @NotNull final Mono<RegisterRequest> body) {
        return body
                .map(RegisterRequest::validate)
                .flatMap(it -> pgClient.register(it.gender.toString(), it.age, it.firstLanguage.toLanguageTag()))
                .map(RegisterResponse::ok)
                .onErrorReturn(Exceptions::logging, RegisterResponse.ERROR);
    }

    @RequiredArgsConstructor(onConstructor = @__(@JsonCreator))
    private static final class RegisterRequest implements Request {
        @Nullable
        private final Gender gender;
        private final int age;
        @Nullable
        private final Locale firstLanguage;

        @NotNull
        @Override
        public RegisterRequest validate() {
            return (RegisterRequest) Request.super.validate();
        }

        private enum Gender {
            MALE,
            FEMALE,
            THIRD
        }
    }

    @RequiredArgsConstructor
    private static final class RegisterResponse implements Response {
        @NotNull
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
