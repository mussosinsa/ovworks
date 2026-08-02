package org.ovirt.engine.core.uutils.config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Reads plaintext configuration or the authenticated OVENC001 format. */
public final class EncryptedConfigFile {
    private static final byte[] MAGIC = "OVENC001".getBytes(StandardCharsets.US_ASCII);
    private static final int VERSION = 1;
    private static final int ITERATIONS = 600_000;
    private static final int SALT_SIZE = 16;
    private static final int NONCE_SIZE = 12;
    private static final int WRAPPED_KEY_SIZE = 48;
    private static final int HEADER_SIZE = 8 + 1 + 4 + SALT_SIZE + NONCE_SIZE + NONCE_SIZE + 2;
    private static final int GCM_TAG_BITS = 128;
    private static final String CREDENTIAL_NAME = "ovirt-encryptor-passphrase";
    private static final String PASSPHRASE_ENV = "OVIRT_ENCRYPTOR_PASSPHRASE";
    private static final String SECRET_FILE_ENV = "OVIRT_ENCRYPTOR_SECRET_FILE";
    private static final Set<String> ALLOWED_FILES = Collections.unmodifiableSet(
            Set.of("10-setup-database.conf", "10-setup-dwh-database.conf", "internal.properties"));
    private static final Set<Path> ALLOWED_ROOTS = Collections.unmodifiableSet(
            Set.of(Paths.get("/etc/ovirt-engine"), Paths.get("/etc/ovirt-engine-dwh")));

    private EncryptedConfigFile() {
    }

    private static class WipingInputStream extends ByteArrayInputStream {
        WipingInputStream(byte[] value) {
            super(value);
        }

        @Override
        public void close() throws IOException {
            Arrays.fill(buf, (byte) 0);
            super.close();
        }
    }

    public static InputStream open(File file) throws IOException {
        byte[] content = Files.readAllBytes(file.toPath());
        if (!hasMagic(content)) {
            return new WipingInputStream(content);
        }
        validate(file.toPath());
        byte[] passphrase = readPassphrase();
        try {
            return new WipingInputStream(decrypt(content, passphrase));
        } finally {
            Arrays.fill(passphrase, (byte) 0);
            Arrays.fill(content, (byte) 0);
        }
    }

    public static boolean isEncrypted(File file) throws IOException {
        try (InputStream input = new FileInputStream(file)) {
            byte[] prefix = new byte[MAGIC.length];
            return input.read(prefix) == prefix.length && Arrays.equals(prefix, MAGIC);
        }
    }

