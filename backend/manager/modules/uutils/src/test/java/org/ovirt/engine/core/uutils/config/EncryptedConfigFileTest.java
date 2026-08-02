package org.ovirt.engine.core.uutils.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

public class EncryptedConfigFileTest {
    private static final String VECTOR =
            "T1ZFTkMwMDEBAAknwAABAgMEBQYHCAkKCwwNDg8QERITFBUWFxgZGhscHR4fICEiIyQlJicAMKCHF6zUoLJtPx0c"
                    + "DtBOcYes6QpztiMZSMLZBcqYBF0iu7+9G6dT6TOPeBPA17dzYgPsIZAoebwlz6I7YOg4Sazh8c+FE7nua6YPBTrK"
                    + "s159Z+gzrffyniMJYw==";

    @Test
    public void decryptsPythonOvenc001Vector() throws Exception {
        byte[] plaintext = EncryptedConfigFile.decrypt(
                Base64.getDecoder().decode(VECTOR),
                "test-passphrase".getBytes(StandardCharsets.UTF_8));
        assertEquals("ENGINE_DB_PASSWORD=\"test\"\n", new String(plaintext, StandardCharsets.UTF_8));
    }

    @Test
    public void rejectsWrongKey() {
        assertThrows(
                java.io.IOException.class,
                () -> EncryptedConfigFile.decrypt(
                        Base64.getDecoder().decode(VECTOR),
                        "wrong-passphrase".getBytes(StandardCharsets.UTF_8)));
    }
}
