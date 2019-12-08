package org.studyintonation.analytics.api.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;

public final class JsonEncoder {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonEncoder() {
        throw new RuntimeException("JsonEncoder can not be instantiated");
    }

    @NotNull
    public static String asJson(Object o) {
        try {
            return OBJECT_MAPPER.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw Exceptions.uncheckedWrappingChecked(e);
        }
    }
}
