package com.bcp.workshop.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HtmlEscaperTest {
    @Test
    void escapesHtmlSpecialCharacters() {
        assertEquals("&lt;b&gt;A &amp; B&lt;/b&gt; &quot;&#39;", HtmlEscaper.escape("<b>A & B</b> \"'"));
    }

    @Test
    void keepsPlainTextUnchanged() {
        assertEquals("Workshop Student", HtmlEscaper.escape("Workshop Student"));
    }
}
