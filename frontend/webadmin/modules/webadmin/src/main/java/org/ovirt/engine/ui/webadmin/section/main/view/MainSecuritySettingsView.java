package org.ovirt.engine.ui.webadmin.section.main.view;

import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.uicommon.model.MainModelProvider;
import org.ovirt.engine.ui.common.widget.table.column.AbstractTextColumn;
import org.ovirt.engine.ui.uicommonweb.models.SecuritySettingsListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.gin.AssetProvider;
import org.ovirt.engine.ui.webadmin.section.main.presenter.MainSecuritySettingsPresenter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

public class MainSecuritySettingsView extends AbstractMainWithDetailsTableView<Object, SecuritySettingsListModel>
        implements MainSecuritySettingsPresenter.ViewDef {

    interface ViewIdHandler extends ElementIdHandler<MainSecuritySettingsView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    private static final ApplicationConstants constants = AssetProvider.getConstants();

    private Button securityAuditButton;
    private Button integrityVerificationButton;
    private Label securityAuditStatusLabel;
    private Label integrityVerificationStatusLabel;

    @Inject
    public MainSecuritySettingsView(MainModelProvider<Object, SecuritySettingsListModel> modelProvider) {
        super(modelProvider);
        ViewIdHandler.idHandler.generateAndSetIds(this);
        initTable();
        initWidget(createMainPanel());
    }

    private FlowPanel createMainPanel() {
        FlowPanel mainPanel = new FlowPanel();
        mainPanel.setWidth("100%"); //$NON-NLS-1$

        // Add title
        HTML title = new HTML("<h2>" + constants.securitySettings() + "</h2>"); //$NON-NLS-1$ //$NON-NLS-2$
        mainPanel.add(title);

        // Create security audit section
        VerticalPanel securityAuditPanel = new VerticalPanel();
        securityAuditPanel.setSpacing(10);
        securityAuditPanel.getElement().getStyle().setProperty("marginTop", "20px"); //$NON-NLS-1$ //$NON-NLS-2$
        securityAuditPanel.getElement().getStyle().setProperty("marginBottom", "20px"); //$NON-NLS-1$ //$NON-NLS-2$

        HorizontalPanel auditButtonPanel = new HorizontalPanel();
        auditButtonPanel.setSpacing(10);

        securityAuditButton = new Button(constants.runSecurityAudit());
        securityAuditButton.setStyleName("btn btn-primary"); //$NON-NLS-1$
        securityAuditButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (getModelProvider().getModel().getSecurityAuditCommand() != null) {
                    getModelProvider().getModel().getSecurityAuditCommand().execute();
                }
            }
        });

        securityAuditStatusLabel = new Label(constants.statusNormal());
        securityAuditStatusLabel.setStyleName("label label-success"); //$NON-NLS-1$

        auditButtonPanel.add(securityAuditButton);
        auditButtonPanel.add(securityAuditStatusLabel);

        securityAuditPanel.add(new Label(constants.securityAuditStatus() + ":")); //$NON-NLS-1$
        securityAuditPanel.add(auditButtonPanel);

        // Create integrity verification section
        VerticalPanel integrityPanel = new VerticalPanel();
        integrityPanel.setSpacing(10);
        integrityPanel.getElement().getStyle().setProperty("marginTop", "20px"); //$NON-NLS-1$ //$NON-NLS-2$
        integrityPanel.getElement().getStyle().setProperty("marginBottom", "20px"); //$NON-NLS-1$ //$NON-NLS-2$

        HorizontalPanel integrityButtonPanel = new HorizontalPanel();
        integrityButtonPanel.setSpacing(10);

        integrityVerificationButton = new Button(constants.runIntegrityVerification());
        integrityVerificationButton.setStyleName("btn btn-primary"); //$NON-NLS-1$
        integrityVerificationButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (getModelProvider().getModel().getIntegrityVerificationCommand() != null) {
                    getModelProvider().getModel().getIntegrityVerificationCommand().execute();
                }
            }
        });

        integrityVerificationStatusLabel = new Label(constants.statusNormal());
        integrityVerificationStatusLabel.setStyleName("label label-success"); //$NON-NLS-1$

        integrityButtonPanel.add(integrityVerificationButton);
        integrityButtonPanel.add(integrityVerificationStatusLabel);

        integrityPanel.add(new Label(constants.integrityVerificationStatus() + ":")); //$NON-NLS-1$
        integrityPanel.add(integrityButtonPanel);

        // Add sections to main panel
        mainPanel.add(securityAuditPanel);
        mainPanel.add(integrityPanel);

        // Add table
        mainPanel.add(getTable());

        return mainPanel;
    }

    void initTable() {
        getTable().enableColumnResizing();

        AbstractTextColumn<Object> nameColumn =
                new AbstractTextColumn<Object>() {
                    @Override
                    public String getValue(Object object) {
                        return constants.securitySettingsMainViewLabel();
                    }
                };
        getTable().addColumn(nameColumn, constants.nameLabel(), "300px"); //$NON-NLS-1$

        AbstractTextColumn<Object> descriptionColumn =
                new AbstractTextColumn<Object>() {
                    @Override
                    public String getValue(Object object) {
                        return "Security settings management"; //$NON-NLS-1$
                    }
                };
        getTable().addColumn(descriptionColumn, constants.descriptionVm(), "400px"); //$NON-NLS-1$
    }
}
