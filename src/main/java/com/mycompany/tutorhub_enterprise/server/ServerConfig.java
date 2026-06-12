package com.mycompany.tutorhub_enterprise.server;

import java.io.InputStream;
import java.util.Properties;

public final class ServerConfig {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("application.properties")) {
            if (in != null) {
                PROPS.load(in);
                System.out.println("[CONFIG] Loaded application.properties from classpath.");
            } else {
                System.out.println("[CONFIG] application.properties not found on classpath. Using env/defaults.");
            }
        } catch (Exception ex) {
            System.err.println("[CONFIG] Failed to load application.properties: " + ex.getMessage());
        }
    }

    private ServerConfig() {
    }

    public static String get(String envName, String propertyName, String defaultValue) {
        // Priority 1: Environment variable
        String envValue = System.getenv(envName);
        if (!isBlank(envValue)) {
            return envValue.trim();
        }

        // Priority 2: System property
        String systemProp = System.getProperty(propertyName);
        if (!isBlank(systemProp)) {
            return systemProp.trim();
        }

        // Priority 3: application.properties
        String fileProp = PROPS.getProperty(propertyName);
        if (!isBlank(fileProp)) {
            return fileProp.trim();
        }

        // Fallback
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
