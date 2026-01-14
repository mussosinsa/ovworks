package org.ovirt.engine.ui.webadmin.section.main.view.popup.security;

import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.gin.AssetProvider;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class ClientManagementView extends Composite {

    interface ViewUiBinder extends UiBinder<Widget, ClientManagementView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    private static final ApplicationConstants constants = AssetProvider.getConstants();

    public ClientManagementView() {
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
    }
}
