package org.ovirt.engine.ui.webadmin.section.main.view;

import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.uicommon.model.MainModelProvider;
import org.ovirt.engine.ui.uicommonweb.models.SecuritySettingsListModel;
import org.ovirt.engine.ui.webadmin.section.main.presenter.MainSecuritySettingsPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.security.AuditLogManagementView;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.security.ClientManagementView;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.security.IntegrityCheckView;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class MainSecuritySettingsView extends AbstractMainWithDetailsTableView<Object, SecuritySettingsListModel>
        implements MainSecuritySettingsPresenter.ViewDef {

    interface ViewUiBinder extends UiBinder<Widget, MainSecuritySettingsView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<MainSecuritySettingsView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @UiField
    Element integrityCheckMenuItem;

    @UiField
    Element clientManagementMenuItem;

    @UiField
    Element auditLogManagementMenuItem;

    @UiField
    SimplePanel contentPanel;

    private final IntegrityCheckView integrityCheckView;
    private final ClientManagementView clientManagementView;
    private final AuditLogManagementView auditLogManagementView;

    private Element currentActiveMenuItem;

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

        // Create UI using UiBinder
        Widget mainWidget = ViewUiBinder.uiBinder.createAndBindUi(this);

        // Add to table container
        getTable().getOuterWidget().add(mainWidget);

        // Initialize handlers
        initializeHandlers();

        // Show first tab by default
        showIntegrityCheck();

        // Generate IDs
        ViewIdHandler.idHandler.generateAndSetIds(this);

        initWidget(getTable());
    }

    private void initializeHandlers() {
        addClickHandler(integrityCheckMenuItem, new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                showIntegrityCheck();
            }
        });

        addClickHandler(clientManagementMenuItem, new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                showClientManagement();
            }
        });

        addClickHandler(auditLogManagementMenuItem, new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                showAuditLogManagement();
            }
        });
    }

    private void addClickHandler(Element element, ClickHandler handler) {
        com.google.gwt.user.client.Event.sinkEvents(element, com.google.gwt.user.client.Event.ONCLICK);
        com.google.gwt.user.client.Event.setEventListener(element, new com.google.gwt.user.client.EventListener() {
            @Override
            public void onBrowserEvent(com.google.gwt.user.client.Event event) {
                if (com.google.gwt.user.client.Event.ONCLICK == event.getTypeInt()) {
                    handler.onClick(null);
                }
            }
        });
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

    private void setActiveMenuItem(Element menuItem) {
        // Remove active class from current active item
        if (currentActiveMenuItem != null) {
            currentActiveMenuItem.removeClassName("menuItemActive"); //$NON-NLS-1$
        }

        // Add active class to new active item
        menuItem.addClassName("menuItemActive"); //$NON-NLS-1$
        currentActiveMenuItem = menuItem;
    }
}
