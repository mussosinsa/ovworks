package org.ovirt.engine.ui.webadmin.section.main.view;

import org.ovirt.engine.ui.common.uicommon.model.MainModelProvider;
import org.ovirt.engine.ui.uicommonweb.models.AuditLogListModel;
import org.ovirt.engine.ui.webadmin.section.main.presenter.MainAuditLogPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.security.AuditLogManagementView;

import com.google.gwt.user.client.ui.SimplePanel;
import com.google.inject.Inject;

public class MainAuditLogView extends AbstractMainWithDetailsTableView<Object, AuditLogListModel>
        implements MainAuditLogPresenter.ViewDef {

    private final AuditLogManagementView auditLogManagementView;

    @Inject
    public MainAuditLogView(MainModelProvider<Object, AuditLogListModel> modelProvider,
            AuditLogManagementView auditLogManagementView) {
        super(modelProvider);

        this.auditLogManagementView = auditLogManagementView;

        // Hide the default table
        getTable().setVisible(false);

        // Create content panel
        SimplePanel contentPanel = new SimplePanel();
        contentPanel.setWidget(auditLogManagementView);

        // Add content panel to table container
        getTable().getOuterWidget().add(contentPanel);

        initWidget(getTable());
    }
}
