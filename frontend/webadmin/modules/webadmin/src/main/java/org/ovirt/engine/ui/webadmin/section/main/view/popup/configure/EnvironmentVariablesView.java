package org.ovirt.engine.ui.webadmin.section.main.view.popup.configure;

import java.util.ArrayList;
import java.util.List;

import org.gwtbootstrap3.client.ui.Button;
import org.ovirt.engine.core.common.action.ActionParametersBase;
import org.ovirt.engine.core.common.action.ActionType;
import org.ovirt.engine.core.common.action.EngineConfigValueParameters;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.uicompat.FrontendActionAsyncResult;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;

public class EnvironmentVariablesView extends Composite {

    private static final class ConfigRow {
        private String name;
        private String description;
        private String value;

        ConfigRow(String name, String description) {
            this.name = name;
            this.description = description;
            this.value = ""; //$NON-NLS-1$
        }
    }

    interface ViewUiBinder extends UiBinder<Widget, EnvironmentVariablesView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    @UiField
    Button refreshButton;

    @UiField
    Button queryButton;

    @UiField
    Button updateButton;

    @UiField
    TextBox keyTextBox;

    @UiField
    TextBox valueTextBox;

    @UiField(provided = true)
    CellTable<ConfigRow> configTable;

    @UiField
    HTML resultLabel;

    private final ListDataProvider<ConfigRow> provider = new ListDataProvider<>();
    private String lastQueriedKey;

    public EnvironmentVariablesView() {
        configTable = new CellTable<>();
        initColumns();
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        provider.addDataDisplay(configTable);
        initHandlers();
        updateButton.setEnabled(false);
        refreshEntries();
    }

    private void initColumns() {
        Column<ConfigRow, String> nameCol = new Column<ConfigRow, String>(new TextCell()) {
            @Override
            public String getValue(ConfigRow object) {
                return object != null ? object.name : ""; //$NON-NLS-1$
            }
        };
        Column<ConfigRow, String> descCol = new Column<ConfigRow, String>(new TextCell()) {
            @Override
            public String getValue(ConfigRow object) {
                return object != null ? object.description : ""; //$NON-NLS-1$
            }
        };
        Column<ConfigRow, String> valueCol = new Column<ConfigRow, String>(new TextCell()) {
            @Override
            public String getValue(ConfigRow object) {
                return object != null ? object.value : ""; //$NON-NLS-1$
            }
        };

        configTable.addColumn(nameCol, "이름"); //$NON-NLS-1$
        configTable.addColumn(descCol, "설명"); //$NON-NLS-1$
        configTable.addColumn(valueCol, "값"); //$NON-NLS-1$
        configTable.setWidth("100%"); //$NON-NLS-1$
    }

    private void initHandlers() {
        refreshButton.addClickHandler((ClickHandler) event -> refreshEntries());

        configTable.addDomHandler((ClickEvent event) -> {
            ConfigRow selected = configTable.getVisibleItem(configTable.getKeyboardSelectedRow());
            if (selected != null) {
                keyTextBox.setValue(selected.name);
                valueTextBox.setValue(""); //$NON-NLS-1$
                clearQueriedState();
            }
        }, ClickEvent.getType());

        queryButton.addClickHandler((ClickHandler) event -> queryValue());
        updateButton.addClickHandler((ClickHandler) event -> updateValue());
    }

    private void refreshEntries() {
        clearQueriedState();
        keyTextBox.setValue(""); //$NON-NLS-1$
        valueTextBox.setValue(""); //$NON-NLS-1$
        resultLabel.setText("환경변수 목록 조회 중..."); //$NON-NLS-1$
        Frontend.getInstance().runAction(ActionType.ListEngineConfigProperties, new ActionParametersBase(),
                result -> {
                    List<ConfigRow> entries = new ArrayList<>();
                    if (result != null && result.getReturnValue() != null && result.getReturnValue().getSucceeded()) {
                        Object value = result.getReturnValue().getActionReturnValue();
                        if (value instanceof List<?>) {
                            for (Object item : (List<?>) value) {
                                if (item == null) {
                                    continue;
                                }
                                String line = item.toString();
                                String[] parts = line.split("\t", 2); //$NON-NLS-1$
                                String name = parts.length > 0 ? parts[0] : ""; //$NON-NLS-1$
                                String description = parts.length > 1 ? parts[1] : ""; //$NON-NLS-1$
                                entries.add(new ConfigRow(name, description));
                            }
                        }
                        provider.getList().clear();
                        provider.getList().addAll(entries);
                        resultLabel.setText("조회 완료: " + entries.size() + "건"); //$NON-NLS-1$ //$NON-NLS-2$
                        return;
                    }
                    resultLabel.setText("환경변수 목록 조회 실패"); //$NON-NLS-1$
                });
    }

