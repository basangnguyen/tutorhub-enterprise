package com.mycompany.tutorhub_enterprise.server;

import java.io.InputStream;
import java.util.Properties;

public final class ServerConfig {

    private static final Properties PROPS = new Properties();

    static {
        // 1. Load application.properties
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                PROPS.load(in);
                System.out.println("[CONFIG] Loaded application.properties from classpath.");
            }
        } catch (Exception ex) {
            System.err.println("[CONFIG] Failed to load application.properties: " + ex.getMessage());
        }

        // 2. Load application-local.properties
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("application-local.properties")) {
            if (in != null) {
                PROPS.load(in);
                System.out.println("[CONFIG] Loaded application-local.properties from classpath.");
            }
        } catch (Exception ex) {}

        // 3. Load config/local-oauth.properties
        try (InputStream in = new java.io.FileInputStream("config/local-oauth.properties")) {
            PROPS.load(in);
            System.out.println("[CONFIG] Loaded config/local-oauth.properties from file system.");
        } catch (Exception ex) {}
    }

    private ServerConfig() {
    }

    public static String get(String envName, String propertyName, String altPropertyName, String defaultValue) {
        // Priority 1: Environment variable
        String envValue = System.getenv(envName);
        if (!isBlank(envValue)) return envValue.trim();

        // Priority 2: System property / VM Options
        String systemProp = System.getProperty(propertyName);
        if (!isBlank(systemProp)) return systemProp.trim();
        
        if (altPropertyName != null) {
            String altSystemProp = System.getProperty(altPropertyName);
            if (!isBlank(altSystemProp)) return altSystemProp.trim();
        }

        // Priority 3 & 4: Properties (application.properties -> local files)
        String fileProp = PROPS.getProperty(propertyName);
        if (!isBlank(fileProp)) return fileProp.trim();
        
        if (altPropertyName != null) {
            String altFileProp = PROPS.getProperty(altPropertyName);
            if (!isBlank(altFileProp)) return altFileProp.trim();
        }

        return defaultValue;
    }

    public static String get(String envName, String propertyName, String defaultValue) {
        return get(envName, propertyName, null, defaultValue);
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
