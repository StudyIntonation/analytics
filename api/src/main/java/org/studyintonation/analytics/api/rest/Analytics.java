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
import org.studyintonation.analytics.api.util.JsonEncoder;
import org.studyintonation.analytics.pgclient.PgClient;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/v0/analytics")
@RequiredArgsConstructor
public final class Analytics implements Api {
    @NotNull
    private final PgClient pgClient;
    @NotNull
    private final JsonEncoder jsonEncoder;

    @PostMapping(path = "/sendAttemptReport", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(ACCEPTED)
    @NotNull
    public Mono<SendAttemptReportResponse> sendAttemptReport(@RequestBody @NotNull final Mono<SendAttemptReportRequest> body) {
        return body
                .map(SendAttemptReportRequest::validated)
                .flatMap(request -> pgClient
                        .addUserAttemptReport(
                                request.uid,
                                request.cid,
                                request.lid,
                                request.tid,
                                jsonEncoder.asJson(request.rawPitch),
                                request.dtw
                        )
                )
                .map(success -> success ? SendAttemptReportResponse.OK : SendAttemptReportResponse.ERROR)
                .onErrorReturn(Exceptions::logging, SendAttemptReportResponse.ERROR);
    }

    @RequiredArgsConstructor
    private static final class SendAttemptReportRequest implements Request {
        private final long uid;
        @Nullable
        private final String cid;
        @Nullable
        private final String lid;
        @Nullable
        private final String tid;
        @Nullable
        private final Pitch rawPitch;
        private final float dtw;

        @RequiredArgsConstructor(onConstructor = @__(@JsonCreator))
        @Getter
        private static final class Pitch {
            @NotNull
            private final float[] samples;
            private final int sampleRate;
        }

        @Override
        @NotNull
        public SendAttemptReportRequest validated() {
            return (SendAttemptReportRequest) Request.super.validated();
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
