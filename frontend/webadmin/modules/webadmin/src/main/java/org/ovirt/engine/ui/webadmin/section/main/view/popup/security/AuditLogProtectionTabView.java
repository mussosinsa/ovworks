package org.ovirt.engine.ui.webadmin.section.main.view.popup.security;

import java.util.Date;

import org.gwtbootstrap3.client.ui.Button;
import org.ovirt.engine.core.common.action.ActionType;
import org.ovirt.engine.core.common.action.AuditLogBackupParameters;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.uicompat.FrontendActionAsyncResult;

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

public class AuditLogProtectionTabView extends Composite {

    interface ViewUiBinder extends UiBinder<Widget, AuditLogProtectionTabView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    @UiField
    Button fullLogBackupButton;

    @UiField
    HTML fullLogBackupResultLabel;

    @UiField
    TextBox fullLogBackupPathInput;

    public AuditLogProtectionTabView() {
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        initializeHandlers();
    }

    private void initializeHandlers() {
        fullLogBackupButton.addClickHandler((ClickHandler) event -> {
            String backupPath = fullLogBackupPathInput.getText().trim();
            if (backupPath.isEmpty()) {
                fullLogBackupResultLabel.setText("저장 위치를 입력해 주세요."); //$NON-NLS-1$
                return;
            }
            AuditLogBackupParameters parameters = new AuditLogBackupParameters();
            parameters.setBackupPath(backupPath);
            fullLogBackupResultLabel.setText("처리 중..."); //$NON-NLS-1$
            Frontend.getInstance().runAction(ActionType.FullLogBackup, parameters, result -> {
                handleResult(result, buildSuccessMessage(backupPath), fullLogBackupResultLabel);
            });
        });
    }

    private String buildSuccessMessage(String backupPath) {
        return "처리날짜 : " + currentTimestamp() + " - 정상저장\n" //$NON-NLS-1$ //$NON-NLS-2$
                + "실행 명령: tar cvfz " + buildPath(backupPath, archiveFilename()) //$NON-NLS-1$
                + " /var/log/ovirt-engine/"; //$NON-NLS-1$
    }

    private String buildPath(String basePath, String filename) {
        if (basePath.endsWith("/")) { //$NON-NLS-1$
            return basePath + filename;
        }
        return basePath + "/" + filename; //$NON-NLS-1$
    }

    private String archiveFilename() {
        return DateTimeFormat.getFormat("yyyyMMddHHmmss").format(new Date()) //$NON-NLS-1$
                + ".tar.gz"; //$NON-NLS-1$
    }

    private String currentTimestamp() {
        return DateTimeFormat.getFormat("yyyy-MM-dd HH:mm:ss").format(new Date()); //$NON-NLS-1$
    }

    private void handleResult(FrontendActionAsyncResult result, String successMessage, HTML target) {
        StringBuilder details = new StringBuilder();
        if (result != null && result.getReturnValue() != null) {
            if (result.getReturnValue().getSucceeded()) {
                details.append(successMessage);
                Object actionReturnValue = result.getReturnValue().getActionReturnValue();
                if (actionReturnValue instanceof String && !((String) actionReturnValue).isEmpty()) {
                    details.append("\n").append(actionReturnValue); //$NON-NLS-1$
                }
                target.setHTML(formatHtml(details.toString()));
                return;
            }
            if (result.getReturnValue().getExecuteFailedMessages() != null) {
                result.getReturnValue().getExecuteFailedMessages().forEach(msg -> {
                    details.append(msg).append("\n"); //$NON-NLS-1$
                });
            }
            Object actionReturnValue = result.getReturnValue().getActionReturnValue();
            if (actionReturnValue instanceof String && !((String) actionReturnValue).isEmpty()) {
                details.append(actionReturnValue);
            }
        }
        if (details.length() == 0) {
            details.append("백업 실행 중 오류가 발생했습니다."); //$NON-NLS-1$
        }
        target.setHTML(formatHtml(details.toString().trim()));
    }

    private String formatHtml(String message) {
        return SafeHtmlUtils.fromString(message).asString().replace("\n", "<br/>"); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
