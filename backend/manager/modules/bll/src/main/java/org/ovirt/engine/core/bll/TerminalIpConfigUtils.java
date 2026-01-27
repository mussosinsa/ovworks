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
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            if (result.length() > 0) {
                result.append("\n"); //$NON-NLS-1$
            }
            result.append(matcher.group(0));
        }
        return result.length() > 0 ? result.toString() : null;
    }

    public static void updateRequireIp(String ipValue) throws IOException {
        Path configPath = getConfigPath();
        String content = Files.readString(configPath, StandardCharsets.UTF_8);
        String requireIpPrefix = "Require ip "; //$NON-NLS-1$
        Matcher prefixMatcher = REQUIRE_IP_PATTERN.matcher(content);
        if (prefixMatcher.find()) {
            requireIpPrefix = prefixMatcher.group(1);
        }
        String normalizedValue = ipValue == null ? "" : ipValue.trim(); //$NON-NLS-1$
        String[] rawLines = normalizedValue.isEmpty() ? new String[0] : normalizedValue.split("\\r?\\n"); //$NON-NLS-1$
        StringBuilder replacement = new StringBuilder();
        for (String line : rawLines) {
            String candidate = line.trim();
            if (candidate.isEmpty()) {
                continue;
            }
            if (REQUIRE_IP_FULL_PATTERN.matcher(candidate).matches()) {
                candidate = candidate.replaceFirst("^\\s*Require\\s+ip\\s+", ""); //$NON-NLS-1$ //$NON-NLS-2$
            } else if (candidate.startsWith("Require ")) { //$NON-NLS-1$
                continue;
            }
            if (replacement.length() > 0) {
                replacement.append("\n"); //$NON-NLS-1$
            }
            replacement.append(requireIpPrefix).append(candidate);
        }

        String[] lines = content.split("\\r?\\n", -1); //$NON-NLS-1$
        StringBuilder updated = new StringBuilder();
        boolean replaced = false;
        for (String line : lines) {
            if (REQUIRE_IP_PATTERN.matcher(line).matches()) {
                if (!replaced && replacement.length() > 0) {
                    updated.append(replacement);
                }
                replaced = true;
                continue;
            }
            if (updated.length() > 0) {
                updated.append("\n"); //$NON-NLS-1$
            }
            updated.append(line);
        }
        if (!replaced) {
            throw new IOException("Require ip line not found in z-ovirt-engine-proxy.conf"); //$NON-NLS-1$
        }
        if (!updated.toString().equals(content)) {
            Files.writeString(configPath, updated.toString(), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }
}
