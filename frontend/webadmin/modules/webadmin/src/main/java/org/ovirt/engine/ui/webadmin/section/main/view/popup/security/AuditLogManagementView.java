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

public class AuditLogManagementView extends Composite {

    interface ViewUiBinder extends UiBinder<Widget, AuditLogManagementView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    private static final ApplicationConstants constants = AssetProvider.getConstants();

    @UiField
    Button fullLogBackupButton;

    @UiField
    Button remoteBackupButton;

    @UiField
    Button engineBackupButton;

    @UiField
    Label fullLogBackupStatusLabel;

    @UiField
    Label remoteBackupStatusLabel;

    @UiField
    Label engineBackupStatusLabel;

    public AuditLogManagementView() {
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        initializeHandlers();
    }

    private void initializeHandlers() {
        fullLogBackupButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                fullLogBackupStatusLabel.setText(constants.statusRunning());
                fullLogBackupStatusLabel.removeStyleName("text-success"); //$NON-NLS-1$
                fullLogBackupStatusLabel.addStyleName("text-warning"); //$NON-NLS-1$
                executeFullLogBackup();
            }
        });

        remoteBackupButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                remoteBackupStatusLabel.setText(constants.statusRunning());
                remoteBackupStatusLabel.removeStyleName("text-success"); //$NON-NLS-1$
                remoteBackupStatusLabel.addStyleName("text-warning"); //$NON-NLS-1$
                executeRemoteBackup();
            }
        });

        engineBackupButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                engineBackupStatusLabel.setText(constants.statusRunning());
                engineBackupStatusLabel.removeStyleName("text-success"); //$NON-NLS-1$
                engineBackupStatusLabel.addStyleName("text-warning"); //$NON-NLS-1$
                executeEngineBackup();
            }
        });
    }

    private void executeFullLogBackup() {
        Frontend.getInstance().runAction(
            ActionType.FullLogBackup,
            new ActionParametersBase(),
            result -> {
                if (result != null && result.getReturnValue() != null && result.getReturnValue().getSucceeded()) {
                    fullLogBackupStatusLabel.setText(constants.statusNormal());
                    fullLogBackupStatusLabel.removeStyleName("text-warning"); //$NON-NLS-1$
                    fullLogBackupStatusLabel.addStyleName("text-success"); //$NON-NLS-1$
                } else {
                    fullLogBackupStatusLabel.setText(constants.statusFailed());
                    fullLogBackupStatusLabel.removeStyleName("text-warning"); //$NON-NLS-1$
                    fullLogBackupStatusLabel.addStyleName("text-danger"); //$NON-NLS-1$
                }
            }
        );
    }

    private void executeRemoteBackup() {
        Frontend.getInstance().runAction(
            ActionType.RemoteBackup,
            new ActionParametersBase(),
            result -> {
                if (result != null && result.getReturnValue() != null && result.getReturnValue().getSucceeded()) {
                    remoteBackupStatusLabel.setText(constants.statusNormal());
                    remoteBackupStatusLabel.removeStyleName("text-warning"); //$NON-NLS-1$
                    remoteBackupStatusLabel.addStyleName("text-success"); //$NON-NLS-1$
                } else {
                    remoteBackupStatusLabel.setText(constants.statusFailed());
                    remoteBackupStatusLabel.removeStyleName("text-warning"); //$NON-NLS-1$
                    remoteBackupStatusLabel.addStyleName("text-danger"); //$NON-NLS-1$
                }
            }
        );
    }

    private void executeEngineBackup() {
        Frontend.getInstance().runAction(
            ActionType.EngineBackup,
            new ActionParametersBase(),
            result -> {
                if (result != null && result.getReturnValue() != null && result.getReturnValue().getSucceeded()) {
                    engineBackupStatusLabel.setText(constants.statusNormal());
                    engineBackupStatusLabel.removeStyleName("text-warning"); //$NON-NLS-1$
                    engineBackupStatusLabel.addStyleName("text-success"); //$NON-NLS-1$
                } else {
                    engineBackupStatusLabel.setText(constants.statusFailed());
                    engineBackupStatusLabel.removeStyleName("text-warning"); //$NON-NLS-1$
                    engineBackupStatusLabel.addStyleName("text-danger"); //$NON-NLS-1$
                }
            }
        );
    }
}
