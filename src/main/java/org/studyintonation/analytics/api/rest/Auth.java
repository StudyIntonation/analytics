package org.studyintonation.analytics.api.rest;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.studyintonation.analytics.api.util.RequestValidator;
import org.studyintonation.analytics.db.PgClient;
import org.studyintonation.analytics.model.User;
import reactor.core.publisher.Mono;

import static lombok.AccessLevel.PRIVATE;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
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
                .switchIfEmpty(Mono.just(RegisterResponse.ERROR))
                .onErrorReturn(e -> {
                    log.error("Error: ", e);
                    return true;
                }, RegisterResponse.ERROR);
    }

    @Value
    @RequiredArgsConstructor
    @NoArgsConstructor(force = true, access = PRIVATE)
    private static class RegisterRequest implements Request {
        User.Input user;
    }

    @Value
    private static class RegisterResponse implements Response {
        private static final RegisterResponse ERROR = new RegisterResponse(Status.ERROR, null);

        Status status;
        @Nullable
        Long id;

        private static RegisterResponse ok(final Long id) {
            return new RegisterResponse(Status.OK, id);
        }

        private enum Status {
            OK,
            ERROR,
        }
    }
}
