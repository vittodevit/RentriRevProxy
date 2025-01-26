package app.fiuto.rentrirevproxy.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@Document(collection = "Bundle")
public class Bundle {

    @Id
    private String id;

    private String encodedBundle;

    private String passkey;

    private ObjectId proprietarioId;

    private byte[] extractedBundle;

    public String getId() {
        return id;
    }

    public String getEncodedBundle() {
        return encodedBundle;
    }

    public String getPasskey() {
        return passkey;
    }

    public ObjectId getProprietarioId() {
        return proprietarioId;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setEncodedBundle(String encodedBundle) {
        this.encodedBundle = encodedBundle;
    }

    public void setPasskey(String passkey) {
        this.passkey = passkey;
    }

    public void setProprietarioId(ObjectId proprietarioId) {
        this.proprietarioId = proprietarioId;
    }

    public byte[] getExtractedBundle() {
        return extractedBundle;
    }

    public void setExtractedBundle(byte[] extractedBundle) {
        this.extractedBundle = extractedBundle;
    }
}
