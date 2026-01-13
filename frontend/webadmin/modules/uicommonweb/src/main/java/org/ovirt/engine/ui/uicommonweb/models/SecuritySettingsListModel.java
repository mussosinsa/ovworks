package org.ovirt.engine.ui.uicommonweb.models;

public class SecuritySettingsListModel extends ListWithDetailsModel {

    public SecuritySettingsListModel() {
        super();
        setTitle("Security Settings"); //$NON-NLS-1$
        // HelpTag setHelpTag(HelpTag.content);
        setHashName("security_settings"); //$NON-NLS-1$
    }

    @Override
    protected void onEntityChanged() {
        super.onEntityChanged();
    }

    @Override
    protected void syncSearch() {
        super.syncSearch();
    }

    @Override
    protected Object provideDetailModelEntity(Object selectedItem) {
        return selectedItem;
    }

    @Override
    protected String getListName() {
        return "SecuritySettingsListModel"; //$NON-NLS-1$
    }
}
