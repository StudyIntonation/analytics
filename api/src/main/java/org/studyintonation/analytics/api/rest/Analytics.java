package org.studyintonation.analytics.api.rest;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.studyintonation.analytics.pgclient.PgClient;
import reactor.core.publisher.Mono;

import static io.netty.handler.codec.http.HttpHeaders.Values.APPLICATION_JSON;
import static org.springframework.http.HttpStatus.ACCEPTED;

@RestController
@RequestMapping("/v0/analytics")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public final class Analytics implements Api {
    @NotNull
    private final PgClient pgClient;

    @PostMapping(path = "/sendAttemptReport", consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    @ResponseStatus(ACCEPTED)
    @NotNull
    public Mono<? extends Response> sendAttemptReport(@RequestBody @NotNull final Mono<SendAttemptReportRequest> body) {
        return body
                .map(SendAttemptReportRequest::validate)
                .flatMap(request -> pgClient
                        .addUserAttemptReport(
                                request.uid,
                                request.cid,
                                request.lid,
                                request.tid,
                                request.rawPitch,
                                request.rawSampleRate,
                                request.dtw
                        )
                )
                .map(success -> success ? SendAttemptReportResponse.OK : SendAttemptReportResponse.ERROR)
                .onErrorReturn(SendAttemptReportResponse.ERROR);
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
        private final Float[] rawPitch;
        private final int rawSampleRate;
        private final float dtw;

        @Override
        public SendAttemptReportRequest validate() {
            return (SendAttemptReportRequest) Request.super.validate();
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
