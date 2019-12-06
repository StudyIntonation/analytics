package org.studyintonation.analytics.api.rest;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.studyintonation.analytics.pgclient.PgClient;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.ACCEPTED;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public final class Analytics implements Api {
    @NotNull
    private final PgClient pgClient;

    @PostMapping("/sendAttemptReport")
    @ResponseStatus(ACCEPTED)
    @NotNull
    public Mono<? extends Response> sendAttemptReport(@NotNull final Mono<SendAttemptReportRequest> body) {
        return body
                .map(it -> {
                    if (it.isValid()) {
                        return it;
                    }

                    throw Request.InvalidError.instance();
                })
                .flatMap(request -> pgClient
                        .addUserAttemptReport(
                                request.uid,
                                request.cid,
                                request.lid,
                                request.tid,
                                request.pitchSamples,
                                request.dtw
                        )
                )
                .map(success -> success ? SendAttemptReportResponse.OK : SendAttemptReportResponse.ERROR)
                .onErrorReturn(SendAttemptReportResponse.ERROR);
    }

    @RequiredArgsConstructor
    private static final class SendAttemptReportRequest implements Request {
        @Nullable
        private final String uid;
        @Nullable
        private final String cid;
        @Nullable
        private final String lid;
        @Nullable
        private final String tid;
        @Nullable
        private final float[] pitchSamples;
        private final float dtw;
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