    private void queryValue() {
        clearQueriedState();
        String key = keyTextBox.getText() != null ? keyTextBox.getText().trim() : ""; //$NON-NLS-1$
        if (key.isEmpty()) {
            resultLabel.setText("키를 입력해 주세요."); //$NON-NLS-1$
            return;
        }

        resultLabel.setText("조회 중..."); //$NON-NLS-1$
        Frontend.getInstance().runAction(ActionType.GetEngineConfigValue, new EngineConfigValueParameters(key),
                result -> handleEngineConfigResult(result, false));
    }

    private void updateValue() {
        String key = keyTextBox.getText() != null ? keyTextBox.getText().trim() : ""; //$NON-NLS-1$
        if (key.isEmpty()) {
            resultLabel.setText("키를 입력해 주세요."); //$NON-NLS-1$
            return;
        }

        if (lastQueriedKey == null || !key.equals(lastQueriedKey)) {
            resultLabel.setText("먼저 값을 조회한 후 수정해 주세요."); //$NON-NLS-1$
            return;
        }

        String value = valueTextBox.getText() == null ? "" : valueTextBox.getText(); //$NON-NLS-1$
        resultLabel.setText("수정 중..."); //$NON-NLS-1$
        Frontend.getInstance().runAction(ActionType.SetEngineConfigValue, new EngineConfigValueParameters(key, value),
                result -> {
                    handleEngineConfigResult(result, true);
                    queryValue();
                });
    }

    private void handleEngineConfigResult(FrontendActionAsyncResult result, boolean isUpdate) {
        String defaultError = isUpdate ? "수정 실패" : "조회 실패"; //$NON-NLS-1$ //$NON-NLS-2$
        if (result != null && result.getReturnValue() != null) {
            Object output = result.getReturnValue().getActionReturnValue();
            if (result.getReturnValue().getSucceeded()) {
                String text = output instanceof String ? (String) output : (isUpdate ? "수정 완료" : "조회 완료"); //$NON-NLS-1$ //$NON-NLS-2$
                if (!isUpdate && output instanceof String) {
                    String key = keyTextBox.getText().trim();
                    String currentValue = extractEngineConfigValue((String) output, key);
                    valueTextBox.setValue(currentValue);
                    lastQueriedKey = key;
                    updateButton.setEnabled(true);
                    updateCurrentRow(key, currentValue);
                }
                resultLabel.setHTML(SafeHtmlUtils.fromString(text).asString().replace("\n", "<br/>")); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }
            if (output instanceof String && !((String) output).isEmpty()) {
                resultLabel.setHTML(SafeHtmlUtils.fromString((String) output).asString().replace("\n", "<br/>")); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }
        }
        resultLabel.setText(defaultError);
    }

    private void clearQueriedState() {
        lastQueriedKey = null;
        updateButton.setEnabled(false);
    }

    private String extractEngineConfigValue(String output, String key) {
        String[] lines = output.split("\\r?\\n"); //$NON-NLS-1$
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.startsWith(key + ":")) { //$NON-NLS-1$
                return trimmed.substring(key.length() + 1).trim();
            }
            if (trimmed.startsWith(key + "=")) { //$NON-NLS-1$
                return trimmed.substring(key.length() + 1).trim();
            }
        }
        return output.trim();
    }

    private void updateCurrentRow(String key, String value) {
        for (ConfigRow e : provider.getList()) {
            if (e.name != null && e.name.equals(key)) {
                e.value = value;
                configTable.redraw();
                return;
            }
        }
    }
}
