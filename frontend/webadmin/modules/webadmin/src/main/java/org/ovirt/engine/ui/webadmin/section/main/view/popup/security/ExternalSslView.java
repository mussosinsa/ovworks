package org.ovirt.engine.ui.webadmin.section.main.view.popup.security;

import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.constants.ButtonType;
import org.ovirt.engine.core.common.action.ActionType;
import org.ovirt.engine.core.common.action.ApplyExternalSslParameters;
import org.ovirt.engine.ui.frontend.Frontend;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

public class ExternalSslView extends Composite {

    private final Button applyButton = new Button("외부 SSL 적용"); //$NON-NLS-1$
    private final TextBox privateKeyPath = new TextBox();
    private final TextBox certificatePath = new TextBox();
    private final TextBox caChainPath = new TextBox();

    public ExternalSslView() {
        FlowPanel container = new FlowPanel();
        container.getElement().getStyle().setProperty("padding", "20px"); //$NON-NLS-1$ //$NON-NLS-2$
        container.getElement().getStyle().setProperty("backgroundColor", "#ffffff"); //$NON-NLS-1$ //$NON-NLS-2$

        HTML title = new HTML("<h3>외부 SSL 적용</h3>"); //$NON-NLS-1$
        container.add(title);

        HTML guide = new HTML(
                "<div style='margin-bottom:10px;color:#666;'>" //$NON-NLS-1$
                        + "외부 SSL 적용을 실행하면 engine-setup을 통해 SSL 설정을 적용하고 " //$NON-NLS-1$
                        + "httpd/ovirt-engine 서비스 상태와 Apache 인증서를 검증합니다. " //$NON-NLS-1$
                        + "또한 모든 호스트 인증서 재등록(Enroll Certificate)을 순차 실행합니다. " //$NON-NLS-1$
                        + "실행 전 모든 호스트를 유지보수(Maintenance) 상태로 전환해야 합니다." //$NON-NLS-1$
                        + "</div>"); //$NON-NLS-1$
        container.add(guide);

        privateKeyPath.setWidth("100%"); //$NON-NLS-1$
        privateKeyPath.setText("/etc/pki/ovirt-engine/keys/apache.key.nopass"); //$NON-NLS-1$
        certificatePath.setWidth("100%"); //$NON-NLS-1$
        certificatePath.setText("/etc/pki/ovirt-engine/certs/apache.cer"); //$NON-NLS-1$
        caChainPath.setWidth("100%"); //$NON-NLS-1$
        caChainPath.setText("/etc/pki/ovirt-engine/apache-ca.pem"); //$NON-NLS-1$

        container.add(new Label("1. 서버 개인키 경로")); //$NON-NLS-1$
        container.add(privateKeyPath);
        container.add(new Label("2. 서버 인증서 경로")); //$NON-NLS-1$
        container.add(certificatePath);
        container.add(new Label("3. CA 체인 파일 경로")); //$NON-NLS-1$
        container.add(caChainPath);

        applyButton.setType(ButtonType.PRIMARY);
        applyButton.addClickHandler(event -> applyExternalSsl());
        container.add(applyButton);

        initWidget(container);
    }

    private void applyExternalSsl() {
        applyButton.setEnabled(false);
        ApplyExternalSslParameters parameters = new ApplyExternalSslParameters(
                privateKeyPath.getText(),
                certificatePath.getText(),
                caChainPath.getText());
        Frontend.getInstance().runAction(
                ActionType.ApplyExternalSsl,
                parameters,
                result -> {
                    applyButton.setEnabled(true);
                    if (result != null
                            && result.getReturnValue() != null
                            && result.getReturnValue().getSucceeded()) {
                        Window.alert("외부 SSL 적용이 완료되었습니다."); //$NON-NLS-1$
                    } else {
                        String errorMessage = "외부 SSL 적용에 실패했습니다."; //$NON-NLS-1$
                        if (result != null
                                && result.getReturnValue() != null
                                && result.getReturnValue().getExecuteFailedMessages() != null
                                && !result.getReturnValue().getExecuteFailedMessages().isEmpty()) {
                            errorMessage += "\n" + String.join("\n", result.getReturnValue().getExecuteFailedMessages()); //$NON-NLS-1$ //$NON-NLS-2$
                        }
                        Window.alert(errorMessage);
                    }
                });
    }
}
