package app.fiuto.rentrirevproxy.utils;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.security.cert.CertificateEncodingException;
import java.security.interfaces.ECPrivateKey;
import java.util.*;

public class JwtFactory {

    public final ExtractedBundle extractedBundle;

    //NOTE: for the time being it will be hardcoded in jwt refresh thread
    private final long expSeconds = 30; //TODO: maybe parametrize this?
    private final JWSHeader header;
    private final String certAudience;
    private final JWSSigner signer;

    public JwtFactory(ExtractedBundle extractedBundle) throws CertificateEncodingException, JOSEException {
        this.extractedBundle = extractedBundle;
        String certificateBase64 = Base64.getEncoder().encodeToString(extractedBundle.getCertificate().getEncoded());
        this.header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                .type(JOSEObjectType.JWT)
                .x509CertChain(Collections.singletonList(new com.nimbusds.jose.util.Base64(certificateBase64)))
                .build();
        certAudience = extractedBundle.isEnableApiDemoMode() ? "rentrigov.demo.api" : "rentrigov.api";
        signer = new ECDSASigner((ECPrivateKey) extractedBundle.getPrivateKey());
    }

    public String createJWT() throws Exception {
        long now = System.currentTimeMillis() / 1000L;

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .audience(certAudience)
                .issuer(extractedBundle.getJwtIssuer())
                .jwtID(UUID.randomUUID().toString())
                .notBeforeTime(new Date(now * 1000))
                .issueTime(new Date(now * 1000))
                .expirationTime(new Date((now + expSeconds) * 1000))
                .build();

        SignedJWT signedJWT = new SignedJWT(header, claimsSet);
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    public String createAgidSignatureJWT(String bodySHA265Digest) throws Exception {
        long now = System.currentTimeMillis() / 1000L;
        List<Map<String, String>> signedHeaders = getMaps(bodySHA265Digest);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .audience(certAudience)
                .issuer(extractedBundle.getJwtIssuer())
                .jwtID(UUID.randomUUID().toString())
                .notBeforeTime(new Date(now * 1000))
                .issueTime(new Date(now * 1000))
                .expirationTime(new Date((now + expSeconds) * 1000))
                .claim("signed_headers", signedHeaders)
                .build();

        SignedJWT signedJWT = new SignedJWT(header, claimsSet);
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    private static List<Map<String, String>> getMaps(String bodySHA265Digest) {
        List<Map<String, String>> signedHeaders = new ArrayList<>();

        Map<String, String> digestHeader = new HashMap<>();
        digestHeader.put("digest", "SHA-256=" + bodySHA265Digest);
        signedHeaders.add(digestHeader);

        Map<String, String> contentTypeHeader = new HashMap<>();
        contentTypeHeader.put("content-type", "application/json");
        signedHeaders.add(contentTypeHeader);
        return signedHeaders;
    }

    // lombok not yet initalized so explicit getter
    public ExtractedBundle getExtractedBundle() {
        return extractedBundle;
    }

}
