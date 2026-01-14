package org.ovirt.engine.ui.webadmin.section.main.view.popup;

import org.ovirt.engine.ui.common.view.AbstractPopupView;
import org.ovirt.engine.ui.common.widget.dialog.PopupNativeKeyPressHandler;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogButton;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.gin.AssetProvider;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.SecuritySettingsPopupPresenterWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.inject.Inject;

public class SecuritySettingsPopupView extends AbstractPopupView<SimpleDialogPanel> implements SecuritySettingsPopupPresenterWidget.ViewDef {

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, SecuritySettingsPopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    private static final ApplicationConstants constants = AssetProvider.getConstants();

    @UiField
    SimpleDialogButton integrityCheckButton;

    @UiField
    SimpleDialogButton clientManagementButton;

    @UiField
    SimpleDialogButton closeButton;

    private Runnable integrityCheckHandler;
    private Runnable clientManagementHandler;

    @Inject
    public SecuritySettingsPopupView(EventBus eventBus) {
        super(eventBus);
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
    }

    @Override
    public void setIntegrityCheckHandler(Runnable handler) {
        this.integrityCheckHandler = handler;
    }

    @Override
    public void setClientManagementHandler(Runnable handler) {
        this.clientManagementHandler = handler;
    }

    @UiHandler("integrityCheckButton")
    void onIntegrityCheckClick(ClickEvent event) {
        if (integrityCheckHandler != null) {
            integrityCheckHandler.run();
        }
    }

    @UiHandler("clientManagementButton")
    void onClientManagementClick(ClickEvent event) {
        if (clientManagementHandler != null) {
            clientManagementHandler.run();
        }
    }

    @UiHandler("closeButton")
    void onCloseClick(ClickEvent event) {
        hide();
    }

    @Override
    public HasClickHandlers getCloseButton() {
        return closeButton;
    }

    @Override
    public HasClickHandlers getCloseIconButton() {
        return asWidget().getCloseIconButton();
    }

    @Override
    public HandlerRegistration setPopupKeyPressHandler(PopupNativeKeyPressHandler handler) {
        return asWidget().setKeyPressHandler(handler);
    }
}
