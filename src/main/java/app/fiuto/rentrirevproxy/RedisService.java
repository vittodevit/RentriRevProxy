package app.fiuto.rentrirevproxy;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Store object in Redis
    public void save(String key, Object value, long timeoutSeconds) {
        redisTemplate.opsForValue().set(key, value, timeoutSeconds, TimeUnit.SECONDS);
    }

    // Retrieve object from Redis
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    // Delete object from Redis
    public void delete(String key) {
        redisTemplate.delete(key);
    }
}
