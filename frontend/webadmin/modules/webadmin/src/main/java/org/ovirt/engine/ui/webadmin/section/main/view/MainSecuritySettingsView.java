package org.ovirt.engine.ui.webadmin.section.main.view;

import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.uicommon.model.MainModelProvider;
import org.ovirt.engine.ui.common.widget.table.column.AbstractTextColumn;
import org.ovirt.engine.ui.uicommonweb.models.SecuritySettingsListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.gin.AssetProvider;
import org.ovirt.engine.ui.webadmin.section.main.presenter.MainSecuritySettingsPresenter;

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;

public class MainSecuritySettingsView extends AbstractMainWithDetailsTableView<Object, SecuritySettingsListModel>
        implements MainSecuritySettingsPresenter.ViewDef {

    interface ViewIdHandler extends ElementIdHandler<MainSecuritySettingsView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    private static final ApplicationConstants constants = AssetProvider.getConstants();

    @Inject
    public MainSecuritySettingsView(MainModelProvider<Object, SecuritySettingsListModel> modelProvider) {
        super(modelProvider);
        ViewIdHandler.idHandler.generateAndSetIds(this);
        initTable();
        initWidget(getTable());
    }

    void initTable() {
        getTable().enableColumnResizing();

        AbstractTextColumn<Object> nameColumn =
                new AbstractTextColumn<Object>() {
                    @Override
                    public String getValue(Object object) {
                        return constants.securitySettingsMainViewLabel();
                    }
                };
        getTable().addColumn(nameColumn, constants.nameLabel(), "300px"); //$NON-NLS-1$

        AbstractTextColumn<Object> descriptionColumn =
                new AbstractTextColumn<Object>() {
                    @Override
                    public String getValue(Object object) {
                        return "Security settings management"; //$NON-NLS-1$
                    }
                };
        getTable().addColumn(descriptionColumn, constants.descriptionVm(), "400px"); //$NON-NLS-1$
    }
}
