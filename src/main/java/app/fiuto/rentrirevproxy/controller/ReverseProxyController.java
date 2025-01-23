package app.fiuto.rentrirevproxy.controller;

import app.fiuto.rentrirevproxy.utils.SharedContext;
import app.fiuto.rentrirevproxy.utils.Utils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Enumeration;

@Controller
class ReverseProxyController {

    private final RestTemplate restTemplate;

    public ReverseProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    Logger logger = LoggerFactory.getLogger(ReverseProxyController.class);

    @RequestMapping("/**")
    public ResponseEntity<String> proxyRequest(HttpServletRequest request) {
        try {
            String targetUrl = Utils.determineTargetUrl(
                    request,
                    SharedContext.jwtFactory.getExtractedBundle().isEnableApiDemoMode()
            );
            HttpMethod method = HttpMethod.valueOf(request.getMethod());
            var body = request.getInputStream().readAllBytes();

            // Forward headers
            var headers = new org.springframework.http.HttpHeaders();
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                // override eventual authorization header from client
                if(!headerName.equalsIgnoreCase("Authorization")) {
                    headers.set(headerName, request.getHeader(headerName));
                }
            }

            headers.set("Authorization", "Bearer " + SharedContext.jwtFactory.createJWT());

            if(method.matches("POST") || method.matches("PUT")) {
                headers.set("Content-Type", "application/json");

                String digest = Utils.createDigest(body);
                String agidJwtSignature = SharedContext.jwtFactory.createAgidSignatureJWT(digest);

                headers.set("Digest", "SHA-256=" + digest);
                headers.set("Agid-JWT-Signature", agidJwtSignature);
            }


            HttpEntity<?> httpEntity = new HttpEntity<>(body, headers);

            // Send the request
            logger.info("Proxying request to: " + targetUrl);
            var response = restTemplate.exchange(URI.create(targetUrl), method, httpEntity, String.class);

            return ResponseEntity.status(response.getStatusCode())
                    .headers(response.getHeaders())
                    .body(response.getBody());

        } catch (RestClientException restClientException) {
            logger.error("Error proxying request: " + restClientException.getMessage());
            if (restClientException instanceof org.springframework.web.client.HttpClientErrorException httpClientErrorException) {
                return ResponseEntity
                        .status(httpClientErrorException.getStatusCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(httpClientErrorException.getResponseBodyAsString());
            } else {
                return ResponseEntity
                        .status(500)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Utils.getJsonFromException(restClientException));
            }
        } catch (Exception e) {
            // something wrong with the proxy
            logger.error("Error in reverse proxy: " + e.getMessage());
            return ResponseEntity
                    .status(500)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Utils.getJsonFromException(e));
        }
    }
}