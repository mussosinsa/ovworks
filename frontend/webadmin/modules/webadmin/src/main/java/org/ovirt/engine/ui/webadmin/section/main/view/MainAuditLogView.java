package org.ovirt.engine.ui.webadmin.section.main.view;

import org.ovirt.engine.ui.common.uicommon.model.MainModelProvider;
import org.ovirt.engine.ui.common.widget.table.SimpleActionTable;
import org.ovirt.engine.ui.common.widget.table.column.AbstractTextColumn;
import org.ovirt.engine.ui.uicommonweb.models.AuditLogListModel;
import org.ovirt.engine.ui.webadmin.gin.ClientGinjectorProvider;
import org.ovirt.engine.ui.webadmin.section.main.presenter.MainAuditLogPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.security.AuditLogProtectionTabView;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.security.AuditLogRemoteBackupTabView;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.security.AvailabilityTabView;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.inject.Inject;

public class MainAuditLogView extends AbstractMainWithDetailsTableView<Object, AuditLogListModel>
        implements MainAuditLogPresenter.ViewDef {

    private final AuditLogProtectionTabView auditLogProtectionTabView;
    private final AuditLogRemoteBackupTabView auditLogRemoteBackupTabView;
    private final AvailabilityTabView availabilityTabView;

    private SimplePanel contentPanel;
    private FlowPanel sidebar;

    @Override
    protected SimpleActionTable<Void, Object> createActionTable() {
        SimpleActionTable<Void, Object> table = new SimpleActionTable<Void, Object>(getModelProvider(),
                getTableResources(), ClientGinjectorProvider.getEventBus(),
                ClientGinjectorProvider.getClientStorage()) {
            {
                showRefreshButton();
                showItemsCount();
                showSelectionCountTooltip();
                enableHeaderContextMenu();
            }
        };

        // Add a dummy column to prevent rendering errors when table has data but no columns
        table.addColumn(new AbstractTextColumn<Object>() {
            @Override
            public String getValue(Object object) {
                return ""; //$NON-NLS-1$
            }
        }, ""); //$NON-NLS-1$

        return table;
    }

    @Inject
    public MainAuditLogView(MainModelProvider<Object, AuditLogListModel> modelProvider,
            AuditLogProtectionTabView auditLogProtectionTabView,
            AuditLogRemoteBackupTabView auditLogRemoteBackupTabView,
            AvailabilityTabView availabilityTabView) {
        super(modelProvider);

        this.auditLogProtectionTabView = auditLogProtectionTabView;
        this.auditLogRemoteBackupTabView = auditLogRemoteBackupTabView;
        this.availabilityTabView = availabilityTabView;

        // Hide the default table
        getTable().setVisible(false);

        // Create main container with two-column layout
        FlowPanel mainContainer = new FlowPanel();
        mainContainer.getElement().getStyle().setProperty("display", "flex"); //$NON-NLS-1$ //$NON-NLS-2$
        mainContainer.getElement().getStyle().setProperty("width", "100%"); //$NON-NLS-1$ //$NON-NLS-2$
        mainContainer.getElement().getStyle().setProperty("minHeight", "600px"); //$NON-NLS-1$ //$NON-NLS-2$
        mainContainer.getElement().getStyle().setProperty("backgroundColor", "#ffffff"); //$NON-NLS-1$ //$NON-NLS-2$

        // Create static sidebar matching the security settings menu
        sidebar = new FlowPanel();
        sidebar.getElement().getStyle().setProperty("width", "250px"); //$NON-NLS-1$ //$NON-NLS-2$
        sidebar.getElement().getStyle().setProperty("backgroundColor", "#ffffff"); //$NON-NLS-1$ //$NON-NLS-2$
        sidebar.getElement().getStyle().setProperty("borderRight", "1px solid #ddd"); //$NON-NLS-1$ //$NON-NLS-2$
        sidebar.getElement().getStyle().setProperty("flexShrink", "0"); //$NON-NLS-1$ //$NON-NLS-2$
        sidebar.getElement().getStyle().setProperty("overflowY", "auto"); //$NON-NLS-1$ //$NON-NLS-2$

        HTML sidebarHeader = new HTML("보안 설정"); //$NON-NLS-1$
        sidebarHeader.getElement().getStyle().setProperty("padding", "10px 15px"); //$NON-NLS-1$ //$NON-NLS-2$
        sidebarHeader.getElement().getStyle().setProperty("fontSize", "16px"); //$NON-NLS-1$ //$NON-NLS-2$
        sidebarHeader.getElement().getStyle().setProperty("fontWeight", "bold"); //$NON-NLS-1$ //$NON-NLS-2$
        sidebarHeader.getElement().getStyle().setProperty("borderBottom", "1px solid #ddd"); //$NON-NLS-1$ //$NON-NLS-2$
        sidebarHeader.getElement().getStyle().setProperty("backgroundColor", "#f8f8f8"); //$NON-NLS-1$ //$NON-NLS-2$
        sidebar.add(sidebarHeader);

        sidebar.add(buildSidebarItem("무결성 검사", "#337ab7", false)); //$NON-NLS-1$ //$NON-NLS-2$
        sidebar.add(buildSidebarItem("클라이언트 관리", "#333333", false)); //$NON-NLS-1$ //$NON-NLS-2$
        sidebar.add(buildSidebarItem("감사기록 관리", "#d9534f", true)); //$NON-NLS-1$ //$NON-NLS-2$

        mainContainer.add(sidebar);

        // Create content panel that stacks all sections
        contentPanel = new SimplePanel();
        contentPanel.getElement().getStyle().setProperty("flex", "1"); //$NON-NLS-1$ //$NON-NLS-2$
        contentPanel.getElement().getStyle().setProperty("backgroundColor", "#ffffff"); //$NON-NLS-1$ //$NON-NLS-2$
        contentPanel.getElement().getStyle().setProperty("overflowY", "auto"); //$NON-NLS-1$ //$NON-NLS-2$

        FlowPanel stackedSections = new FlowPanel();
        stackedSections.getElement().getStyle().setProperty("padding", "20px"); //$NON-NLS-1$ //$NON-NLS-2$
        stackedSections.add(auditLogProtectionTabView);
        stackedSections.add(auditLogRemoteBackupTabView);
        stackedSections.add(availabilityTabView);

        contentPanel.setWidget(stackedSections);

        mainContainer.add(contentPanel);

        // Add main container to table
        getTable().getOuterWidget().add(mainContainer);

        // Make sure mainContainer is visible even though table is hidden
        mainContainer.setVisible(true);

        initWidget(getTable());
    }

    private HTML buildSidebarItem(String label, String color, boolean isActive) {
        HTML item = new HTML(label);
        item.getElement().getStyle().setProperty("display", "block"); //$NON-NLS-1$ //$NON-NLS-2$
        item.getElement().getStyle().setProperty("padding", "10px 15px"); //$NON-NLS-1$ //$NON-NLS-2$
        item.getElement().getStyle().setProperty("borderBottom", "1px solid #f0f0f0"); //$NON-NLS-1$ //$NON-NLS-2$
        item.getElement().getStyle().setProperty("cursor", "default"); //$NON-NLS-1$ //$NON-NLS-2$
        item.getElement().getStyle().setProperty("color", color); //$NON-NLS-1$ //$NON-NLS-2$
        if (isActive) {
            item.getElement().getStyle().setProperty("fontWeight", "bold"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return item;
    }
}
