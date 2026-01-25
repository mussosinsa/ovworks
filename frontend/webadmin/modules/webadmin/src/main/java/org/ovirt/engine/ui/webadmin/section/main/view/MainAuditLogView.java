package org.ovirt.engine.ui.webadmin.section.main.view;

import org.ovirt.engine.ui.common.uicommon.model.MainModelProvider;
import org.ovirt.engine.ui.uicommonweb.models.AuditLogListModel;
import org.ovirt.engine.ui.webadmin.section.main.presenter.MainAuditLogPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.security.AuditLogTabsView;

import com.google.gwt.user.client.ui.SimplePanel;
import com.google.inject.Inject;

public class MainAuditLogView extends AbstractMainWithDetailsTableView<Object, AuditLogListModel>
        implements MainAuditLogPresenter.ViewDef {

    private final AuditLogTabsView auditLogTabsView;

    @Inject
    public MainAuditLogView(MainModelProvider<Object, AuditLogListModel> modelProvider,
            AuditLogTabsView auditLogTabsView) {
        super(modelProvider);

        this.auditLogTabsView = auditLogTabsView;

        // Hide the default table
        getTable().setVisible(false);

        // Create content panel
        SimplePanel contentPanel = new SimplePanel();
        contentPanel.setWidget(auditLogTabsView);

        // Add content panel to table container
        getTable().getOuterWidget().add(contentPanel);

        initWidget(getTable());
    }
}
