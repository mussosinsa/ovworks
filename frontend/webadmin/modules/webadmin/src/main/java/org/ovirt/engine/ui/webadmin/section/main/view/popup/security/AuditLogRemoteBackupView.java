package org.ovirt.engine.ui.webadmin.section.main.view.popup.security;

import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.constants.ButtonType;
import org.ovirt.engine.core.common.action.ActionParametersBase;
import org.ovirt.engine.core.common.action.ActionType;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.gin.AssetProvider;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;

public class AuditLogRemoteBackupView extends Composite {

    private static final ApplicationConstants constants = AssetProvider.getConstants();

    private Button remoteBackupButton;
    private Label remoteBackupStatusLabel;

    public AuditLogRemoteBackupView() {
        FlowPanel mainPanel = new FlowPanel();
        mainPanel.getElement().getStyle().setProperty("padding", "20px"); //$NON-NLS-1$ //$NON-NLS-2$

        // Title
        HTML title = new HTML("<h3>감사기록 원격 백업</h3>"); //$NON-NLS-1$
        title.getElement().getStyle().setProperty("marginBottom", "20px"); //$NON-NLS-1$ //$NON-NLS-2$
        mainPanel.add(title);

        // Remote backup section
        FlowPanel remoteSection = new FlowPanel();
        remoteSection.getElement().getStyle().setProperty("marginBottom", "30px"); //$NON-NLS-1$ //$NON-NLS-2$

        remoteBackupButton = new Button("원격 주소(/etc/rsyslog.conf)"); //$NON-NLS-1$
        remoteBackupButton.setType(ButtonType.PRIMARY);
        remoteBackupButton.getElement().getStyle().setProperty("marginBottom", "10px"); //$NON-NLS-1$ //$NON-NLS-2$
        remoteSection.add(remoteBackupButton);

        remoteBackupStatusLabel = new Label();
        remoteBackupStatusLabel.getElement().getStyle().setProperty("display", "block"); //$NON-NLS-1$ //$NON-NLS-2$
        remoteBackupStatusLabel.getElement().getStyle().setProperty("marginLeft", "10px"); //$NON-NLS-1$ //$NON-NLS-2$
        remoteSection.add(remoteBackupStatusLabel);

        mainPanel.add(remoteSection);

        initializeHandlers();
        initWidget(mainPanel);
    }

    private void initializeHandlers() {
        remoteBackupButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                remoteBackupStatusLabel.setText(constants.statusRunning());
                remoteBackupStatusLabel.removeStyleName("text-success"); //$NON-NLS-1$
                remoteBackupStatusLabel.addStyleName("text-warning"); //$NON-NLS-1$
                executeRemoteBackup();
            }
        });
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
}
