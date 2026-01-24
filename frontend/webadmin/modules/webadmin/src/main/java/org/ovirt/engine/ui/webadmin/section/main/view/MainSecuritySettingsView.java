package org.ovirt.engine.ui.webadmin.section.main.view;

import org.ovirt.engine.ui.common.uicommon.model.MainModelProvider;
import org.ovirt.engine.ui.uicommonweb.models.SecuritySettingsListModel;
import org.ovirt.engine.ui.webadmin.section.main.presenter.MainSecuritySettingsPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.security.AuditLogManagementView;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.security.ClientManagementView;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.security.IntegrityCheckView;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.inject.Inject;

public class MainSecuritySettingsView extends AbstractMainWithDetailsTableView<Object, SecuritySettingsListModel>
        implements MainSecuritySettingsPresenter.ViewDef {

    private final IntegrityCheckView integrityCheckView;
    private final ClientManagementView clientManagementView;
    private final AuditLogManagementView auditLogManagementView;

    private SimplePanel contentPanel;
    private HTML integrityCheckMenuItem;
    private HTML clientManagementMenuItem;
    private HTML auditLogManagementMenuItem;
    private HTML currentActiveMenuItem;

    @Inject
    public MainSecuritySettingsView(MainModelProvider<Object, SecuritySettingsListModel> modelProvider,
            IntegrityCheckView integrityCheckView,
            ClientManagementView clientManagementView,
            AuditLogManagementView auditLogManagementView) {
        super(modelProvider);

        this.integrityCheckView = integrityCheckView;
        this.clientManagementView = clientManagementView;
        this.auditLogManagementView = auditLogManagementView;

        // Hide the default table
        getTable().setVisible(false);

        // Create main container
        FlowPanel mainContainer = new FlowPanel();
        mainContainer.getElement().setInnerHTML(
            "<div style='display: table; width: 100%; height: 100%;'>" + //$NON-NLS-1$
            "  <div id='sidebar' style='display: table-cell; width: 250px; height: 100%; vertical-align: top; background-color: #ffffff; border-right: 1px solid #ddd;'>" + //$NON-NLS-1$
            "    <div style='display: flex; flex-direction: column; height: 100%;'>" + //$NON-NLS-1$
            "      <div style='padding: 10px 15px; font-size: 16px; font-weight: bold; border-bottom: 1px solid #ddd; background-color: #f8f8f8; flex-shrink: 0;'>보안 설정</div>" + //$NON-NLS-1$
            "      <div id='menuList' style='flex: 1; overflow-y: auto;'></div>" + //$NON-NLS-1$
            "    </div>" + //$NON-NLS-1$
            "  </div>" + //$NON-NLS-1$
            "  <div id='contentArea' style='display: table-cell; height: 100%; vertical-align: top; background-color: #ffffff;'></div>" + //$NON-NLS-1$
            "</div>" //$NON-NLS-1$
        );

        // Create menu items
        integrityCheckMenuItem = new HTML("무결성 검사"); //$NON-NLS-1$
        integrityCheckMenuItem.setStyleName("security-menu-item security-menu-item-active"); //$NON-NLS-1$
        integrityCheckMenuItem.getElement().getStyle().setProperty("display", "block"); //$NON-NLS-1$ //$NON-NLS-2$
        integrityCheckMenuItem.getElement().getStyle().setProperty("padding", "10px 15px"); //$NON-NLS-1$ //$NON-NLS-2$
        integrityCheckMenuItem.getElement().getStyle().setProperty("cursor", "pointer"); //$NON-NLS-1$ //$NON-NLS-2$
        integrityCheckMenuItem.getElement().getStyle().setProperty("borderBottom", "1px solid #f0f0f0"); //$NON-NLS-1$ //$NON-NLS-2$
        integrityCheckMenuItem.getElement().getStyle().setProperty("backgroundColor", "#337ab7"); //$NON-NLS-1$ //$NON-NLS-2$
        integrityCheckMenuItem.getElement().getStyle().setProperty("color", "white"); //$NON-NLS-1$ //$NON-NLS-2$
        integrityCheckMenuItem.getElement().getStyle().setProperty("fontWeight", "bold"); //$NON-NLS-1$ //$NON-NLS-2$

        clientManagementMenuItem = new HTML("클라이언트 관리"); //$NON-NLS-1$
        clientManagementMenuItem.setStyleName("security-menu-item"); //$NON-NLS-1$
        clientManagementMenuItem.getElement().getStyle().setProperty("display", "block"); //$NON-NLS-1$ //$NON-NLS-2$
        clientManagementMenuItem.getElement().getStyle().setProperty("padding", "10px 15px"); //$NON-NLS-1$ //$NON-NLS-2$
        clientManagementMenuItem.getElement().getStyle().setProperty("cursor", "pointer"); //$NON-NLS-1$ //$NON-NLS-2$
        clientManagementMenuItem.getElement().getStyle().setProperty("borderBottom", "1px solid #f0f0f0"); //$NON-NLS-1$ //$NON-NLS-2$

        auditLogManagementMenuItem = new HTML("감사기록 관리"); //$NON-NLS-1$
        auditLogManagementMenuItem.setStyleName("security-menu-item"); //$NON-NLS-1$
        auditLogManagementMenuItem.getElement().getStyle().setProperty("display", "block"); //$NON-NLS-1$ //$NON-NLS-2$
        auditLogManagementMenuItem.getElement().getStyle().setProperty("padding", "10px 15px"); //$NON-NLS-1$ //$NON-NLS-2$
        auditLogManagementMenuItem.getElement().getStyle().setProperty("cursor", "pointer"); //$NON-NLS-1$ //$NON-NLS-2$
        auditLogManagementMenuItem.getElement().getStyle().setProperty("borderBottom", "1px solid #f0f0f0"); //$NON-NLS-1$ //$NON-NLS-2$

        // Create content panel
        contentPanel = new SimplePanel();
        contentPanel.getElement().setId("contentPanel"); //$NON-NLS-1$

        // Add to table container
        getTable().getOuterWidget().add(mainContainer);

        // Use Scheduler to ensure DOM is ready before adding children
        com.google.gwt.core.client.Scheduler.get().scheduleDeferred(() -> {
            // Add menu items directly to sidebar's menuList div
            com.google.gwt.dom.client.Element menuListElement =
                com.google.gwt.dom.client.Document.get().getElementById("menuList"); //$NON-NLS-1$
            if (menuListElement != null) {
                menuListElement.appendChild(integrityCheckMenuItem.getElement());
                menuListElement.appendChild(clientManagementMenuItem.getElement());
                menuListElement.appendChild(auditLogManagementMenuItem.getElement());
            }

            // Add content panel to contentArea div
            com.google.gwt.dom.client.Element contentAreaElement =
                com.google.gwt.dom.client.Document.get().getElementById("contentArea"); //$NON-NLS-1$
            if (contentAreaElement != null) {
                contentAreaElement.appendChild(contentPanel.getElement());
            }

            // Initialize handlers
            initializeHandlers();

            // Show first tab by default
            showIntegrityCheck();
        });

        initWidget(getTable());
    }

    private void initializeHandlers() {
        integrityCheckMenuItem.addClickHandler(event -> showIntegrityCheck());
        clientManagementMenuItem.addClickHandler(event -> showClientManagement());
        auditLogManagementMenuItem.addClickHandler(event -> showAuditLogManagement());
    }

    private void showIntegrityCheck() {
        setActiveMenuItem(integrityCheckMenuItem);
        contentPanel.setWidget(integrityCheckView);
    }

    private void showClientManagement() {
        setActiveMenuItem(clientManagementMenuItem);
        contentPanel.setWidget(clientManagementView);
    }

    private void showAuditLogManagement() {
        setActiveMenuItem(auditLogManagementMenuItem);
        contentPanel.setWidget(auditLogManagementView);
    }

    private void setActiveMenuItem(HTML menuItem) {
        // Remove active class from current active item
        if (currentActiveMenuItem != null) {
            currentActiveMenuItem.getElement().getStyle().clearBackgroundColor();
            currentActiveMenuItem.getElement().getStyle().clearColor();
            currentActiveMenuItem.getElement().getStyle().clearFontWeight();
        }

        // Add active class to new active item
        menuItem.getElement().getStyle().setProperty("backgroundColor", "#337ab7"); //$NON-NLS-1$ //$NON-NLS-2$
        menuItem.getElement().getStyle().setProperty("color", "white"); //$NON-NLS-1$ //$NON-NLS-2$
        menuItem.getElement().getStyle().setProperty("fontWeight", "bold"); //$NON-NLS-1$ //$NON-NLS-2$
        currentActiveMenuItem = menuItem;
    }
}
