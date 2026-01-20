package org.ovirt.engine.core.bll;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.ActionParametersBase;
import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.AuditLogDao;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogable;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command to verify integrity of critical engine files using SHA-256 checksums
 */
public class IntegrityVerificationCommand<T extends ActionParametersBase> extends CommandBase<T> {

    private static final Logger log = LoggerFactory.getLogger(IntegrityVerificationCommand.class);
    private static final String CHECKSUM_FILE = "/var/lib/ovirt-engine/integrity/checksums.txt";
    private static final String ENGINE_LIB_PATH = "/usr/share/ovirt-engine/modules";
    private static final String ENGINE_CONFIG_PATH = "/etc/ovirt-engine";

    @Inject
    private AuditLogDao auditLogDao;

    @Inject
    private AuditLogDirector auditLogDirector;

    private int filesChecked = 0;
    private int filesModified = 0;
    private int filesMissing = 0;
    private List<String> modifiedFiles = new ArrayList<>();
    private List<String> missingFiles = new ArrayList<>();

    public IntegrityVerificationCommand(T parameters, CommandContext cmdContext) {
        super(parameters, cmdContext);
    }

    @Override
    protected boolean validate() {
        return true;
    }

    @Override
    protected void executeCommand() {
        logAuditEvent(AuditLogType.INTEGRITY_VERIFICATION_STARTED, "Integrity verification started");

        try {
            Map<String, String> storedChecksums = loadStoredChecksums();

            if (storedChecksums.isEmpty()) {
                log.info("No stored checksums found. Creating baseline...");
                createChecksumBaseline();
                logAuditEvent(AuditLogType.INTEGRITY_VERIFICATION_COMPLETED,
                    "Integrity verification baseline created with " + filesChecked + " files");
                setSucceeded(true);
                return;
            }

            // Verify integrity against stored checksums
            verifyIntegrity(storedChecksums);

            // Prepare result summary
            StringBuilder summary = new StringBuilder();
            summary.append("Integrity verification completed. ");
            summary.append("Files checked: ").append(filesChecked).append(", ");
            summary.append("Modified: ").append(filesModified).append(", ");
            summary.append("Missing: ").append(filesMissing);

            if (filesModified > 0 || filesMissing > 0) {
                logAuditEvent(AuditLogType.INTEGRITY_VERIFICATION_WARNING, summary.toString());

                // Log details for each modified file
                for (String file : modifiedFiles) {
                    logAuditEvent(AuditLogType.INTEGRITY_VERIFICATION_FILE_MODIFIED,
                        "File modified: " + file);
                }

                // Log details for each missing file
                for (String file : missingFiles) {
                    logAuditEvent(AuditLogType.INTEGRITY_VERIFICATION_FILE_MISSING,
                        "File missing: " + file);
                }

                setSucceeded(false);
            } else {
                logAuditEvent(AuditLogType.INTEGRITY_VERIFICATION_COMPLETED, summary.toString());
                setSucceeded(true);
            }

            // Store the result details
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("filesChecked", filesChecked);
            resultMap.put("filesModified", filesModified);
            resultMap.put("filesMissing", filesMissing);
            resultMap.put("modifiedFiles", modifiedFiles);
            resultMap.put("missingFiles", missingFiles);
            getReturnValue().setActionReturnValue(resultMap);

        } catch (Exception e) {
            log.error("Failed to execute integrity verification", e);
            logAuditEvent(AuditLogType.INTEGRITY_VERIFICATION_FAILED,
                "Integrity verification failed with error: " + e.getMessage());
            setSucceeded(false);
        }
    }

    private Map<String, String> loadStoredChecksums() {
        Map<String, String> checksums = new HashMap<>();
        File checksumFile = new File(CHECKSUM_FILE);

        if (!checksumFile.exists()) {
            return checksums;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(checksumFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+", 2);
                if (parts.length == 2) {
                    checksums.put(parts[1], parts[0]);
                }
            }
        } catch (IOException e) {
            log.error("Failed to load stored checksums", e);
        }

        return checksums;
    }

    private void createChecksumBaseline() throws IOException {
        File checksumFile = new File(CHECKSUM_FILE);
        checksumFile.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(checksumFile)) {
            // Calculate checksums for critical directories
            calculateChecksumsForDirectory(new File(ENGINE_LIB_PATH), writer);
            calculateChecksumsForDirectory(new File(ENGINE_CONFIG_PATH), writer);
        }
    }

    private void calculateChecksumsForDirectory(File directory, FileWriter writer) throws IOException {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                calculateChecksumsForDirectory(file, writer);
            } else if (file.isFile() && shouldCheckFile(file)) {
                String checksum = calculateChecksum(file);
                if (checksum != null) {
                    writer.write(checksum + "  " + file.getAbsolutePath() + "\n");
                    filesChecked++;
                }
            }
        }
    }

    private void verifyIntegrity(Map<String, String> storedChecksums) {
        for (Map.Entry<String, String> entry : storedChecksums.entrySet()) {
            String filePath = entry.getKey();
            String storedChecksum = entry.getValue();

            File file = new File(filePath);
            filesChecked++;

            if (!file.exists()) {
                filesMissing++;
                missingFiles.add(filePath);
                log.warn("File missing: {}", filePath);
                continue;
            }

            String currentChecksum = calculateChecksum(file);
            if (currentChecksum != null && !currentChecksum.equals(storedChecksum)) {
                filesModified++;
                modifiedFiles.add(filePath);
                log.warn("File modified: {}", filePath);
            }
        }
    }

    private boolean shouldCheckFile(File file) {
        String name = file.getName();
        // Check important file types
        return name.endsWith(".jar") ||
               name.endsWith(".war") ||
               name.endsWith(".ear") ||
               name.endsWith(".conf") ||
               name.endsWith(".properties") ||
               name.endsWith(".xml");
    }

    private String calculateChecksum(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Failed to calculate checksum for file: {}", file.getAbsolutePath(), e);
            return null;
        }
    }

    private void logAuditEvent(AuditLogType type, String message) {
        AuditLogable logable = new AuditLogableImpl();
        logable.setUserId(getCurrentUser().getId());
        logable.setUserName(getCurrentUser().getLoginName());
        logable.setCustomData(message);
        auditLogDirector.log(logable, type);
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        return Collections.singletonList(new PermissionSubject(Guid.SYSTEM,
                VdcObjectType.System,
                ActionGroup.AUDIT_LOG_MANAGEMENT));
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        if (filesModified > 0 || filesMissing > 0) {
            return AuditLogType.INTEGRITY_VERIFICATION_WARNING;
        }
        return getSucceeded() ? AuditLogType.INTEGRITY_VERIFICATION_COMPLETED : AuditLogType.INTEGRITY_VERIFICATION_FAILED;
    }
}
