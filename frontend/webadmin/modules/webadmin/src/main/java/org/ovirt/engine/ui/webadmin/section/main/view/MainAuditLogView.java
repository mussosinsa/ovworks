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

        // Create main container with stacked header + body layout
        FlowPanel mainContainer = new FlowPanel();
        mainContainer.getElement().getStyle().setProperty("width", "100%"); //$NON-NLS-1$ //$NON-NLS-2$
        mainContainer.getElement().getStyle().setProperty("minHeight", "600px"); //$NON-NLS-1$ //$NON-NLS-2$

        FlowPanel header = new FlowPanel();
        header.getElement().getStyle().setProperty("display", "flex"); //$NON-NLS-1$ //$NON-NLS-2$
        header.getElement().getStyle().setProperty("justifyContent", "space-between"); //$NON-NLS-1$ //$NON-NLS-2$
        header.getElement().getStyle().setProperty("alignItems", "center"); //$NON-NLS-1$ //$NON-NLS-2$
        header.getElement().getStyle().setProperty("padding", "10px 15px"); //$NON-NLS-1$ //$NON-NLS-2$
        header.getElement().getStyle().setProperty("borderBottom", "1px solid #ddd"); //$NON-NLS-1$ //$NON-NLS-2$
        header.getElement().getStyle().setProperty("backgroundColor", "#f8f8f8"); //$NON-NLS-1$ //$NON-NLS-2$

        HTML headerTitle = new HTML("관리 - 감사기록 관리"); //$NON-NLS-1$
        headerTitle.getElement().getStyle().setProperty("fontWeight", "bold"); //$NON-NLS-1$ //$NON-NLS-2$
        headerTitle.getElement().getStyle().setProperty("fontSize", "16px"); //$NON-NLS-1$ //$NON-NLS-2$
        header.add(headerTitle);

        HTML headerClose = new HTML("&times;"); //$NON-NLS-1$
        headerClose.getElement().getStyle().setProperty("color", "#777"); //$NON-NLS-1$ //$NON-NLS-2$
        headerClose.getElement().getStyle().setProperty("fontSize", "18px"); //$NON-NLS-1$ //$NON-NLS-2$
        headerClose.getElement().getStyle().setProperty("cursor", "default"); //$NON-NLS-1$ //$NON-NLS-2$
        header.add(headerClose);

        FlowPanel body = new FlowPanel();
        body.getElement().getStyle().setProperty("display", "flex"); //$NON-NLS-1$ //$NON-NLS-2$
        body.getElement().getStyle().setProperty("width", "100%"); //$NON-NLS-1$ //$NON-NLS-2$

        FlowPanel sidebar = new FlowPanel();
        sidebar.getElement().getStyle().setProperty("width", "200px"); //$NON-NLS-1$ //$NON-NLS-2$
        sidebar.getElement().getStyle().setProperty("backgroundColor", "#f5f5f5"); //$NON-NLS-1$ //$NON-NLS-2$
        sidebar.getElement().getStyle().setProperty("borderRight", "1px solid #ddd"); //$NON-NLS-1$ //$NON-NLS-2$
        sidebar.getElement().getStyle().setProperty("flexShrink", "0"); //$NON-NLS-1$ //$NON-NLS-2$

        sidebar.add(buildSidebarItem("감사기록 보호", true)); //$NON-NLS-1$
        sidebar.add(buildSidebarItem("감사기록 원격 백업", false)); //$NON-NLS-1$
        sidebar.add(buildSidebarItem("가용성 확보", false)); //$NON-NLS-1$

        body.add(sidebar);

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

        body.add(contentPanel);

        mainContainer.add(header);
        mainContainer.add(body);

        getTable().getOuterWidget().add(mainContainer);
        mainContainer.setVisible(true);

        initWidget(getTable());
    }

    private FlowPanel buildSidebarItem(String label, boolean isActive) {
        FlowPanel item = new FlowPanel();
        item.getElement().getStyle().setProperty("display", "flex"); //$NON-NLS-1$ //$NON-NLS-2$
        item.getElement().getStyle().setProperty("alignItems", "center"); //$NON-NLS-1$ //$NON-NLS-2$
        item.getElement().getStyle().setProperty("justifyContent", "space-between"); //$NON-NLS-1$ //$NON-NLS-2$
        item.getElement().getStyle().setProperty("padding", "10px 12px"); //$NON-NLS-1$ //$NON-NLS-2$
        item.getElement().getStyle().setProperty("borderBottom", "1px solid #e6e6e6"); //$NON-NLS-1$ //$NON-NLS-2$
        if (isActive) {
            item.getElement().getStyle().setProperty("backgroundColor", "#ededed"); //$NON-NLS-1$ //$NON-NLS-2$
            item.getElement().getStyle().setProperty("borderLeft", "3px solid #2b83c9"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        HTML text = new HTML(label);
        item.add(text);

        HTML chevron = new HTML("&rsaquo;"); //$NON-NLS-1$
        chevron.getElement().getStyle().setProperty("color", "#2b83c9"); //$NON-NLS-1$ //$NON-NLS-2$
        if (!isActive) {
            chevron.getElement().getStyle().setProperty("visibility", "hidden"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        item.add(chevron);
        return item;
    }
}
