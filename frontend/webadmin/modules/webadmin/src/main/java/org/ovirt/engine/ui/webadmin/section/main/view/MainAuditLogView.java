package org.ovirt.engine.ui.webadmin.section.main.view;

import org.ovirt.engine.ui.common.presenter.ActionPanelPresenterWidget;
import org.ovirt.engine.ui.common.presenter.PlaceTransitionHandler;
import org.ovirt.engine.ui.common.uicommon.model.MainModelProvider;
import org.ovirt.engine.ui.common.view.AbstractView;
import org.ovirt.engine.ui.uicommonweb.models.AuditLogListModel;
import org.ovirt.engine.ui.webadmin.section.main.presenter.AbstractMainWithDetailsPresenter;
import org.ovirt.engine.ui.webadmin.section.main.presenter.MainAuditLogPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.security.AuditLogProtectionTabView;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.security.AuditLogRemoteBackupTabView;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.security.AvailabilityTabView;

import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.inject.Inject;

public class MainAuditLogView extends AbstractView
        implements MainAuditLogPresenter.ViewDef {

    private final AuditLogProtectionTabView auditLogProtectionTabView;
    private final AuditLogRemoteBackupTabView auditLogRemoteBackupTabView;
    private final AvailabilityTabView availabilityTabView;
    private final MainModelProvider<Object, AuditLogListModel> modelProvider;

    private SimplePanel contentPanel;
    private HTML auditLogProtectionMenuItem;
    private HTML auditLogRemoteBackupMenuItem;
    private HTML availabilityMenuItem;
    private HTML currentActiveMenuItem;
    private PlaceTransitionHandler placeTransitionHandler;
    private FlowPanel mainContainer;

    @Inject
    public MainAuditLogView(MainModelProvider<Object, AuditLogListModel> modelProvider,
            AuditLogProtectionTabView auditLogProtectionTabView,
            AuditLogRemoteBackupTabView auditLogRemoteBackupTabView,
            AvailabilityTabView availabilityTabView) {
        this.modelProvider = modelProvider;
        this.auditLogProtectionTabView = auditLogProtectionTabView;
        this.auditLogRemoteBackupTabView = auditLogRemoteBackupTabView;
        this.availabilityTabView = availabilityTabView;

        // Create main container with flexbox layout
        mainContainer = new FlowPanel();
        mainContainer.getElement().getStyle().setProperty("display", "flex"); //$NON-NLS-1$ //$NON-NLS-2$
        mainContainer.getElement().getStyle().setProperty("width", "100%"); //$NON-NLS-1$ //$NON-NLS-2$
        mainContainer.getElement().getStyle().setProperty("minHeight", "600px"); //$NON-NLS-1$ //$NON-NLS-2$

        // Create sidebar
        FlowPanel sidebar = new FlowPanel();
        sidebar.getElement().getStyle().setProperty("width", "250px"); //$NON-NLS-1$ //$NON-NLS-2$
        sidebar.getElement().getStyle().setProperty("backgroundColor", "#ffffff"); //$NON-NLS-1$ //$NON-NLS-2$
        sidebar.getElement().getStyle().setProperty("borderRight", "1px solid #ddd"); //$NON-NLS-1$ //$NON-NLS-2$
        sidebar.getElement().getStyle().setProperty("flexShrink", "0"); //$NON-NLS-1$ //$NON-NLS-2$
        sidebar.getElement().getStyle().setProperty("overflowY", "auto"); //$NON-NLS-1$ //$NON-NLS-2$

        // Create sidebar header
        HTML sidebarHeader = new HTML("관리 - 감사기록 관리"); //$NON-NLS-1$
        sidebarHeader.getElement().getStyle().setProperty("padding", "10px 15px"); //$NON-NLS-1$ //$NON-NLS-2$
        sidebarHeader.getElement().getStyle().setProperty("fontSize", "16px"); //$NON-NLS-1$ //$NON-NLS-2$
        sidebarHeader.getElement().getStyle().setProperty("fontWeight", "bold"); //$NON-NLS-1$ //$NON-NLS-2$
        sidebarHeader.getElement().getStyle().setProperty("borderBottom", "1px solid #ddd"); //$NON-NLS-1$ //$NON-NLS-2$
        sidebarHeader.getElement().getStyle().setProperty("backgroundColor", "#f8f8f8"); //$NON-NLS-1$ //$NON-NLS-2$
        sidebar.add(sidebarHeader);

        // Create menu items
        auditLogProtectionMenuItem = new HTML("감사기록 보호"); //$NON-NLS-1$
        auditLogProtectionMenuItem.setStyleName("security-menu-item security-menu-item-active"); //$NON-NLS-1$
        auditLogProtectionMenuItem.getElement().getStyle().setProperty("display", "block"); //$NON-NLS-1$ //$NON-NLS-2$
        auditLogProtectionMenuItem.getElement().getStyle().setProperty("padding", "10px 15px"); //$NON-NLS-1$ //$NON-NLS-2$
        auditLogProtectionMenuItem.getElement().getStyle().setProperty("cursor", "pointer"); //$NON-NLS-1$ //$NON-NLS-2$
        auditLogProtectionMenuItem.getElement().getStyle().setProperty("borderBottom", "1px solid #f0f0f0"); //$NON-NLS-1$ //$NON-NLS-2$
        auditLogProtectionMenuItem.getElement().getStyle().setProperty("backgroundColor", "#337ab7"); //$NON-NLS-1$ //$NON-NLS-2$
        auditLogProtectionMenuItem.getElement().getStyle().setProperty("color", "white"); //$NON-NLS-1$ //$NON-NLS-2$
        auditLogProtectionMenuItem.getElement().getStyle().setProperty("fontWeight", "bold"); //$NON-NLS-1$ //$NON-NLS-2$
        sidebar.add(auditLogProtectionMenuItem);

        auditLogRemoteBackupMenuItem = new HTML("감사기록 원격 백업"); //$NON-NLS-1$
        auditLogRemoteBackupMenuItem.setStyleName("security-menu-item"); //$NON-NLS-1$
        auditLogRemoteBackupMenuItem.getElement().getStyle().setProperty("display", "block"); //$NON-NLS-1$ //$NON-NLS-2$
        auditLogRemoteBackupMenuItem.getElement().getStyle().setProperty("padding", "10px 15px"); //$NON-NLS-1$ //$NON-NLS-2$
        auditLogRemoteBackupMenuItem.getElement().getStyle().setProperty("cursor", "pointer"); //$NON-NLS-1$ //$NON-NLS-2$
        auditLogRemoteBackupMenuItem.getElement().getStyle().setProperty("borderBottom", "1px solid #f0f0f0"); //$NON-NLS-1$ //$NON-NLS-2$
        sidebar.add(auditLogRemoteBackupMenuItem);

        availabilityMenuItem = new HTML("가용성 확보"); //$NON-NLS-1$
        availabilityMenuItem.setStyleName("security-menu-item"); //$NON-NLS-1$
        availabilityMenuItem.getElement().getStyle().setProperty("display", "block"); //$NON-NLS-1$ //$NON-NLS-2$
        availabilityMenuItem.getElement().getStyle().setProperty("padding", "10px 15px"); //$NON-NLS-1$ //$NON-NLS-2$
        availabilityMenuItem.getElement().getStyle().setProperty("cursor", "pointer"); //$NON-NLS-1$ //$NON-NLS-2$
        availabilityMenuItem.getElement().getStyle().setProperty("borderBottom", "1px solid #f0f0f0"); //$NON-NLS-1$ //$NON-NLS-2$
        sidebar.add(availabilityMenuItem);

        // Add sidebar to main container
        mainContainer.add(sidebar);

        // Create content panel
        contentPanel = new SimplePanel();
        contentPanel.getElement().getStyle().setProperty("flex", "1"); //$NON-NLS-1$ //$NON-NLS-2$
        contentPanel.getElement().getStyle().setProperty("backgroundColor", "#ffffff"); //$NON-NLS-1$ //$NON-NLS-2$
        contentPanel.getElement().getStyle().setProperty("overflowY", "auto"); //$NON-NLS-1$ //$NON-NLS-2$

        // Add content panel to main container
        mainContainer.add(contentPanel);

        // Initialize handlers
        initializeHandlers();

        // Show first tab by default
        showAuditLogProtection();

        initWidget(mainContainer);
    }

    private void initializeHandlers() {
        auditLogProtectionMenuItem.addClickHandler(event -> showAuditLogProtection());
        auditLogRemoteBackupMenuItem.addClickHandler(event -> showAuditLogRemoteBackup());
        availabilityMenuItem.addClickHandler(event -> showAvailability());
    }

    private void showAuditLogProtection() {
        setActiveMenuItem(auditLogProtectionMenuItem);
        contentPanel.setWidget(auditLogProtectionTabView);
    }

    private void showAuditLogRemoteBackup() {
        setActiveMenuItem(auditLogRemoteBackupMenuItem);
        contentPanel.setWidget(auditLogRemoteBackupTabView);
    }

    private void showAvailability() {
        setActiveMenuItem(availabilityMenuItem);
        contentPanel.setWidget(availabilityTabView);
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

    @Override
    public HandlerRegistration addWindowResizeHandler(ResizeHandler handler) {
        return Window.addResizeHandler(handler);
    }

    @Override
    public void resizeToFullHeight() {
        // Not needed for this custom view
    }

    @Override
    public void setInSlot(Object slot, IsWidget content) {
        if (slot == AbstractMainWithDetailsPresenter.TYPE_SetBreadCrumbs) {
            // Bread crumbs can be added here if needed
        } else if (slot == AbstractMainWithDetailsPresenter.TYPE_SetSearchPanel) {
            // Search panel can be added here if needed
        } else if (slot == AbstractMainWithDetailsPresenter.TYPE_SetActionPanel) {
            // Action panel can be added here if needed
        }
    }

    @Override
    public IsWidget getTableContainer() {
        return mainContainer;
    }

    @Override
    public void setPlaceTransitionHandler(PlaceTransitionHandler handler) {
        this.placeTransitionHandler = handler;
    }

    @Override
    public org.ovirt.engine.ui.common.widget.table.ActionTable<Object> getTable() {
        // This view doesn't use a table, return null
        return null;
    }
}
