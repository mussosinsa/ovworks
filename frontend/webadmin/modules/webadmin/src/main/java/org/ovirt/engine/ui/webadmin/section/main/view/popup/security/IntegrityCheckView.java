package org.ovirt.engine.ui.webadmin.section.main.view.popup.security;

import org.gwtbootstrap3.client.ui.Button;
import org.ovirt.engine.core.common.action.ActionParametersBase;
import org.ovirt.engine.core.common.action.ActionType;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.uicompat.FrontendActionAsyncResult;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.gin.AssetProvider;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class IntegrityCheckView extends Composite {

    interface ViewUiBinder extends UiBinder<Widget, IntegrityCheckView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    private static final ApplicationConstants constants = AssetProvider.getConstants();

    @UiField
    Button securityAuditButton;

    @UiField
    Button integrityVerificationButton;

    @UiField
    Label securityAuditStatusLabel;

    @UiField
    Label integrityVerificationStatusLabel;

    @UiField
    HTML securityAuditErrorLabel;

    @UiField
    HTML integrityVerificationErrorLabel;

    public IntegrityCheckView() {
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        initializeHandlers();
    }

    private void initializeHandlers() {
        securityAuditButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                securityAuditStatusLabel.setText(constants.statusRunning());
                securityAuditStatusLabel.removeStyleName("text-success"); //$NON-NLS-1$
                securityAuditStatusLabel.removeStyleName("text-danger"); //$NON-NLS-1$
                securityAuditStatusLabel.addStyleName("text-warning"); //$NON-NLS-1$
                securityAuditErrorLabel.setVisible(false);
                executeSecurityAudit();
            }
        });

        integrityVerificationButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                integrityVerificationStatusLabel.setText(constants.statusRunning());
                integrityVerificationStatusLabel.removeStyleName("text-success"); //$NON-NLS-1$
                integrityVerificationStatusLabel.removeStyleName("text-danger"); //$NON-NLS-1$
                integrityVerificationStatusLabel.addStyleName("text-warning"); //$NON-NLS-1$
                integrityVerificationErrorLabel.setVisible(false);
                executeIntegrityVerification();
            }
        });
    }

    private void executeSecurityAudit() {
        Frontend.getInstance().runAction(
            ActionType.SecurityAudit,
            new ActionParametersBase(),
            result -> {
                if (result != null && result.getReturnValue() != null && result.getReturnValue().getSucceeded()) {
                    securityAuditStatusLabel.setText(constants.statusNormal());
                    securityAuditStatusLabel.removeStyleName("text-warning"); //$NON-NLS-1$
                    securityAuditStatusLabel.addStyleName("text-success"); //$NON-NLS-1$
                    securityAuditErrorLabel.setVisible(false);
                } else {
                    securityAuditStatusLabel.setText(constants.statusFailed());
                    securityAuditStatusLabel.removeStyleName("text-warning"); //$NON-NLS-1$
                    securityAuditStatusLabel.addStyleName("text-danger"); //$NON-NLS-1$
                    // Display error details
                    showErrorDetails(result, securityAuditErrorLabel);
                }
            }
        );
    }

    private void executeIntegrityVerification() {
        Frontend.getInstance().runAction(
            ActionType.IntegrityVerification,
            new ActionParametersBase(),
            result -> {
                if (result != null && result.getReturnValue() != null && result.getReturnValue().getSucceeded()) {
                    integrityVerificationStatusLabel.setText(constants.statusNormal());
                    integrityVerificationStatusLabel.removeStyleName("text-warning"); //$NON-NLS-1$
                    integrityVerificationStatusLabel.addStyleName("text-success"); //$NON-NLS-1$
                    integrityVerificationErrorLabel.setVisible(false);
                } else {
                    integrityVerificationStatusLabel.setText(constants.statusFailed());
                    integrityVerificationStatusLabel.removeStyleName("text-warning"); //$NON-NLS-1$
                    integrityVerificationStatusLabel.addStyleName("text-danger"); //$NON-NLS-1$
                    // Display error details
                    showErrorDetails(result, integrityVerificationErrorLabel);
                }
            }
        );
    }

    private void showErrorDetails(FrontendActionAsyncResult result, HTML errorLabel) {
        StringBuilder errorMsg = new StringBuilder();

        if (result != null && result.getReturnValue() != null) {
            // Get error messages from executeFailedMessages
            if (result.getReturnValue().getExecuteFailedMessages() != null
                    && !result.getReturnValue().getExecuteFailedMessages().isEmpty()) {
                for (String msg : result.getReturnValue().getExecuteFailedMessages()) {
                    errorMsg.append(msg).append("\n"); //$NON-NLS-1$
                }
            }

            // Get action return value if it contains output
            Object actionReturnValue = result.getReturnValue().getActionReturnValue();
            if (actionReturnValue != null && actionReturnValue instanceof String) {
                String output = (String) actionReturnValue;
                if (!output.isEmpty()) {
                    if (errorMsg.length() > 0) {
                        errorMsg.append("\n"); //$NON-NLS-1$
                    }
                    errorMsg.append(output);
                }
            }
        }

        if (errorMsg.length() > 0) {
            String htmlContent = SafeHtmlUtils.fromString(errorMsg.toString().trim())
                    .asString()
                    .replace("\n", "<br/>"); //$NON-NLS-1$ //$NON-NLS-2$
            errorLabel.setHTML(htmlContent);
            errorLabel.setVisible(true);
        } else {
            errorLabel.setVisible(false);
        }
    }
}
