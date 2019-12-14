package org.studyintonation.analytics.api.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.studyintonation.analytics.api.util.Exceptions;
import org.studyintonation.analytics.api.util.Parser;
import org.studyintonation.analytics.pgclient.PgClient;
import org.studyintonation.analytics.pgclient.domain.AttemptReport;
import org.studyintonation.analytics.pgclient.domain.User;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/v0/analytics")
@RequiredArgsConstructor
public final class Analytics implements Api {
    @NotNull
    private final PgClient pgClient;
    @NotNull
    private final Parser parser;

    @PostMapping(path = "/sendAttemptReport", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(ACCEPTED)
    @NotNull
    public Mono<SendAttemptReportResponse> sendAttemptReport(@RequestBody @NotNull final Mono<SendAttemptReportRequest> body) {
        return body
                .map(parser::validatedObject)
                .flatMap(request -> pgClient
                        .addUserAttemptReport(
                                request.uid,
                                request.cid,
                                request.lid,
                                request.tid,
                                parser.validatedJson(request.rawPitch),
                                request.dtw
                        )
                )
                .map(success -> success ? SendAttemptReportResponse.OK : SendAttemptReportResponse.ERROR)
                .onErrorReturn(Exceptions::logging, SendAttemptReportResponse.ERROR);
    }

    @GetMapping(path = "/getUsers", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(OK)
    @NotNull
    public Mono<List<User>> getUsers(@RequestParam(name = "token") @NotNull final String adminToken) {
        return Mono.just(adminToken)
                .map(parser::validatedPrimitive)
                .flatMap(pgClient::getUsers)
                .onErrorReturn(Exceptions::logging, Collections.emptyList());
    }

    @GetMapping(path = "/getAttemptReports", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(OK)
    @NotNull
    public Mono<List<AttemptReport>> getAttemptReports(@RequestParam(name = "token") @NotNull final String adminToken,
                                                       @RequestParam(required = false) @Nullable final Long uid,
                                                       @RequestParam(required = false) @Nullable final Instant from,
                                                       @RequestParam(required = false) @Nullable final Instant to) {
        return Mono.just(adminToken)
                .map(parser::validatedPrimitive)
                .flatMap(token -> pgClient.getAttemptReports(token, uid, from, to))
                .onErrorReturn(Exceptions::logging, Collections.emptyList());
    }

    @RequiredArgsConstructor
    private static final class SendAttemptReportRequest implements Request {
        @Nullable
        private final Long uid;
        @Nullable
        private final String cid;
        @Nullable
        private final String lid;
        @Nullable
        private final String tid;
        @Nullable
        private final Pitch rawPitch;
        @Nullable
        private final Float dtw;

        @RequiredArgsConstructor(onConstructor = @__(@JsonCreator))
        @Getter
        private static final class Pitch {
            @Nullable
            private final float[] samples;
            @Nullable
            private final Integer sampleRate;
        }
    }

    @RequiredArgsConstructor
    private static final class SendAttemptReportResponse implements Response {
        @NotNull
        private static final SendAttemptReportResponse OK = new SendAttemptReportResponse(Status.OK);
        @NotNull
        private static final SendAttemptReportResponse ERROR = new SendAttemptReportResponse(Status.ERROR);

        @NotNull
        private final Status status;

        private enum Status {
            OK,
            ERROR,
        }
    }
}
