package app.fiuto.rentrirevproxy.controller;

import app.fiuto.rentrirevproxy.RedisService;
import app.fiuto.rentrirevproxy.model.Bundle;
import app.fiuto.rentrirevproxy.model.SerializableResponseEntity;
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

    @Autowired
    RedisService redisService;


    public ReverseProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    Logger logger = LoggerFactory.getLogger(ReverseProxyController.class);

    private static final int CACHE_EXPIRY = Integer.parseInt(System.getenv("CACHE_EXPIRY"));

    @RequestMapping("/**")
    public ResponseEntity<String> proxyRequest(HttpServletRequest request) {

        // NO BUNDLE CHECK IS NEEDED:
        // The bundle is needed only for API's that are not "codifiche",
        // an API that does not really require authentication because it
        // exposes no personal data.
        boolean isCacheable = request.getRequestURI().contains("codifiche/v1.0") && CACHE_EXPIRY > 0;
        String cacheKey = "";
        if (isCacheable) {
            cacheKey = Utils.calculateCacheKeyFromRequest(request);
            // pull it from cache if it exists
            var byteCached = (byte[]) redisService.get(cacheKey);
            SerializableResponseEntity cachedResponse = null;
            if (byteCached != null) {
                cachedResponse = (SerializableResponseEntity) Utils.deserialize(byteCached);
            }
            if (cachedResponse != null) {
                // passing expiry and key to the response entity
                // because headers are not editable after the response is created
                return cachedResponse.toResponseEntity(CACHE_EXPIRY, cacheKey);
            }
        }

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
            logger.info("Proxying request to: {}", targetUrl);
            var response = restTemplate.exchange(URI.create(targetUrl), method, httpEntity, String.class);

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.putAll(response.getHeaders());
            responseHeaders.set("X-Signed-By", jwtFactory.getExtractedBundle().getJwtIssuer());

            // if it arrived here the cache did not hit, save the response
            if (isCacheable) {
                var cachedResponse = new SerializableResponseEntity(
                        response.getStatusCode(),
                        responseHeaders,
                        response.getBody()
                );
                byte[] serialized = Utils.serialize(cachedResponse);
                redisService.save(cacheKey, serialized, CACHE_EXPIRY);
            }

            return ResponseEntity.status(response.getStatusCode())
                    .headers(responseHeaders)
                    .body(response.getBody());

        } catch (RestClientException restClientException) {
            logger.error("Error proxying request: {}", restClientException.getMessage());
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
            logger.error("Error in reverse proxy: {}", e.getMessage());
            return ResponseEntity
                    .status(500)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Utils.getJsonFromException(e));
        }
    }
}