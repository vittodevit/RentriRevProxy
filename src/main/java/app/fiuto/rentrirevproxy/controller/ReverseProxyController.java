package app.fiuto.rentrirevproxy.controller;

import app.fiuto.rentrirevproxy.model.Bundle;
import app.fiuto.rentrirevproxy.repository.BundleRepository;
import app.fiuto.rentrirevproxy.utils.JwtFactory;
import app.fiuto.rentrirevproxy.utils.Utils;
import jakarta.servlet.http.HttpServletRequest;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Enumeration;

@Controller
class ReverseProxyController {

    private final RestTemplate restTemplate;

    @Autowired
    BundleRepository bundleRepository;

    public ReverseProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    Logger logger = LoggerFactory.getLogger(ReverseProxyController.class);

    @RequestMapping("/**")
    public ResponseEntity<String> proxyRequest(HttpServletRequest request) {

        // get certificate owner id from the Db-ID header
        String certificateOwnerId = request.getHeader("Db-ID");
        if(certificateOwnerId == null) {
            return ResponseEntity
                    .status(400)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{ \"proxyError\": \"Db-ID header not found\" }");
        }

        Bundle bundle;
        try {
            bundle = bundleRepository.findByProprietarioId(new ObjectId(certificateOwnerId));
            if (bundle == null) {
                throw new Exception("Bundle not found");
            }
        } catch (Exception e) {
            return ResponseEntity
                    .status(404)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{ \"proxyError\": \"Bundle not found for that user\" }");
        }

        if(bundle.getExtractedBundle() == null) {
            return ResponseEntity
                    .status(400)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{ \"proxyError\": \"Bundle not yet extracted\" }");
        }

        JwtFactory jwtFactory;

        try {
            jwtFactory = new JwtFactory(bundle.getExtractedBundle());
        } catch (Exception e) {
            return ResponseEntity
                    .status(500)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{ \"proxyError\": \"Error creating JWT factory\" }");
        }

        try {
            String targetUrl = Utils.determineTargetUrl(
                    request,
                    jwtFactory.getExtractedBundle().isEnableApiDemoMode()
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

            headers.set("Authorization", "Bearer " + jwtFactory.createJWT());

            if(method.matches("POST") || method.matches("PUT")) {
                headers.set("Content-Type", "application/json");

                String digest = Utils.createDigest(body);
                String agidJwtSignature = jwtFactory.createAgidSignatureJWT(digest);

                headers.set("Digest", "SHA-256=" + digest);
                headers.set("Agid-JWT-Signature", agidJwtSignature);
            }


            HttpEntity<?> httpEntity = new HttpEntity<>(body, headers);

            // Send the request
            logger.info("Proxying request to: " + targetUrl);
            var response = restTemplate.exchange(URI.create(targetUrl), method, httpEntity, String.class);

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.putAll(response.getHeaders());
            responseHeaders.set("X-Signed-By", jwtFactory.getExtractedBundle().getJwtIssuer());

            return ResponseEntity.status(response.getStatusCode())
                    .headers(responseHeaders)
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