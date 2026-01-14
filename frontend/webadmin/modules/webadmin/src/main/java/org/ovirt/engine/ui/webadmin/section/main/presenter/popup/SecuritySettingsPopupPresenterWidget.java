package org.ovirt.engine.ui.webadmin.section.main.presenter.popup;

import org.ovirt.engine.ui.common.presenter.AbstractPopupPresenterWidget;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;

/**
 * Implements the Security Settings popup dialog.
 */
public class SecuritySettingsPopupPresenterWidget extends AbstractPopupPresenterWidget<SecuritySettingsPopupPresenterWidget.ViewDef> {

    public interface ViewDef extends AbstractPopupPresenterWidget.ViewDef {
        void setIntegrityCheckHandler(Runnable handler);
        void setClientManagementHandler(Runnable handler);
    }

    private final PlaceManager placeManager;

    @Inject
    public SecuritySettingsPopupPresenterWidget(EventBus eventBus, ViewDef view, PlaceManager placeManager) {
        super(eventBus, view);
        this.placeManager = placeManager;
    }

    @Override
    protected void onReveal() {
        super.onReveal();
        getView().setIntegrityCheckHandler(() -> {
            getView().hide();
            placeManager.revealPlace(new PlaceRequest.Builder().nameToken("integrityCheck").build()); //$NON-NLS-1$
        });
        getView().setClientManagementHandler(() -> {
            getView().hide();
            placeManager.revealPlace(new PlaceRequest.Builder().nameToken("clientManagement").build()); //$NON-NLS-1$
        });
    }
}
