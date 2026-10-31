package com.example.runbuddy.export;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RFC 4180 CSV, shaped for spreadsheet apps: CRLF line ends, and a UTF-8 byte order mark so Excel
 * reads Japanese text correctly instead of guessing a legacy encoding.
 */
final class Csv {

    static final String BYTE_ORDER_MARK = "﻿";

    private final StringBuilder text = new StringBuilder(BYTE_ORDER_MARK);

    Csv row(List<String> fields) {
        text.append(fields.stream().map(Csv::quoteIfNeeded).collect(Collectors.joining(","))).append("\r\n");
        return this;
    }

    @Override
    public String toString() {
        return text.toString();
    }

    /** Fields with a comma, quote or line break are quoted, and quotes inside them doubled. */
    static String quoteIfNeeded(String field) {
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}
