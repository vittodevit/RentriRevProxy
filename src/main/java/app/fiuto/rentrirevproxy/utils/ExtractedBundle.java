package app.fiuto.rentrirevproxy.utils;

import java.io.Serial;
import java.io.Serializable;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class ExtractedBundle implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private final X509Certificate certificate;
    private final PrivateKey privateKey;
    private final String jwtIssuer;
    private final boolean enableApiDemoMode;

    public ExtractedBundle(X509Certificate certificate, PrivateKey privateKey, String jwtIssuer, boolean enableApiDemoMode) {
        this.certificate = certificate;
        this.privateKey = privateKey;
        this.jwtIssuer = jwtIssuer;
        this.enableApiDemoMode = enableApiDemoMode;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public String getJwtIssuer() {
        return jwtIssuer;
    }

    public boolean isEnableApiDemoMode() {
        return enableApiDemoMode;
    }

}
