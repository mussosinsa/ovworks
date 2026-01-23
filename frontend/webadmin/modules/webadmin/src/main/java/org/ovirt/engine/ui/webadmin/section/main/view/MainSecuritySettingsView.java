package org.ovirt.engine.ui.webadmin.section.main.view;

import org.gwtbootstrap3.client.ui.Button;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.uicommon.model.MainModelProvider;
import org.ovirt.engine.ui.uicommonweb.models.SecuritySettingsListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.gin.AssetProvider;
import org.ovirt.engine.ui.webadmin.section.main.presenter.MainSecuritySettingsPresenter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

public class MainSecuritySettingsView extends AbstractMainWithDetailsTableView<Object, SecuritySettingsListModel>
        implements MainSecuritySettingsPresenter.ViewDef {

    interface ViewUiBinder extends UiBinder<FlowPanel, MainSecuritySettingsView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<MainSecuritySettingsView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
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

    @Inject
    public MainSecuritySettingsView(MainModelProvider<Object, SecuritySettingsListModel> modelProvider) {
        super(modelProvider);

        // Hide the default table
        getTable().setVisible(false);

        // Create custom UI using UiBinder
        FlowPanel customPanel = ViewUiBinder.uiBinder.createAndBindUi(this);

        // Add the custom panel to the table's outer container
        FlowPanel tableContainer = getTable().getOuterWidget();
        tableContainer.add(customPanel);

        // Generate IDs
        ViewIdHandler.idHandler.generateAndSetIds(this);

        // Initialize button handlers
        initializeHandlers();

        // Initialize widget
        initWidget(getTable());
    }

    private void initializeHandlers() {
        securityAuditButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (getModelProvider().getModel().getSecurityAuditCommand() != null) {
                    securityAuditStatusLabel.setText(constants.statusRunning());
                    securityAuditStatusLabel.removeStyleName("text-success"); //$NON-NLS-1$
                    securityAuditStatusLabel.addStyleName("text-warning"); //$NON-NLS-1$
                    getModelProvider().getModel().getSecurityAuditCommand().execute();
                }
            }
        });

        integrityVerificationButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (getModelProvider().getModel().getIntegrityVerificationCommand() != null) {
                    integrityVerificationStatusLabel.setText(constants.statusRunning());
                    integrityVerificationStatusLabel.removeStyleName("text-success"); //$NON-NLS-1$
                    integrityVerificationStatusLabel.addStyleName("text-warning"); //$NON-NLS-1$
                    getModelProvider().getModel().getIntegrityVerificationCommand().execute();
                }
            }
        });
    }
}
