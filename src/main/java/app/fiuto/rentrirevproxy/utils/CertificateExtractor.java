package app.fiuto.rentrirevproxy.utils;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CertificateExtractor {
    public static ExtractedBundle extract(String p12FilePath, String passwordFilePath) throws Exception {
        try {
            String password = new String(java.nio.file.Files.readAllBytes(new File(passwordFilePath).toPath())).trim();

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(p12FilePath)) {
                keyStore.load(fis, password.toCharArray());
            }

            //TODO: guessing the first alias is always the correct one
            String alias = keyStore.aliases().nextElement();
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
            Certificate certificate = keyStore.getCertificate(alias);

            if (certificate instanceof X509Certificate x509Certificate) {
                try {
                    String distinguishedName = x509Certificate.getSubjectX500Principal().toString();

                    String oPattern = "O=([^,]+)";
                    String cfPattern = "CF:([^,]+)";
                    String dnqPattern = "DNQ=RENTRI-([0-9]+)";

                    //TODO: edit this if RENTRI will support non italian fiscal codes
                    String issuerCF = extractUsingRegex(cfPattern, distinguishedName);
                    if (issuerCF == null || !issuerCF.startsWith("IT-")) {
                        throw new Exception("Only Italian fiscal codes are supported at the moment");
                    }

                    String organization = extractUsingRegex(oPattern, distinguishedName);
                    System.out.println("JWT will be signed as  : " + organization);

                    // remove the IT- prefix
                    issuerCF = issuerCF.substring(3);
                    System.out.println("Fiscal code            : " + issuerCF);

                    String dnq = extractUsingRegex(dnqPattern, distinguishedName);
                    System.out.println("RENTRI operator id     : " + dnq);

                    String issuer = x509Certificate.getIssuerX500Principal().toString();

                    // watch out for demo flag in issuer
                    boolean isDemo = issuer.contains("DEMO");
                    System.out.println("API Mode               : " + (isDemo ? "DEMO" : "PRODUCTION"));

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
