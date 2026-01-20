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
import org.ovirt.engine.core.common.action.UserPasswordResetParameters;
import org.ovirt.engine.core.common.businessentities.aaa.DbUser;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.DbUserDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResetUserPasswordCommand extends CommandBase<UserPasswordResetParameters> {

    private static final Logger log = LoggerFactory.getLogger(ResetUserPasswordCommand.class);

    @Inject
    private DbUserDao dbUserDao;

    /**
     * Constructor for command creation when compensation is applied on startup
     */
    public ResetUserPasswordCommand(Guid commandId) {
        super(commandId);
    }

    public ResetUserPasswordCommand(UserPasswordResetParameters parameters, CommandContext cmdContext) {
        super(parameters, cmdContext);
    }

    @Override
    protected void executeCommand() {
        Guid userId = getParameters().getUserId();
        String newPassword = getParameters().getNewPassword();

        DbUser user = dbUserDao.get(userId);
        if (user == null) {
            setSucceeded(false);
            return;
        }

        String username = user.getLoginName();

        try {
            // Execute ovirt-aaa-jdbc-tool command
            ProcessBuilder processBuilder = new ProcessBuilder(
                "ovirt-aaa-jdbc-tool",
                "user",
                "password-reset",
                username,
                "--password=" + newPassword
            );

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Read the output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.info("Successfully reset password for user: {}", username);
                setSucceeded(true);
            } else {
                log.error("Failed to reset password for user: {}. Exit code: {}. Output: {}",
                        username, exitCode, output.toString());
                setSucceeded(false);
            }

        } catch (IOException | InterruptedException e) {
            log.error("Error executing ovirt-aaa-jdbc-tool for user: {}", username, e);
            setSucceeded(false);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    protected boolean validate() {
        Guid userId = getParameters().getUserId();
        String newPassword = getParameters().getNewPassword();

        // Check that password is provided
        if (newPassword == null || newPassword.trim().isEmpty()) {
            addValidationMessage(EngineMessage.ACTION_TYPE_FAILED_PASSWORD_MUST_BE_SPECIFIED);
            return false;
        }

        // Check that the user exists in the database
        DbUser user = dbUserDao.get(userId);
        if (user == null) {
            addValidationMessage(EngineMessage.USER_MUST_EXIST_IN_DB);
            return false;
        }

        // Check that it's not a group
        if (user.isGroup()) {
            return failValidation(EngineMessage.ACTION_TYPE_FAILED_PASSWORD_CANNOT_BE_RESET_FOR_GROUP);
        }

        return true;
    }

    @Override
    protected void setActionMessageParameters() {
        addValidationMessage(EngineMessage.VAR__ACTION__RESET);
        addValidationMessage(EngineMessage.VAR__TYPE__USER);
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.USER_PASSWORD_CHANGED : AuditLogType.USER_PASSWORD_CHANGE_FAILED;
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        return Collections.singletonList(new PermissionSubject(
                MultiLevelAdministrationHandler.SYSTEM_OBJECT_ID,
                VdcObjectType.System,
                getActionType().getActionGroup()));
    }
}
