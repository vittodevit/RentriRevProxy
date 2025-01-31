package app.fiuto.rentrirevproxy.model;

import app.fiuto.rentrirevproxy.utils.Utils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.io.Serializable;
import java.util.Map;

public class SerializableResponseEntity implements Serializable {

    private final int httpStatusCode;

    private final Map<String, String> responseHeaders;

    private final String responseBody;

    public SerializableResponseEntity(
            HttpStatusCode httpStatusCode,
            HttpHeaders responseHeaders,
            String responseBody
    ) {
        this.httpStatusCode = httpStatusCode.value();
        this.responseHeaders = responseHeaders.toSingleValueMap();
        this.responseBody = responseBody;

    }

    public ResponseEntity<String> toResponseEntity(int cacheExpiry, String cacheKey) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("X-Cache-Status", "HIT");
        responseHeaders.set("X-Cache-Expiry", "" + cacheExpiry); //porcata gigante
        responseHeaders.set("X-Cache-Key", cacheKey);
        responseHeaders.putAll(Utils.convertToMultiValueMap(this.responseHeaders));
        return ResponseEntity
                .status(HttpStatusCode.valueOf(httpStatusCode))
                .headers(responseHeaders)
                .body(responseBody);
    }

}
