package app.fiuto.rentrirevproxy.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CertificateExtractor {
    public static ExtractedBundle extract(String b64EncodedFile, String password) throws Exception {
        try {

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            byte[] decodedBytes = Base64.getDecoder().decode(b64EncodedFile);
            InputStream inputStream = new ByteArrayInputStream(decodedBytes);
            keyStore.load(inputStream, password.toCharArray());

            //TODO: guessing the first alias is always the correct one
            String alias = keyStore.aliases().nextElement();
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
            Certificate certificate = keyStore.getCertificate(alias);

            if (certificate instanceof X509Certificate x509Certificate) {
                try {
                    String distinguishedName = x509Certificate.getSubjectX500Principal().toString();

                    //String oPattern = "O=([^,]+)";
                    //String dnqPattern = "DNQ=RENTRI-([0-9]+)";
                    String cfPattern = "CF:([^,]+)";

                    //TODO: edit this if RENTRI will support non italian fiscal codes
                    String issuerCF = extractUsingRegex(cfPattern, distinguishedName);
                    if (issuerCF == null || !issuerCF.startsWith("IT-")) {
                        throw new Exception("Only Italian fiscal codes are supported at the moment");
                    }

                    issuerCF = issuerCF.substring(3);
                    String issuer = x509Certificate.getIssuerX500Principal().toString();
                    boolean isDemo = issuer.contains("DEMO");

                    return new ExtractedBundle(x509Certificate, privateKey, issuerCF, isDemo);

                } catch (Exception e) {
                    throw new Exception("Cannot extract RENTRI specific login information from certificate: " + e.getMessage());
                }
            } else {
                throw new Exception("Certificate is not an X509 certificate");
            }
        } catch (Exception e) {
            throw new Exception("Error extracting certificate and private key: " + e.getMessage());
        }
    }

    private static String extractUsingRegex(String pattern, String input) {
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
