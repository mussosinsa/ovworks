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

public class AuditLogProtectionView extends Composite {

    private static final ApplicationConstants constants = AssetProvider.getConstants();

    private Button fullLogBackupButton;
    private Label fullLogBackupStatusLabel;

    public AuditLogProtectionView() {
        FlowPanel mainPanel = new FlowPanel();
        mainPanel.getElement().getStyle().setProperty("padding", "20px"); //$NON-NLS-1$ //$NON-NLS-2$

        // Title
        HTML title = new HTML("<h3>감사기록 보호</h3>"); //$NON-NLS-1$
        title.getElement().getStyle().setProperty("marginBottom", "20px"); //$NON-NLS-1$ //$NON-NLS-2$
        mainPanel.add(title);

        // Full log backup section
        FlowPanel backupSection = new FlowPanel();
        backupSection.getElement().getStyle().setProperty("marginBottom", "30px"); //$NON-NLS-1$ //$NON-NLS-2$

        fullLogBackupButton = new Button("전체 로그 백업"); //$NON-NLS-1$
        fullLogBackupButton.setType(ButtonType.PRIMARY);
        fullLogBackupButton.getElement().getStyle().setProperty("marginBottom", "10px"); //$NON-NLS-1$ //$NON-NLS-2$
        backupSection.add(fullLogBackupButton);

        fullLogBackupStatusLabel = new Label();
        fullLogBackupStatusLabel.getElement().getStyle().setProperty("display", "block"); //$NON-NLS-1$ //$NON-NLS-2$
        fullLogBackupStatusLabel.getElement().getStyle().setProperty("marginLeft", "10px"); //$NON-NLS-1$ //$NON-NLS-2$
        backupSection.add(fullLogBackupStatusLabel);

        mainPanel.add(backupSection);

        initializeHandlers();
        initWidget(mainPanel);
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
}
