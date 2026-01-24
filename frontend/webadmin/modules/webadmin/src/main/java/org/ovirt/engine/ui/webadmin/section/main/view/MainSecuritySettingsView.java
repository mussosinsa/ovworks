package org.ovirt.engine.ui.webadmin.section.main.view;

import org.gwtbootstrap3.client.ui.TabContent;
import org.gwtbootstrap3.client.ui.TabListItem;
import org.gwtbootstrap3.client.ui.TabPane;
import org.gwtbootstrap3.client.ui.TabPanel;
import org.ovirt.engine.ui.common.uicommon.model.MainModelProvider;
import org.ovirt.engine.ui.uicommonweb.models.SecuritySettingsListModel;
import org.ovirt.engine.ui.webadmin.section.main.presenter.MainSecuritySettingsPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.security.AuditLogManagementView;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.security.ClientManagementView;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.security.IntegrityCheckView;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.inject.Inject;

public class MainSecuritySettingsView extends AbstractMainWithDetailsTableView<Object, SecuritySettingsListModel>
        implements MainSecuritySettingsPresenter.ViewDef {

    @Inject
    public MainSecuritySettingsView(MainModelProvider<Object, SecuritySettingsListModel> modelProvider,
            IntegrityCheckView integrityCheckView,
            ClientManagementView clientManagementView,
            AuditLogManagementView auditLogManagementView) {
        super(modelProvider);

        // Hide the default table
        getTable().setVisible(false);

        // Create tab interface
        FlowPanel mainPanel = new FlowPanel();
        TabPanel tabPanel = new TabPanel();

        // Create tab list items
        TabListItem integrityCheckTab = new TabListItem("무결성 검사"); //$NON-NLS-1$
        TabListItem clientManagementTab = new TabListItem("클라이언트 관리"); //$NON-NLS-1$
        TabListItem auditLogManagementTab = new TabListItem("감사기록 관리"); //$NON-NLS-1$

        // Create tab panes
        TabPane integrityCheckPane = new TabPane();
        integrityCheckPane.add(integrityCheckView);
        integrityCheckPane.setActive(true);

        TabPane clientManagementPane = new TabPane();
        clientManagementPane.add(clientManagementView);

        TabPane auditLogManagementPane = new TabPane();
        auditLogManagementPane.add(auditLogManagementView);

        // Link tabs to panes
        integrityCheckTab.setDataTarget("#integrityCheckPane"); //$NON-NLS-1$
        clientManagementTab.setDataTarget("#clientManagementPane"); //$NON-NLS-1$
        auditLogManagementTab.setDataTarget("#auditLogManagementPane"); //$NON-NLS-1$
        integrityCheckTab.setActive(true);

        integrityCheckPane.setId("integrityCheckPane"); //$NON-NLS-1$
        clientManagementPane.setId("clientManagementPane"); //$NON-NLS-1$
        auditLogManagementPane.setId("auditLogManagementPane"); //$NON-NLS-1$

        // Add tabs to panel
        tabPanel.add(integrityCheckTab);
        tabPanel.add(clientManagementTab);
        tabPanel.add(auditLogManagementTab);

        TabContent tabContent = new TabContent();
        tabContent.add(integrityCheckPane);
        tabContent.add(clientManagementPane);
        tabContent.add(auditLogManagementPane);

        mainPanel.add(tabPanel);
        mainPanel.add(tabContent);

        // Add to table container
        FlowPanel tableContainer = getTable().getOuterWidget();
        tableContainer.add(mainPanel);

        initWidget(getTable());
    }
}
