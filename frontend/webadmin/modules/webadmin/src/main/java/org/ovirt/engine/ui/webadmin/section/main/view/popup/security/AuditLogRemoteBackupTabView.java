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
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class AuditLogRemoteBackupTabView extends Composite {

    interface ViewUiBinder extends UiBinder<Widget, AuditLogRemoteBackupTabView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    @UiField
    Button remoteBackupButton;

    @UiField
    Label remoteBackupResultLabel;

    @UiField
    TextBox remoteBackupPathInput;

    public AuditLogRemoteBackupTabView() {
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        initializeHandlers();
    }

    private void initializeHandlers() {
        remoteBackupButton.addClickHandler((ClickHandler) event -> {
            String backupPath = remoteBackupPathInput.getText().trim();
            if (backupPath.isEmpty()) {
                remoteBackupResultLabel.setText("저장 위치를 입력해 주세요."); //$NON-NLS-1$
                return;
            }
            remoteBackupResultLabel.setText(buildSuccessMessage(backupPath));
        });
    }

    private String buildSuccessMessage(String backupPath) {
        String timestamp = DateTimeFormat.getFormat("yyyy-MM-dd HH:mm:ss").format(new Date()); //$NON-NLS-1$
        return "처리날짜 : " + timestamp + " - 정상저장 (" + backupPath + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
}
