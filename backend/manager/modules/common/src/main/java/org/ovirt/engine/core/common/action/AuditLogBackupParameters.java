package org.ovirt.engine.core.common.action;

public class AuditLogBackupParameters extends ActionParametersBase {

    private static final long serialVersionUID = 2408237070602374302L;

    private String backupPath;
    private String remoteAddress;

    public AuditLogBackupParameters() {
    }

    public AuditLogBackupParameters(String backupPath, String remoteAddress) {
        this.backupPath = backupPath;
        this.remoteAddress = remoteAddress;
    }

    public String getBackupPath() {
        return backupPath;
    }

    public void setBackupPath(String backupPath) {
        this.backupPath = backupPath;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }
}
