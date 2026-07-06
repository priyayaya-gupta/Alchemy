package com.example.alchemy.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Service
public class MemoryService {

    private static final Logger log = LoggerFactory.getLogger(MemoryService.class);

    private static final String MEMORY_PREFIX =
            "alchemy:memory:summary:";

    private final JedisPool jedisPool;

    @Value("${app.memory.ttl-days:30}")
    private long ttlDays;

    public MemoryService(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public String getSummary(String sessionId) {

        try (Jedis jedis = jedisPool.getResource()) {

            return jedis.get(MEMORY_PREFIX + sessionId);

        } catch (Exception e) {

            log.error("Failed to retrieve conversation summary", e);
            return null;
        }
    }

    public void saveSummary(String sessionId, String summary) {

        try (Jedis jedis = jedisPool.getResource()) {

            String key = MEMORY_PREFIX + sessionId;

            jedis.set(key, summary);

            jedis.expire(key, (int) (ttlDays * 24 * 60 * 60));

            log.info("Conversation summary saved for session {}", sessionId);

        } catch (Exception e) {

            log.error("Failed to save conversation summary", e);
        }
    }

    public void deleteSummary(String sessionId) {

        try (Jedis jedis = jedisPool.getResource()) {

            jedis.del(MEMORY_PREFIX + sessionId);

            log.info("Conversation summary deleted for session {}", sessionId);

        } catch (Exception e) {

            log.error("Failed to delete conversation summary", e);
        }
    }
}

//Ye sirf summary rakhta hai. --> upar wali conversation ki summary save karega redis mai
//conversation service-->LLM summary--> MemoryService