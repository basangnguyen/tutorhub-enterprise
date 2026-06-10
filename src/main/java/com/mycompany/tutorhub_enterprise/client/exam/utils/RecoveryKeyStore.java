package com.mycompany.tutorhub_enterprise.client.exam.utils;

public interface RecoveryKeyStore {
    /**
     * Protects a given key (e.g., base64 string) and returns an encrypted byte array.
     */
    byte[] protectKey(String keyB64) throws Exception;

    /**
     * Unprotects an encrypted byte array and returns the original key (base64 string).
     */
    String unprotectKey(byte[] encryptedKey) throws Exception;
}
