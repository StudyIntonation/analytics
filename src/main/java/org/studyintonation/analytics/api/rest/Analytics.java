package org.studyintonation.analytics.api.rest;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.studyintonation.analytics.api.util.RequestValidator;
import org.studyintonation.analytics.db.PgClient;
import org.studyintonation.analytics.model.AttemptReport;
import org.studyintonation.analytics.model.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static lombok.AccessLevel.PRIVATE;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequestMapping("/v0/analytics")
@RequiredArgsConstructor
public final class Analytics implements Api {
    private final PgClient pgClient;
    private final RequestValidator requestValidator;

    @PostMapping(path = "/sendAttemptReport", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(ACCEPTED)
    public Mono<SendAttemptReportResponse> sendAttemptReport(@RequestBody final Mono<SendAttemptReportRequest> body) {
        return body
                .filter(requestValidator::isValid)
                .map(SendAttemptReportRequest::getAttempt)
                .flatMap(pgClient::addUserAttemptReport)
                .map(success -> success ? SendAttemptReportResponse.OK : SendAttemptReportResponse.ERROR)
                .switchIfEmpty(Mono.just(SendAttemptReportResponse.ERROR))
                .onErrorReturn(e -> {
                    log.error("Error: ", e);
                    return true;
                }, SendAttemptReportResponse.ERROR);
    }

    @GetMapping(path = "/getUsers", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(OK)
    public Flux<User.Output> getUsers(@RequestParam(name = "token") final String adminToken) {
        return Mono.just(adminToken)
                .filter(requestValidator::isValidPrimitive)
                .flatMapMany(pgClient::getUsers)
                .onErrorResume(ex -> {
                    log.info("Error occurred: ", ex);
                    return Flux.empty();
                });
    }

    @GetMapping(path = "/getAttemptReports", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(OK)
    public Flux<AttemptReport.Output> getAttemptReports(@RequestParam(name = "token") final String adminToken,
                                                        @RequestParam(required = false) final Long uid,
                                                        @RequestParam(required = false) final Instant from,
                                                        @RequestParam(required = false) final Instant to) {
        return Mono.just(adminToken)
                .filter(requestValidator::isValidPrimitive)
                .flatMapMany(token -> pgClient.getAttemptReports(token, uid, from, to))
                .onErrorResume(ex -> {
                    log.info("Error occurred: ", ex);
                    return Flux.empty();
                });
    }

    @Value
    @RequiredArgsConstructor
    @NoArgsConstructor(force = true, access = PRIVATE)
    private static class SendAttemptReportRequest implements Request {
        AttemptReport.Input attempt;
    }

    @Value
    private static class SendAttemptReportResponse implements Response {
        private static final SendAttemptReportResponse OK = new SendAttemptReportResponse(Status.OK);
        private static final SendAttemptReportResponse ERROR = new SendAttemptReportResponse(Status.ERROR);

        Status status;

        private enum Status {
            OK,
            ERROR,
        }
    }
}
