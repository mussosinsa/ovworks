package org.ovirt.engine.core.bll;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.ActionParametersBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonTransactiveCommandAttribute
public class ApplyExternalSslCommand extends CommandBase<ActionParametersBase> {

    private static final Logger log = LoggerFactory.getLogger(ApplyExternalSslCommand.class);

    public ApplyExternalSslCommand(ActionParametersBase parameters, CommandContext cmdContext) {
        super(parameters, cmdContext);
    }

    @Override
    protected void executeCommand() {
        try {
            runCommand(
                    Arrays.asList(
                            "/usr/bin/engine-setup", //$NON-NLS-1$
                            "--offline", //$NON-NLS-1$
                            "--accept-defaults", //$NON-NLS-1$
                            "--otopi-environment=OVESETUP_APACHE_CONFIG_SSL=bool:True" //$NON-NLS-1$
                    ),
                    "engine-setup external SSL apply"); //$NON-NLS-1$
            runCommand(
                    Arrays.asList(
                            "/usr/bin/systemctl", //$NON-NLS-1$
                            "restart", //$NON-NLS-1$
                            "httpd", //$NON-NLS-1$
                            "ovirt-engine" //$NON-NLS-1$
                    ),
                    "service restart"); //$NON-NLS-1$
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

    private void runCommand(List<String> command, String commandName) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
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
