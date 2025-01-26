package app.fiuto.rentrirevproxy.controller;

import app.fiuto.rentrirevproxy.model.Bundle;
import app.fiuto.rentrirevproxy.repository.BundleRepository;
import app.fiuto.rentrirevproxy.utils.CertificateExtractor;
import app.fiuto.rentrirevproxy.utils.ExtractedBundle;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

@Controller
public class UnpackerController {

    @Autowired
    BundleRepository bundleRepository;

    private final RestTemplate restTemplate;

    public UnpackerController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    Logger logger = LoggerFactory.getLogger(ReverseProxyController.class);

    @RequestMapping("/unpacker/{id}")
    public ResponseEntity<String> proxyRequest(
            HttpServletRequest request,
            @PathVariable("id") String id
    ) {
        Bundle bundle;
        try {
            bundle = bundleRepository.findById(id).orElseThrow();
        } catch (Exception e) {
            return ResponseEntity
                    .status(404)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{ \"error\": \"Bundle not found\" }");
        }

        if(bundle.getExtractedBundle() != null) {
            return ResponseEntity
                    .status(400)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{ \"error\": \"Bundle already extracted\" }");
        }

        ExtractedBundle extractedBundle;
        try {
            extractedBundle =
                    CertificateExtractor.extract(bundle.getEncodedBundle(), bundle.getPasskey());
        } catch (Exception e) {
            return ResponseEntity
                    .status(500)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{ \"error\": \"Error extracting bundle:\" }");
        }

        byte[] serializedData;
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {

            objectOutputStream.writeObject(extractedBundle);
            serializedData = byteArrayOutputStream.toByteArray();

            System.out.println("Object serialized successfully into a byte array.");

        } catch (Exception e) {
            return ResponseEntity
                    .status(500)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{ \"error\": \"Error serializing bundle:\" }");
        }


        // update the bundle with the extracted bundle
        bundle.setExtractedBundle(serializedData);
        bundle.setEncodedBundle("");
        bundle.setPasskey("");
        bundleRepository.save(bundle);

        return ResponseEntity
                .status(200)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{}");

    }

}
