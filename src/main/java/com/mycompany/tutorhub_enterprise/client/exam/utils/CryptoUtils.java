package com.mycompany.tutorhub_enterprise.client.exam.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class CryptoUtils {

    private static final String ALGORITHM = "AES";
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    public static String generateAESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
        keyGen.init(256);
        SecretKey secretKey = keyGen.generateKey();
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }

    public static String encryptWrapper(String plainText, String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM);

        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

        byte[] cipherText = cipher.doFinal(plainText.getBytes("UTF-8"));

        JsonObject obj = new JsonObject();
        obj.addProperty("alg", CIPHER_ALGORITHM);
        obj.addProperty("iv", Base64.getEncoder().encodeToString(iv));
        obj.addProperty("ciphertext", Base64.getEncoder().encodeToString(cipherText));

        return new Gson().toJson(obj);
    }

    public static String decryptWrapper(String jsonWrapper, String base64Key) throws Exception {
        JsonObject obj = new Gson().fromJson(jsonWrapper, JsonObject.class);
        String alg = obj.get("alg").getAsString();
        if (!CIPHER_ALGORITHM.equals(alg)) {
            throw new Exception("Unsupported algorithm: " + alg);
        }

        byte[] iv = Base64.getDecoder().decode(obj.get("iv").getAsString());
        byte[] cipherText = Base64.getDecoder().decode(obj.get("ciphertext").getAsString());

        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

        byte[] plainText = cipher.doFinal(cipherText);
        return new String(plainText, "UTF-8");
    }
}
