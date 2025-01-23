package app.fiuto.rentrirevproxy;

import app.fiuto.rentrirevproxy.utils.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class RentriRevProxyApplication {

    public static void main(String[] args) {
        String serverPort = System.getenv("SERVER_PORT");
        String p12FilePath = System.getenv("BUNDLE_PATH");
        String passwordFilePath = System.getenv("BUNDLE_PASSWORD_PATH");

        Utils.welcomeScreen(p12FilePath, passwordFilePath, serverPort);

        ExtractedBundle extractedBundle;
        try {
            extractedBundle = CertificateExtractor.extract(p12FilePath, passwordFilePath);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }

        JwtFactory jwtFactory;

        try {
            jwtFactory = new JwtFactory(extractedBundle);
            // test the most complex scenario and catch errors early
            jwtFactory.createAgidSignatureJWT("test");
        } catch (Exception e) {
            System.out.println("Error initializing the JWT factory: " + e.getMessage());
            return;
        }

        SharedContext.jwtFactory = jwtFactory;
        SpringApplication.run(RentriRevProxyApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
