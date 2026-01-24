package org.ovirt.engine.ui.webadmin.section.main.view;

import org.ovirt.engine.ui.common.uicommon.model.MainModelProvider;
import org.ovirt.engine.ui.uicommonweb.models.SecuritySettingsListModel;
import org.ovirt.engine.ui.webadmin.section.main.presenter.MainSecuritySettingsPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.security.AuditLogManagementView;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.security.ClientManagementView;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.security.IntegrityCheckView;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
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

        // Create main container with layout structure
        FlowPanel mainContainer = new FlowPanel();
        mainContainer.getElement().setInnerHTML(
            "<div id='securitySettingsLayout' style='display: flex; width: 100%; min-height: 600px;'>" +
            "  <div id='sidebar' style='width: 250px; background-color: #ffffff; border-right: 1px solid #ddd;'>" +
            "    <div style='padding: 10px 15px; font-size: 16px; font-weight: bold; border-bottom: 1px solid #ddd; background-color: #f8f8f8;'>보안 설정</div>" +
            "    <div id='menuList'></div>" +
            "  </div>" +
            "  <div id='contentArea' style='flex: 1; background-color: #ffffff; overflow-y: auto;'></div>" +
            "</div>"
        );

        // Add main container to table
        getTable().getOuterWidget().add(mainContainer);

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

        // Find the menuList div and add menu items to it
        com.google.gwt.dom.client.NodeList<com.google.gwt.dom.client.Element> menuListElements =
            mainContainer.getElement().getElementsByTagName("div"); //$NON-NLS-1$
        Element menuListElement = null;
        Element contentAreaElement = null;

        for (int i = 0; i < menuListElements.getLength(); i++) {
            Element elem = menuListElements.getItem(i);
            if ("menuList".equals(elem.getId())) { //$NON-NLS-1$
                menuListElement = elem;
            } else if ("contentArea".equals(elem.getId())) { //$NON-NLS-1$
                contentAreaElement = elem;
            }
        }

        if (menuListElement != null) {
            menuListElement.appendChild(integrityCheckMenuItem.getElement());
            menuListElement.appendChild(clientManagementMenuItem.getElement());
            menuListElement.appendChild(auditLogManagementMenuItem.getElement());
        }

        // Create content panel and add to content area
        contentPanel = new SimplePanel();
        contentPanel.getElement().getStyle().setProperty("width", "100%"); //$NON-NLS-1$ //$NON-NLS-2$
        contentPanel.getElement().getStyle().setProperty("height", "100%"); //$NON-NLS-1$ //$NON-NLS-2$

        if (contentAreaElement != null) {
            contentAreaElement.appendChild(contentPanel.getElement());
        }

        // Initialize handlers
        initializeHandlers();

        // Show first tab by default
        showIntegrityCheck();

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
