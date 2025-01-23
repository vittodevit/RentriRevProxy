package app.fiuto.rentrirevproxy.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.codec.digest.DigestUtils;

public class Utils {
    public static void welcomeScreen(String p12FilePath, String passwordFilePath, String serverPort){
        System.out.println("\n   ___           __      _ ___           ___                   \n" +
                "  / _ \\___ ___  / /_____(_) _ \\___ _  __/ _ \\_______ __ ____ __\n" +
                " / , _/ -_) _ \\/ __/ __/ / , _/ -_) |/ / ___/ __/ _ \\\\ \\ / // /\n" +
                "/_/|_|\\__/_//_/\\__/_/ /_/_/|_|\\__/|___/_/  /_/  \\___/_\\_\\\\_, / \n" +
                "                                                        /___/  ");
        System.out.println("Extracting cert. and key from PKCS12 file...");
        System.out.println("Cert. Bundle File   : " + p12FilePath);
        System.out.println("Password File       : " + passwordFilePath);
        System.out.println("Local Server Port   : " + serverPort);
        System.out.println();
        System.out.println("---------------------------------------------");
    }

    public static String determineTargetUrl(HttpServletRequest request, boolean isDemoMode) {
        String backendBaseUrl;
        if (isDemoMode) {
            backendBaseUrl = "https://demoapi.rentri.gov.it";
        } else {
            backendBaseUrl = "https://api.rentri.gov.it"; //TODO: probably will be changed in the future
        }
        String path = request.getRequestURI();
        String queryString = request.getQueryString();

        return backendBaseUrl + path + (queryString != null ? ("?" + queryString) : "");
    }

    public static String createDigest(byte[] requestBytes) {
        var digest = DigestUtils.sha256(requestBytes);
        return java.util.Base64.getEncoder().encodeToString(digest);
    }

    public static String getJsonFromException(Exception exception) {
        ObjectMapper Obj = new ObjectMapper();
        ExceptionAdapter exceptionAdapter = new ExceptionAdapter(exception);
        try {
            return Obj.writeValueAsString(exceptionAdapter);
        } catch (Exception e) {
            return "{\"message\":\"Error serializing exception\"}";
        }
    }
}
