package com.antioxidant;

import java.util.Map;
import java.util.Objects;

public final class JsonTest {
    public static void run() {
        parsesAndSerializesNestedJson();
        rejectsTrailingData();
    }

    private static void parsesAndSerializesNestedJson() {
        Object value = Json.parse("{\"name\":\"blade\\n1\",\"values\":[1,true,null]}");
        Map<String, Object> object = Json.object(value);
        check(Objects.equals("blade\n1", object.get("name")), "escaped string parsing");
        check(Objects.equals(value, Json.parse(Json.stringify(value))), "nested JSON round trip");
    }

    private static void rejectsTrailingData() {
        try {
            Json.parse("{} false");
            throw new AssertionError("trailing JSON data was accepted");
        } catch (IllegalArgumentException expected) {
            // Expected parser rejection.
        }
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}