    public static File materialize(File file) throws IOException {
        if (!isEncrypted(file)) {
            return file;
        }
        String runtimeDirectory = System.getenv("RUNTIME_DIRECTORY");
        if (runtimeDirectory == null || runtimeDirectory.isEmpty()) {
            throw new IOException("RUNTIME_DIRECTORY is required for decrypted datasource configuration");
        }
        Path runtimePath = Paths.get(runtimeDirectory);
        if (Files.isSymbolicLink(runtimePath) || !Files.isDirectory(runtimePath, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("RUNTIME_DIRECTORY must be a real directory");
        }
        Path temporary = Files.createTempFile(runtimePath, "ovirt-encrypted-config-", ".properties");
        try (InputStream input = open(file)) {
            Files.copy(input, temporary, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        try {
            Files.setPosixFilePermissions(
                    temporary,
                    EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException exception) {
            if (!temporary.toFile().setReadable(false, false)
                    || !temporary.toFile().setReadable(true, true)
                    || !temporary.toFile().setWritable(false, false)
                    || !temporary.toFile().setWritable(true, true)) {
                Files.deleteIfExists(temporary);
                throw new IOException("Unable to restrict decrypted configuration permissions", exception);
            }
        }
        temporary.toFile().deleteOnExit();
        return temporary.toFile();
    }

    private static Path validate(Path path) throws IOException {
        Path absolute = path.toAbsolutePath().normalize();
        Path current = absolute;
        while (current != null) {
            if (Files.isSymbolicLink(current)) {
                throw new IOException("Refusing symbolic link in configuration path: " + current);
            }
            current = current.getParent();
        }
        if (!Files.isRegularFile(absolute, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Refusing symbolic link or non-regular configuration: " + absolute);
        }
        if (!ALLOWED_FILES.contains(absolute.getFileName().toString())) {
            throw new IOException("Encrypted configuration filename is not approved: " + absolute);
        }
        boolean allowed = ALLOWED_ROOTS.stream().anyMatch(absolute::startsWith);
        if (!allowed) {
            throw new IOException("Encrypted configuration is outside approved directories: " + absolute);
        }
        try {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(
                    absolute,
                    LinkOption.NOFOLLOW_LINKS);
            if (permissions.contains(PosixFilePermission.GROUP_WRITE)
                    || permissions.contains(PosixFilePermission.OTHERS_WRITE)) {
                throw new IOException("Encrypted configuration is group/other writable: " + absolute);
            }
        } catch (UnsupportedOperationException exception) {
            // Linux production deployments provide POSIX permissions.
        }
        return absolute;
    }

    private static boolean hasMagic(byte[] content) {
        return content.length >= MAGIC.length && Arrays.equals(MAGIC, Arrays.copyOf(content, MAGIC.length));
    }

    static byte[] decrypt(byte[] content, byte[] passphrase) throws IOException {
        if (content.length < HEADER_SIZE + WRAPPED_KEY_SIZE + 16) {
            throw new IOException("OVENC001 configuration is truncated");
        }
        ByteBuffer header = ByteBuffer.wrap(content, 0, HEADER_SIZE).order(ByteOrder.BIG_ENDIAN);
        byte[] magic = new byte[MAGIC.length];
        header.get(magic);
        int version = Byte.toUnsignedInt(header.get());
        int iterations = header.getInt();
        byte[] salt = new byte[SALT_SIZE];
        byte[] keyNonce = new byte[NONCE_SIZE];
        byte[] dataNonce = new byte[NONCE_SIZE];
        header.get(salt);
        header.get(keyNonce);
        header.get(dataNonce);
        int wrappedSize = Short.toUnsignedInt(header.getShort());
        if (!Arrays.equals(magic, MAGIC) || version != VERSION || iterations != ITERATIONS
                || wrappedSize != WRAPPED_KEY_SIZE) {
            throw new IOException("Invalid or unsupported OVENC001 header");
        }
        byte[] fixedHeader = Arrays.copyOfRange(content, 0, HEADER_SIZE);
        byte[] wrappedKey = Arrays.copyOfRange(content, HEADER_SIZE, HEADER_SIZE + wrappedSize);
        byte[] ciphertext = Arrays.copyOfRange(content, HEADER_SIZE + wrappedSize, content.length);
        byte[] kek = pbkdf2(passphrase, salt, iterations, 32);
        byte[] dataKey = null;
        try {
            dataKey = aesGcmDecrypt(kek, keyNonce, fixedHeader, wrappedKey);
            byte[] aad = ByteBuffer.allocate(fixedHeader.length + wrappedKey.length)
                    .put(fixedHeader)
                    .put(wrappedKey)
                    .array();
            return aesGcmDecrypt(dataKey, dataNonce, aad, ciphertext);
        } catch (GeneralSecurityException exception) {
            throw new IOException("OVENC001 authentication failed", exception);
        } finally {
            Arrays.fill(kek, (byte) 0);
            if (dataKey != null) {
                Arrays.fill(dataKey, (byte) 0);
            }
        }
    }

    private static byte[] aesGcmDecrypt(byte[] key, byte[] nonce, byte[] aad, byte[] ciphertext)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_BITS, nonce));
        cipher.updateAAD(aad);
        return cipher.doFinal(ciphertext);
    }

    private static byte[] pbkdf2(byte[] password, byte[] salt, int iterations, int length) throws IOException {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(password, "HmacSHA256"));
            byte[] output = new byte[length];
            byte[] block = ByteBuffer.allocate(salt.length + 4).put(salt).putInt(1).array();
            byte[] value = mac.doFinal(block);
            byte[] accumulator = value.clone();
            for (int index = 1; index < iterations; index++) {
                value = mac.doFinal(value);
                for (int offset = 0; offset < accumulator.length; offset++) {
                    accumulator[offset] ^= value[offset];
                }
            }
            System.arraycopy(accumulator, 0, output, 0, length);
            Arrays.fill(value, (byte) 0);
            Arrays.fill(accumulator, (byte) 0);
            return output;
        } catch (GeneralSecurityException exception) {
            throw new IOException("Unable to derive OVENC001 key", exception);
        }
    }

    private static byte[] readPassphrase() throws IOException {
        String credentialsDirectory = System.getenv("CREDENTIALS_DIRECTORY");
        if (credentialsDirectory != null && !credentialsDirectory.isEmpty()) {
            Path credential = Paths.get(credentialsDirectory, CREDENTIAL_NAME);
            if (Files.exists(credential)) {
                return readSecret(credential);
            }
        }
        String environment = System.getenv(PASSPHRASE_ENV);
        if (environment != null && !environment.isEmpty()) {
            return environment.getBytes(StandardCharsets.UTF_8);
        }
        String secretFile = System.getenv(SECRET_FILE_ENV);
        if (secretFile != null && !secretFile.isEmpty()) {
            return readSecret(Paths.get(secretFile));
        }
        throw new IOException("No OVENC001 key credential is available");
    }

    private static byte[] readSecret(Path path) throws IOException {
        if (Files.isSymbolicLink(path) || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Credential must be a regular non-symbolic file");
        }
        try {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS);
            Set<PosixFilePermission> forbidden = EnumSet.of(
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_WRITE,
                    PosixFilePermission.GROUP_EXECUTE,
                    PosixFilePermission.OTHERS_READ,
                    PosixFilePermission.OTHERS_WRITE,
                    PosixFilePermission.OTHERS_EXECUTE);
            if (!Collections.disjoint(permissions, forbidden)) {
                throw new IOException("Credential permissions must be 0600 or stricter");
            }
        } catch (UnsupportedOperationException exception) {
            // Linux production deployments provide POSIX permissions.
        }
        byte[] value = Files.readAllBytes(path);
        int length = value.length;
        while (length > 0 && (value[length - 1] == '\n' || value[length - 1] == '\r')) {
            length--;
        }
        if (length == 0) {
            throw new IOException("Credential is empty");
        }
        return Arrays.copyOf(value, length);
    }
}
