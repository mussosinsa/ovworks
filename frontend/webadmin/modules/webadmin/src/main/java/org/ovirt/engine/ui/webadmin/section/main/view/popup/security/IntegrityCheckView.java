package org.ovirt.engine.ui.webadmin.section.main.view.popup.security;

import org.gwtbootstrap3.client.ui.Button;
import org.ovirt.engine.core.common.action.ActionParametersBase;
import org.ovirt.engine.core.common.action.ActionType;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.gin.AssetProvider;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
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
                securityAuditStatusLabel.addStyleName("text-warning"); //$NON-NLS-1$
                executeSecurityAudit();
            }
        });

        integrityVerificationButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                integrityVerificationStatusLabel.setText(constants.statusRunning());
                integrityVerificationStatusLabel.removeStyleName("text-success"); //$NON-NLS-1$
                integrityVerificationStatusLabel.addStyleName("text-warning"); //$NON-NLS-1$
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
                } else {
                    securityAuditStatusLabel.setText(constants.statusFailed());
                    securityAuditStatusLabel.removeStyleName("text-warning"); //$NON-NLS-1$
                    securityAuditStatusLabel.addStyleName("text-danger"); //$NON-NLS-1$
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
                } else {
                    integrityVerificationStatusLabel.setText(constants.statusFailed());
                    integrityVerificationStatusLabel.removeStyleName("text-warning"); //$NON-NLS-1$
                    integrityVerificationStatusLabel.addStyleName("text-danger"); //$NON-NLS-1$
                }
            }
        );
    }
}
