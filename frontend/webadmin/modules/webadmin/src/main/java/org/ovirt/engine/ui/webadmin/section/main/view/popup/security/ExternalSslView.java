package org.ovirt.engine.ui.webadmin.section.main.view.popup.security;

import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.constants.ButtonType;
import org.ovirt.engine.core.common.action.ActionParametersBase;
import org.ovirt.engine.core.common.action.ActionType;
import org.ovirt.engine.ui.frontend.Frontend;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;

public class ExternalSslView extends Composite {

    private final Button applyButton = new Button("외부 SSL 적용"); //$NON-NLS-1$

    public ExternalSslView() {
        FlowPanel container = new FlowPanel();
        container.getElement().getStyle().setProperty("padding", "20px"); //$NON-NLS-1$ //$NON-NLS-2$
        container.getElement().getStyle().setProperty("backgroundColor", "#ffffff"); //$NON-NLS-1$ //$NON-NLS-2$

        HTML title = new HTML("<h3>외부 SSL 적용</h3>"); //$NON-NLS-1$
        container.add(title);

        HTML guide = new HTML(
                "<div style='margin-bottom:10px;color:#666;'>"
                        + "외부 SSL 적용을 실행하면 engine-setup을 통해 SSL 설정을 적용하고 "
                        + "httpd 및 ovirt-engine 서비스를 재시작합니다."
                        + "</div>"); //$NON-NLS-1$
        container.add(guide);

        applyButton.setType(ButtonType.PRIMARY);
        applyButton.addClickHandler(event -> applyExternalSsl());
        container.add(applyButton);

        initWidget(container);
    }

    private void applyExternalSsl() {
        applyButton.setEnabled(false);
        Frontend.getInstance().runAction(
                ActionType.ApplyExternalSsl,
                new ActionParametersBase(),
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
