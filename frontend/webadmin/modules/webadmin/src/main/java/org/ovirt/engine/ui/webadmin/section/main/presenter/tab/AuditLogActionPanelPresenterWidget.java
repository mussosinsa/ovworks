package org.ovirt.engine.ui.webadmin.section.main.presenter.tab;

import javax.inject.Inject;

import org.ovirt.engine.ui.common.presenter.ActionPanelPresenterWidget;
import org.ovirt.engine.ui.common.uicommon.model.MainModelProvider;
import org.ovirt.engine.ui.uicommonweb.models.AuditLogListModel;
import org.ovirt.engine.ui.webadmin.section.main.presenter.MainAuditLogPresenter;

import com.google.web.bindery.event.shared.EventBus;

public class AuditLogActionPanelPresenterWidget extends
        ActionPanelPresenterWidget<Object, Object, AuditLogListModel> {

    @Inject
    public AuditLogActionPanelPresenterWidget(EventBus eventBus,
            ActionPanelPresenterWidget.ViewDef<Object, Object> view,
            MainModelProvider<Object, AuditLogListModel> dataProvider) {
        super(eventBus, view, dataProvider);
    }

    @Override
    protected void initializeButtons() {
        // No action buttons needed for audit log management page
    }

    @Override
    public void onBind() {
        super.onBind();
        registerHandler(getEventBus().addHandler(MainAuditLogPresenter.AuditLogSelectionChangeEvent.getType(),
                event -> updateActionAvailability()));
    }

    private void updateActionAvailability() {
        // No actions to update
    }
}
