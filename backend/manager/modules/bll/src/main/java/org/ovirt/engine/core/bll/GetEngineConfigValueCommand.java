package org.ovirt.engine.core.bll;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.common.action.EngineConfigValueParameters;

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
            getReturnValue().setActionReturnValue(output);
            if (exitCode == 0) {
                setSucceeded(true);
            } else {
                getReturnValue().getExecuteFailedMessages().add(output);
                setSucceeded(false);
            }
        } catch (Exception e) {
            log.error("Failed to get engine-config value", e); //$NON-NLS-1$
            getReturnValue().getExecuteFailedMessages().add(e.getMessage());
            setSucceeded(false);
        }
    }
}
