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

public class AvailabilityView extends Composite {

    private static final ApplicationConstants constants = AssetProvider.getConstants();

    private Button engineBackupButton;
    private Label engineBackupStatusLabel;

    public AvailabilityView() {
        FlowPanel mainPanel = new FlowPanel();
        mainPanel.getElement().getStyle().setProperty("padding", "20px"); //$NON-NLS-1$ //$NON-NLS-2$

        // Title
        HTML title = new HTML("<h3>가용성 확보</h3>"); //$NON-NLS-1$
        title.getElement().getStyle().setProperty("marginBottom", "20px"); //$NON-NLS-1$ //$NON-NLS-2$
        mainPanel.add(title);

        // Engine backup section
        FlowPanel engineSection = new FlowPanel();
        engineSection.getElement().getStyle().setProperty("marginBottom", "30px"); //$NON-NLS-1$ //$NON-NLS-2$

        engineBackupButton = new Button("engine-backup 실행"); //$NON-NLS-1$
        engineBackupButton.setType(ButtonType.PRIMARY);
        engineBackupButton.getElement().getStyle().setProperty("marginBottom", "10px"); //$NON-NLS-1$ //$NON-NLS-2$
        engineSection.add(engineBackupButton);

        engineBackupStatusLabel = new Label();
        engineBackupStatusLabel.getElement().getStyle().setProperty("display", "block"); //$NON-NLS-1$ //$NON-NLS-2$
        engineBackupStatusLabel.getElement().getStyle().setProperty("marginLeft", "10px"); //$NON-NLS-1$ //$NON-NLS-2$
        engineSection.add(engineBackupStatusLabel);

        mainPanel.add(engineSection);

        initializeHandlers();
        initWidget(mainPanel);
    }

    private void initializeHandlers() {
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
