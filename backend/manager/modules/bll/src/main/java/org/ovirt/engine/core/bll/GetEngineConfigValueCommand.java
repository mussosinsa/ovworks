package org.ovirt.engine.core.bll;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.EngineConfigValueParameters;
import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.compat.Guid;

public class GetEngineConfigValueCommand<T extends EngineConfigValueParameters> extends CommandBase<T> {

    public GetEngineConfigValueCommand(T parameters, CommandContext cmdContext) {
        super(parameters, cmdContext);
    }

    @Override
    protected boolean validate() {
        return getParameters().getKey() != null && !getParameters().getKey().trim().isEmpty();
    }

    @Override
    protected void executeCommand() {
        try {
            ProcessBuilder pb = new ProcessBuilder("engine-config", "-g", getParameters().getKey().trim()); //$NON-NLS-1$ //$NON-NLS-2$
            pb.redirectErrorStream(true);
            Process p = pb.start();

            StringBuilder out = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    out.append(line).append('\n');
                }
            }

            int exitCode = p.waitFor();
            String output = out.toString().trim();
            if (exitCode == 0) {
                getReturnValue().setActionReturnValue(output);
                setSucceeded(true);
            } else {
                String normalizedOutput = isMissingEngineConfigKeyOutput(output)
                        ? "존재하지 않는 변수입니다. 다시확인하세요" : output; //$NON-NLS-1$
                getReturnValue().setActionReturnValue(normalizedOutput);
                getReturnValue().getExecuteFailedMessages().add(normalizedOutput);
                setSucceeded(false);
            }
        } catch (Exception e) {
            log.error("Failed to get engine-config value", e); //$NON-NLS-1$
            getReturnValue().getExecuteFailedMessages().add(e.getMessage());
            setSucceeded(false);
        }
    }


    private boolean isMissingEngineConfigKeyOutput(String output) {
        String normalized = output == null ? "" : output.toLowerCase(); //$NON-NLS-1$
        return normalized.contains("no such") //$NON-NLS-1$
                || normalized.contains("not found") //$NON-NLS-1$
                || normalized.contains("does not exist") //$NON-NLS-1$
                || normalized.contains("doesn't exist") //$NON-NLS-1$
                || normalized.contains("there is no variable") //$NON-NLS-1$
                || normalized.contains("no variable named"); //$NON-NLS-1$
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        return Collections.singletonList(new PermissionSubject(Guid.SYSTEM, VdcObjectType.System,
                ActionGroup.CONFIGURE_ENGINE));
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return AuditLogType.UNASSIGNED;
    }
}
