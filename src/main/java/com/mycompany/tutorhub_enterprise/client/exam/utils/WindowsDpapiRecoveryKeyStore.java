package com.mycompany.tutorhub_enterprise.client.exam.utils;

import com.sun.jna.platform.win32.Crypt32Util;

public class WindowsDpapiRecoveryKeyStore implements RecoveryKeyStore {

    @Override
    public byte[] protectKey(String keyB64) throws Exception {
        byte[] data = keyB64.getBytes("UTF-8");
        return Crypt32Util.cryptProtectData(data);
    }

    @Override
    public String unprotectKey(byte[] encryptedKey) throws Exception {
        byte[] data = Crypt32Util.cryptUnprotectData(encryptedKey);
        return new String(data, "UTF-8");
    }
}
