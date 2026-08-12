package com.phyrr.codereview;

public final class Env {

    private Env() {
    }

    public static String getRequired(String key) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Missing required environment variable: " + key);
        }
        return value.trim();
    }

    public static String getOptional(String key) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    public static boolean isFeishuConfigured() {
        return getOptional("FEISHU_WEBHOOK_URL") != null;
    }

}
