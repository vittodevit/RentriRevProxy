package app.fiuto.rentrirevproxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class RentriRevProxyApplication {

    public static void main(String[] args) {
        String mongoUri = System.getenv("MONGODB_URI");
        // Replace the placeholder with your MongoDB deployment's connection string
        SpringApplication.run(RentriRevProxyApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
