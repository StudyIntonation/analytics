package org.studyintonation.analytics.api.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public final class JsonEncoder {
    private final ObjectMapper objectMapper;

    @SneakyThrows
    @NotNull
    public String asJson(@NotNull final Object o) {
        return objectMapper.writeValueAsString(o);
    }
}
