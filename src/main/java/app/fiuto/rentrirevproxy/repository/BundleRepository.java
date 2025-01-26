package app.fiuto.rentrirevproxy.repository;

import app.fiuto.rentrirevproxy.model.Bundle;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface BundleRepository extends MongoRepository<Bundle, String> {
    Bundle findByProprietarioId(ObjectId proprietarioId);

}
