package org.ovirt.engine.core.bll.aaa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.ovirt.engine.core.bll.CommandBase;
import org.ovirt.engine.core.bll.MultiLevelAdministrationHandler;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.IdParameters;
import org.ovirt.engine.core.common.businessentities.aaa.DbUser;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.DbUserDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnlockUserCommand extends CommandBase<IdParameters> {

    private static final Logger log = LoggerFactory.getLogger(UnlockUserCommand.class);

    @Inject
    private DbUserDao dbUserDao;

    /**
     * Constructor for command creation when compensation is applied on startup
     */
    public UnlockUserCommand(Guid commandId) {
        super(commandId);
    }

    public UnlockUserCommand(IdParameters parameters, CommandContext cmdContext) {
        super(parameters, cmdContext);
    }

    @Override
    protected void executeCommand() {
        DbUser user = dbUserDao.get(getParameters().getId());
        if (user == null) {
            setSucceeded(false);
            return;
        }

        String username = user.getLoginName();

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "ovirt-aaa-jdbc-tool", //$NON-NLS-1$
                    "user", //$NON-NLS-1$
                    "unlock", //$NON-NLS-1$
                    "--user=" + username); //$NON-NLS-1$
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            }

            int exitCode = process.waitFor();
            String commandOutput = output.toString().trim();
            getReturnValue().setActionReturnValue(commandOutput);

            if (exitCode == 0) {
                setSucceeded(true);
            } else {
                getReturnValue().getExecuteFailedMessages().add(commandOutput);
                setSucceeded(false);
            }
        } catch (IOException | InterruptedException e) {
            log.error("Failed to unlock user", e); //$NON-NLS-1$
            getReturnValue().getExecuteFailedMessages().add(e.getMessage());
            setSucceeded(false);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    protected boolean validate() {
        if (getParameters().getId() == null || Guid.Empty.equals(getParameters().getId())) {
            return failValidation(EngineMessage.USER_MUST_EXIST_IN_DB);
        }

        DbUser user = dbUserDao.get(getParameters().getId());
        if (user == null) {
            return failValidation(EngineMessage.USER_MUST_EXIST_IN_DB);
        }

        if (user.isGroup()) {
            return failValidation(EngineMessage.ACTION_TYPE_FAILED_PASSWORD_CANNOT_BE_RESET_FOR_GROUP);
        }

        return true;
    }

    @Override
    protected void setActionMessageParameters() {
        addValidationMessage(EngineMessage.VAR__ACTION__UPDATE);
        addValidationMessage(EngineMessage.VAR__TYPE__USER);
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return AuditLogType.UNASSIGNED;
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        return Collections.singletonList(new PermissionSubject(
                MultiLevelAdministrationHandler.SYSTEM_OBJECT_ID,
                VdcObjectType.System,
                getActionType().getActionGroup()));
    }
}
