package org.ovirt.engine.ui.webadmin.section.main.view.popup.security;

import org.gwtbootstrap3.client.ui.Button;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class AvailabilityTabView extends Composite {

    interface ViewUiBinder extends UiBinder<Widget, AvailabilityTabView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    @UiField
    Button engineBackupButton;

    public AvailabilityTabView() {
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
    }
}
