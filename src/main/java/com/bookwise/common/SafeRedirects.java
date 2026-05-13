package com.bookwise.common;

public final class SafeRedirects {

    private SafeRedirects() {
    }

    public static String normalize(String next, String fallback) {
        if (next == null || next.isBlank()) {
            return fallback;
        }
        String trimmed = next.trim();
        if (!trimmed.startsWith("/") || trimmed.startsWith("//")) {
            return fallback;
        }
        return trimmed;
    }
}
