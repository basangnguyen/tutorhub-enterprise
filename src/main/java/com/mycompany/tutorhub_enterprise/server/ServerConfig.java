package com.mycompany.tutorhub_enterprise.server;

public final class ServerConfig {

    private ServerConfig() {
    }

    public static String get(String envName, String propertyName, String defaultValue) {
        String propertyValue = System.getProperty(propertyName);
        if (!isBlank(propertyValue)) {
            return propertyValue.trim();
        }

        String envValue = System.getenv(envName);
        if (!isBlank(envValue)) {
            return envValue.trim();
        }

        return defaultValue;
    }

    public static boolean isEnabled(String envName, String propertyName, boolean defaultValue) {
        String value = get(envName, propertyName, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }

    public static boolean isAuthDevMode() {
        return isEnabled("TUTORHUB_AUTH_DEV_MODE", "tutorhub.auth.devMode", false);
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
