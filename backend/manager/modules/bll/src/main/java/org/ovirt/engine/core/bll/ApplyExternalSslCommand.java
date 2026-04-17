package org.ovirt.engine.core.bll;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.ActionParametersBase;
import org.ovirt.engine.core.common.action.ActionReturnValue;
import org.ovirt.engine.core.common.action.ActionType;
import org.ovirt.engine.core.common.action.VdsActionParameters;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.dao.VdsDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonTransactiveCommandAttribute
public class ApplyExternalSslCommand extends CommandBase<ActionParametersBase> {

    private static final Logger log = LoggerFactory.getLogger(ApplyExternalSslCommand.class);

    private static final String ENGINE_SETUP = "/usr/bin/engine-setup"; //$NON-NLS-1$
    private static final String SYSTEMCTL = "/usr/bin/systemctl"; //$NON-NLS-1$
    private static final String OPENSSL = "/usr/bin/openssl"; //$NON-NLS-1$
    private static final String ENGINE_CERT_FILE = "/etc/pki/ovirt-engine/certs/apache.cer"; //$NON-NLS-1$

    @Inject
    private VdsDao vdsDao;

    public ApplyExternalSslCommand(ActionParametersBase parameters, CommandContext cmdContext) {
        super(parameters, cmdContext);
    }

    @Override
    protected void executeCommand() {
        try {
            validatePrerequisites();
            validateHostsForCertificateEnrollment();

            runCommand(
                    Arrays.asList(
                            ENGINE_SETUP,
                            "--offline", //$NON-NLS-1$
                            "--accept-defaults", //$NON-NLS-1$
                            "--otopi-environment=OVESETUP_APACHE_CONFIG_SSL=bool:True" //$NON-NLS-1$
                    ),
                    "engine-setup external SSL apply"); //$NON-NLS-1$

            runCommand(Arrays.asList(SYSTEMCTL, "restart", "httpd"), "restart httpd"); //$NON-NLS-1$ //$NON-NLS-2$
            runCommand(Arrays.asList(SYSTEMCTL, "restart", "ovirt-engine"), "restart ovirt-engine"); //$NON-NLS-1$ //$NON-NLS-2$

            verifyEngineSslActivation();
            enrollHostCertificates();

            setSucceeded(true);
        } catch (IOException | InterruptedException ex) {
            log.error("Failed to apply external SSL", ex); //$NON-NLS-1$
            getReturnValue().getExecuteFailedMessages().add(ex.getMessage());
            setSucceeded(false);
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void validatePrerequisites() throws IOException {
        validateExecutableExists(ENGINE_SETUP);
        validateExecutableExists(SYSTEMCTL);
        validateExecutableExists(OPENSSL);
    }

    private void validateExecutableExists(String executablePath) throws IOException {
        File executable = new File(executablePath);
        if (!executable.exists() || !executable.canExecute()) {
            throw new IOException("Required executable is missing or not executable: " + executablePath); //$NON-NLS-1$
        }
    }

    private void validateHostsForCertificateEnrollment() throws IOException {
        List<VDS> hosts = vdsDao.getAll();
        List<String> invalidHosts = hosts.stream()
                .filter(host -> !isAllowedEnrollStatus(host.getStatus()))
                .map(host -> host.getName() + "(" + host.getStatus() + ")") //$NON-NLS-1$ //$NON-NLS-2$
                .collect(Collectors.toList());

        if (!invalidHosts.isEmpty()) {
            throw new IOException(
                    "All hosts must be in Maintenance, InstallFailed, or NonResponsive before external SSL apply. " //$NON-NLS-1$
                            + "Please move hosts and retry. Invalid hosts: " + String.join(", ", invalidHosts)); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private boolean isAllowedEnrollStatus(VDSStatus status) {
        return status == VDSStatus.Maintenance
                || status == VDSStatus.InstallFailed
                || status == VDSStatus.NonResponsive;
    }

    private void verifyEngineSslActivation() throws IOException, InterruptedException {
        runCommand(Arrays.asList(SYSTEMCTL, "is-active", "--quiet", "httpd"), "verify httpd is active"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        runCommand(Arrays.asList(SYSTEMCTL, "is-active", "--quiet", "ovirt-engine"), "verify ovirt-engine is active"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        runCommand(
                Arrays.asList(
                        OPENSSL,
                        "x509", //$NON-NLS-1$
                        "-in", //$NON-NLS-1$
                        ENGINE_CERT_FILE,
                        "-noout", //$NON-NLS-1$
                        "-subject", //$NON-NLS-1$
                        "-issuer", //$NON-NLS-1$
                        "-dates" //$NON-NLS-1$
                ),
                "verify engine Apache certificate"); //$NON-NLS-1$
    }

    private void enrollHostCertificates() throws IOException {
        List<VDS> hosts = vdsDao.getAll();
        List<String> failedHosts = new ArrayList<>();

        for (VDS host : hosts) {
            ActionReturnValue returnValue = runInternalAction(
                    ActionType.HostEnrollCertificateInternal,
                    new VdsActionParameters(host.getId()),
                    cloneContextAndDetachFromParent());
            if (returnValue == null || !returnValue.getSucceeded()) {
                String details = "unknown failure"; //$NON-NLS-1$
                if (returnValue != null
                        && returnValue.getExecuteFailedMessages() != null
                        && !returnValue.getExecuteFailedMessages().isEmpty()) {
                    details = String.join(", ", returnValue.getExecuteFailedMessages()); //$NON-NLS-1$
                }
                failedHosts.add(host.getName() + ": " + details); //$NON-NLS-1$
            }
        }

        if (!failedHosts.isEmpty()) {
            throw new IOException("Host certificate enrollment failed: " + String.join("; ", failedHosts)); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private void runCommand(List<String> command, String commandName) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException(commandName + " failed: " + output.toString().trim()); //$NON-NLS-1$
        }
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        return Collections.singletonList(new PermissionSubject(
                MultiLevelAdministrationHandler.SYSTEM_OBJECT_ID,
                VdcObjectType.System,
                getActionType().getActionGroup()));
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return AuditLogType.UNASSIGNED;
    }
}
