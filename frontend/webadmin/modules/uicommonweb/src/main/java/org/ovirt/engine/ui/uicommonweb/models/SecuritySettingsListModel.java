package org.ovirt.engine.ui.uicommonweb.models;

import org.ovirt.engine.core.common.action.ActionParametersBase;
import org.ovirt.engine.core.common.action.ActionType;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.uicommonweb.UICommand;

public class SecuritySettingsListModel extends ListWithDetailsModel {

    private UICommand securityAuditCommand;
    private UICommand integrityVerificationCommand;

    public UICommand getSecurityAuditCommand() {
        return securityAuditCommand;
    }

    private void setSecurityAuditCommand(UICommand value) {
        securityAuditCommand = value;
    }

    public UICommand getIntegrityVerificationCommand() {
        return integrityVerificationCommand;
    }

    private void setIntegrityVerificationCommand(UICommand value) {
        integrityVerificationCommand = value;
    }

    public SecuritySettingsListModel() {
        super();
        setTitle("Security Settings"); //$NON-NLS-1$
        // HelpTag setHelpTag(HelpTag.content);
        setHashName("security_settings"); //$NON-NLS-1$

        // Initialize commands
        setSecurityAuditCommand(new UICommand("SecurityAudit", this)); //$NON-NLS-1$
        setIntegrityVerificationCommand(new UICommand("IntegrityVerification", this)); //$NON-NLS-1$
    }

    @Override
    protected void onEntityChanged() {
        super.onEntityChanged();
    }

    @Override
    protected void syncSearch() {
        super.syncSearch();
    }

    @Override
    protected Object provideDetailModelEntity(Object selectedItem) {
        return selectedItem;
    }

    @Override
    protected String getListName() {
        return "SecuritySettingsListModel"; //$NON-NLS-1$
    }

    @Override
    public void executeCommand(UICommand command) {
        super.executeCommand(command);

        if (command == getSecurityAuditCommand()) {
            executeSecurityAudit();
        } else if (command == getIntegrityVerificationCommand()) {
            executeIntegrityVerification();
        }
    }

    private void executeSecurityAudit() {
        Frontend.getInstance().runAction(
            ActionType.SecurityAudit,
            new ActionParametersBase(),
            result -> {
                // Handle result if needed
                if (result != null && result.getSucceeded()) {
                    // Audit completed successfully
                } else {
                    // Audit failed
                }
            }
        );
    }

    private void executeIntegrityVerification() {
        Frontend.getInstance().runAction(
            ActionType.IntegrityVerification,
            new ActionParametersBase(),
            result -> {
                // Handle result if needed
                if (result != null && result.getSucceeded()) {
                    // Verification completed successfully
                } else {
                    // Verification failed
                }
            }
        );
    }
}
