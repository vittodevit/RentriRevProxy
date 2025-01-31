package app.fiuto.rentrirevproxy.controller;

import app.fiuto.rentrirevproxy.model.Bundle;
import app.fiuto.rentrirevproxy.repository.BundleRepository;
import app.fiuto.rentrirevproxy.model.CumulativeResponse;
import app.fiuto.rentrirevproxy.utils.JwtFactory;
import app.fiuto.rentrirevproxy.model.RentriCheckResponse;
import app.fiuto.rentrirevproxy.utils.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Controller
public class StatusController {

    @Autowired
    BundleRepository bundleRepository;

    private final RestTemplate restTemplate;

    public StatusController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    Logger logger = LoggerFactory.getLogger(ReverseProxyController.class);

    @RequestMapping("/status")
    public ResponseEntity<String> proxyRequest(
            HttpServletRequest request
    ) {
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

        CumulativeResponse statuses = new CumulativeResponse();
        statuses.setResponseAnagrafiche(sendRequest("/anagrafiche/v1.0/status", jwtFactory));
        statuses.setResponseCaRentri(sendRequest("/ca-rentri/v1.0/status", jwtFactory));
        statuses.setResponseCodifiche(sendRequest("/codifiche/v1.0/status", jwtFactory));
        statuses.setResponseDatiRegistri(sendRequest("/dati-registri/v1.0/status", jwtFactory));
        statuses.setResponseFormulari(sendRequest("/formulari/v1.0/status", jwtFactory));
        statuses.setResponseVidimazioneFormulari(sendRequest("/vidimazione-formulari/v1.0/status", jwtFactory));

        ObjectMapper Obj = new ObjectMapper();
        try {
            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Obj.writeValueAsString(statuses));
        } catch (Exception e) {
            return ResponseEntity
                    .status(500)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{ \"proxyError\": \"Error serializing statuses\" }");
        }

    }

    private RentriCheckResponse sendRequest(String target, JwtFactory jwtFactory) {
        try {
            String targetUrl = Utils.determineTargetUrl(
                    target,
                    jwtFactory.getExtractedBundle().isEnableApiDemoMode()
            );

            var headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtFactory.createJWT());

            HttpEntity<?> httpEntity = new HttpEntity<>(new byte[]{}, headers);

            // Send the request
            logger.info("Proxying request to: " + targetUrl);
            var response = restTemplate.exchange(
                    URI.create(targetUrl),
                    HttpMethod.GET,
                    httpEntity,
                    String.class
            );

            // check 200 status
            if(response.getStatusCode() != HttpStatus.OK){
                return new RentriCheckResponse(false, response.getBody());
            }
        } catch (Exception e) {
            return new RentriCheckResponse(false, e.getMessage());
        }

        return new RentriCheckResponse(true, "");
    }
}