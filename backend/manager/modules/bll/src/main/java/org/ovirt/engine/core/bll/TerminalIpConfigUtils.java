package org.ovirt.engine.core.bll;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class TerminalIpConfigUtils {
    private static final Pattern REQUIRE_IP_PATTERN =
            Pattern.compile("(?m)^(\\s*Require\\s+ip\\s+)(.*)$"); //$NON-NLS-1$
    private static final Pattern REQUIRE_IP_FULL_PATTERN =
            Pattern.compile("(?m)^\\s*Require\\s+ip\\s+.*$"); //$NON-NLS-1$

    private TerminalIpConfigUtils() {
    }

    public static Path getConfigPath() {
        return Path.of("/etc/httpd/conf.d/z-ovirt-engine-proxy.conf"); //$NON-NLS-1$
    }

    public static String readRequireIp() throws IOException {
        String content = Files.readString(getConfigPath(), StandardCharsets.UTF_8);
        Matcher matcher = REQUIRE_IP_PATTERN.matcher(content);
        return matcher.find() ? matcher.group(0) : null;
    }

    public static void updateRequireIp(String ipValue) throws IOException {
        Path configPath = getConfigPath();
        String content = Files.readString(configPath, StandardCharsets.UTF_8);
        Matcher matcher = REQUIRE_IP_PATTERN.matcher(content);
        if (!matcher.find()) {
            throw new IOException("Require ip line not found in z-ovirt-engine-proxy.conf"); //$NON-NLS-1$
        }
        String normalizedValue = ipValue == null ? "" : ipValue.trim(); //$NON-NLS-1$
        if (REQUIRE_IP_FULL_PATTERN.matcher(normalizedValue).matches()) {
            normalizedValue = normalizedValue.replaceFirst("^\\s*Require\\s+ip\\s+", ""); //$NON-NLS-1$ //$NON-NLS-2$
        }
        String updated = matcher.replaceFirst("$1" + Matcher.quoteReplacement(normalizedValue)); //$NON-NLS-1$
        if (!updated.equals(content)) {
            Files.writeString(configPath, updated, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }
}
