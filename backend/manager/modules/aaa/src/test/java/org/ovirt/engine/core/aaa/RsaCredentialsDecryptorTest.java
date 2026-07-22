package org.ovirt.engine.core.aaa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import javax.crypto.Cipher;

import org.junit.jupiter.api.Test;

class RsaCredentialsDecryptorTest {

    @Test
    void decryptsPkcs1EncryptedCredential() throws Exception {
        KeyPair keyPair = getKeyPair();

        assertEquals("admin", RsaCredentialsDecryptor.decryptIfEncrypted(
                encrypt("admin", keyPair), keyPair.getPrivate()));
    }

    @Test
    void preservesLegacyPlaintextCredential() throws Exception {
        assertEquals("admin", RsaCredentialsDecryptor.decryptIfEncrypted("admin", getKeyPair().getPrivate()));
    }

    @Test
    void rejectsAnInvalidRsaSizedCiphertext() throws Exception {
        KeyPair keyPair = getKeyPair();
        byte[] invalidCiphertext = new byte[256];

        assertThrows(GeneralSecurityException.class, () -> RsaCredentialsDecryptor.decryptIfEncrypted(
                Base64.getEncoder().encodeToString(invalidCiphertext), keyPair.getPrivate()));
    }

    private static KeyPair getKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static String encrypt(String value, KeyPair keyPair) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
        return Base64.getEncoder().encodeToString(cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }
}
