package com.mr131.redplayer;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Streams private media through AES-256-GCM using a key held by Android Keystore. */
final class VaultCrypto {
    private static final String ALIAS = "131_red_player_vault_v1";
    private static final byte[] MAGIC = {'1','3','1','V','L','T','1'};
    private static final int IV_SIZE = 12;

    private VaultCrypto() {}

    static void encrypt(InputStream source, File target) throws Exception {
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, iv));
        try (BufferedOutputStream raw = new BufferedOutputStream(new FileOutputStream(target))) {
            raw.write(MAGIC);
            raw.write(iv);
            try (CipherOutputStream encrypted = new CipherOutputStream(raw, cipher);
                 BufferedInputStream input = new BufferedInputStream(source)) {
                copy(input, encrypted);
            }
        }
    }

    static void decrypt(File source, File target) throws Exception {
        try (BufferedInputStream raw = new BufferedInputStream(new FileInputStream(source))) {
            byte[] magic = raw.readNBytes(MAGIC.length);
            if (!java.util.Arrays.equals(magic, MAGIC)) throw new SecurityException("Not an encrypted vault file");
            byte[] iv = raw.readNBytes(IV_SIZE);
            if (iv.length != IV_SIZE) throw new SecurityException("Vault file is damaged");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            try (CipherInputStream decrypted = new CipherInputStream(raw, cipher);
                 BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(target))) {
                copy(decrypted, output);
            }
        } catch (Exception error) {
            target.delete();
            throw error;
        }
    }

    private static SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        KeyStore.Entry entry = store.getEntry(ALIAS, null);
        if (entry instanceof KeyStore.SecretKeyEntry) return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generator.generateKey();
    }

    private static void copy(InputStream in, java.io.OutputStream out) throws Exception {
        byte[] buffer = new byte[128 * 1024];
        int count;
        while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
    }
}
