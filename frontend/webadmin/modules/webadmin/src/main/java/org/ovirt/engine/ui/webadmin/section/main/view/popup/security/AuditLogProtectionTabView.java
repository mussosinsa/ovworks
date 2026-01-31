package org.ovirt.engine.ui.webadmin.section.main.view.popup.security;

import java.util.Date;

import org.gwtbootstrap3.client.ui.Button;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class AuditLogProtectionTabView extends Composite {

    interface ViewUiBinder extends UiBinder<Widget, AuditLogProtectionTabView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    @UiField
    Button fullLogBackupButton;

    @UiField
    Label fullLogBackupResultLabel;

    public AuditLogProtectionTabView() {
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        initializeHandlers();
    }

    private void initializeHandlers() {
        fullLogBackupButton.addClickHandler((ClickHandler) event -> {
            fullLogBackupResultLabel.setText(buildSuccessMessage());
        });
    }

    private String buildSuccessMessage() {
        String timestamp = DateTimeFormat.getFormat("yyyy-MM-dd HH:mm:ss").format(new Date()); //$NON-NLS-1$
        return "처리날짜 : " + timestamp + " - 정상저장"; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
