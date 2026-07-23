package org.ovirt.engine.core.aaa;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;

/**
 * Decrypts REST API Basic-auth passwords encrypted with the login RSA public key.
 *
 * <p>The client sends the password as standard Base64 of an RSA PKCS#1 v1.5 ciphertext.
 * The username remains plaintext and the matching private key remains on the engine host.</p>
 */
final class RsaCredentialsDecryptor {

    private static final Path PRIVATE_KEY_PATH =
            Path.of("/etc/ovirt-engine/encryptor/private_pkcs8.der"); //$NON-NLS-1$
    private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding"; //$NON-NLS-1$

    private RsaCredentialsDecryptor() {
    }

    static String decryptIfEncrypted(String value) throws GeneralSecurityException, IOException {
        return decryptIfEncrypted(value, readPrivateKey());
    }

    static String decryptIfEncrypted(String value, PrivateKey privateKey) throws GeneralSecurityException {
        if (value == null || value.isEmpty()) {
            return value;
        }

        byte[] encryptedBytes;
        try {
            encryptedBytes = Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException ex) {
            // A non-Base64 Basic credential is a legacy plaintext credential.
            return value;
        }

        int rsaBlockSize = (((RSAKey) privateKey).getModulus().bitLength() + Byte.SIZE - 1) / Byte.SIZE;
        if (encryptedBytes.length != rsaBlockSize) {
            // Preserve compatibility with existing Basic username:password clients.
            return value;
        }

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return new String(cipher.doFinal(encryptedBytes), StandardCharsets.UTF_8);
    }

    private static PrivateKey readPrivateKey() throws IOException, GeneralSecurityException {
        byte[] keyBytes = Files.readAllBytes(PRIVATE_KEY_PATH);
        return KeyFactory.getInstance("RSA") //$NON-NLS-1$
                .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }
}
