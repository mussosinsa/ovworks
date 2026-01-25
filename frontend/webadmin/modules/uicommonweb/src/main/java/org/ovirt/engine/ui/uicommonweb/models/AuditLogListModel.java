package org.ovirt.engine.ui.uicommonweb.models;

import org.ovirt.engine.ui.uicommonweb.help.HelpTag;

/**
 * Model for Audit Log Management main tab
 */
public class AuditLogListModel extends ListWithDetailsModel<Object, Object> {

    public AuditLogListModel() {
        setTitle("Audit Log Management"); //$NON-NLS-1$
        setHelpTag(HelpTag.audit_log);
        setHashName("audit_log"); //$NON-NLS-1$
    }

    @Override
    protected void syncSearch() {
        // No search functionality needed for this page
    }

    @Override
    protected String getListName() {
        return "AuditLogListModel"; //$NON-NLS-1$
    }
}
