package org.ovirt.engine.core.common.action;

public class ApplyExternalSslParameters extends ActionParametersBase {

    private static final long serialVersionUID = 1L;

    private String serverPrivateKeyPath;
    private String serverCertificatePath;
    private String caChainPath;

    public ApplyExternalSslParameters() {
        // For serialization.
    }

    public ApplyExternalSslParameters(String serverPrivateKeyPath, String serverCertificatePath, String caChainPath) {
        this.serverPrivateKeyPath = serverPrivateKeyPath;
        this.serverCertificatePath = serverCertificatePath;
        this.caChainPath = caChainPath;
    }

    public String getServerPrivateKeyPath() {
        return serverPrivateKeyPath;
    }

    public void setServerPrivateKeyPath(String serverPrivateKeyPath) {
        this.serverPrivateKeyPath = serverPrivateKeyPath;
    }

    public String getServerCertificatePath() {
        return serverCertificatePath;
    }

    public void setServerCertificatePath(String serverCertificatePath) {
        this.serverCertificatePath = serverCertificatePath;
    }

    public String getCaChainPath() {
        return caChainPath;
    }

    public void setCaChainPath(String caChainPath) {
        this.caChainPath = caChainPath;
    }
}
