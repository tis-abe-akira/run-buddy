package com.example.runbuddy.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class CsvTest {

    @Test
    void startsWithAByteOrderMarkAndEndsLinesWithCrLf() {
        String csv = new Csv().row(List.of("a", "b")).row(List.of("1", "2")).toString();

        assertThat(csv).isEqualTo("﻿a,b\r\n1,2\r\n");
    }

    @Test
    void quotesFieldsWithCommasQuotesOrLineBreaks() {
        assertThat(Csv.quoteIfNeeded("400m × 10")).isEqualTo("400m × 10");
        assertThat(Csv.quoteIfNeeded("5:30/km, easy")).isEqualTo("\"5:30/km, easy\"");
        assertThat(Csv.quoteIfNeeded("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
        assertThat(Csv.quoteIfNeeded("two\nlines")).isEqualTo("\"two\nlines\"");
    }
}
