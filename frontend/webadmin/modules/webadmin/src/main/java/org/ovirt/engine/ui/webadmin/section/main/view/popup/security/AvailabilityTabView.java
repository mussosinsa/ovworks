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

public class AvailabilityTabView extends Composite {

    interface ViewUiBinder extends UiBinder<Widget, AvailabilityTabView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    @UiField
    Button engineBackupButton;

    @UiField
    HTML engineBackupResultLabel;

    @UiField
    TextBox engineBackupPathInput;

    public AvailabilityTabView() {
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        initializeHandlers();
    }

    private void initializeHandlers() {
        engineBackupButton.addClickHandler((ClickHandler) event -> {
            String backupPath = engineBackupPathInput.getText().trim();
            if (backupPath.isEmpty()) {
                engineBackupResultLabel.setText("저장 위치를 입력해 주세요."); //$NON-NLS-1$
                return;
            }
            engineBackupResultLabel.setHTML(formatHtml(buildSuccessMessage(backupPath)));
        });
    }

    private String buildSuccessMessage(String backupPath) {
        Date now = new Date();
        String timestamp = DateTimeFormat.getFormat("yyyy-MM-dd HH:mm:ss").format(now); //$NON-NLS-1$
        String backupFile = buildPath(backupPath, "engine_backup.tar.gz"); //$NON-NLS-1$
        String logFile = buildPath(backupPath, "engine_backup.log"); //$NON-NLS-1$
        return "처리날짜 : " + timestamp + " - 정상저장\n" //$NON-NLS-1$ //$NON-NLS-2$
                + "실행 명령: engine-backup --mode=backup --file=" + backupFile //$NON-NLS-1$
                + " --log=" + logFile; //$NON-NLS-1$
    }

    private String buildPath(String basePath, String filename) {
        if (basePath.endsWith("/")) { //$NON-NLS-1$
            return basePath + filename;
        }
        return basePath + "/" + filename; //$NON-NLS-1$
    }

    private String formatHtml(String message) {
        return SafeHtmlUtils.fromString(message).asString().replace("\n", "<br/>"); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
