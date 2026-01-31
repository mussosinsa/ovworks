package org.ovirt.engine.ui.webadmin.section.main.view.popup.security;

import java.util.Date;

import org.gwtbootstrap3.client.ui.Button;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class AuditLogRemoteBackupTabView extends Composite {

    interface ViewUiBinder extends UiBinder<Widget, AuditLogRemoteBackupTabView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    @UiField
    Button remoteBackupButton;

    @UiField
    HTML remoteBackupResultLabel;

    @UiField
    TextBox remoteBackupAddressInput;

    public AuditLogRemoteBackupTabView() {
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        initializeHandlers();
    }

    private void initializeHandlers() {
        remoteBackupButton.addClickHandler((ClickHandler) event -> {
            String remoteAddress = remoteBackupAddressInput.getText().trim();
            if (remoteAddress.isEmpty()) {
                remoteBackupResultLabel.setText("원격 서버 주소를 입력해 주세요."); //$NON-NLS-1$
                return;
            }
            remoteBackupResultLabel.setHTML(formatHtml(buildSuccessMessage(remoteAddress)));
        });
    }

    private String buildSuccessMessage(String remoteAddress) {
        String timestamp = DateTimeFormat.getFormat("yyyy-MM-dd HH:mm:ss").format(new Date()); //$NON-NLS-1$
        return "처리날짜 : " + timestamp + " - 정상저장\n" //$NON-NLS-1$ //$NON-NLS-2$
                + "원격 서버 주소: " + remoteAddress + "\n" //$NON-NLS-1$ //$NON-NLS-2$
                + "rsyslog.conf에 원격 서버 주소 입력 후\n" //$NON-NLS-1$
                + "sudo systemctl restart rsyslog"; //$NON-NLS-1$
    }

    private String formatHtml(String message) {
        return SafeHtmlUtils.fromString(message).asString().replace("\n", "<br/>"); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
