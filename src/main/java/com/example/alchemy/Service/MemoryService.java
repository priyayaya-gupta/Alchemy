package com.example.alchemy.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Collections;
import java.util.List;

@Service
public class MemoryService {

    private final JedisPool jedisPool;

    @Value("${app.memory.max-messages:20}")
    private int maxMessages;

    @Value("${app.memory.ttl-days:7}")
    private long ttlDays;

    public MemoryService() {
        this.jedisPool = new JedisPool("localhost", 6379);
    }

    public List<String> getRecentMemory(String sessionId) {
        try (Jedis jedis = jedisPool.getResource()) {
            List<String> memory = jedis.lrange(key(sessionId), 0, maxMessages - 1);
            Collections.reverse(memory);
            return memory;
        }
    }

    public void saveTurn(String sessionId, String question, String answer) {
        try (Jedis jedis = jedisPool.getResource()) {
            String turn = "User: " + question + "\nAssistant: " + answer;

            jedis.lpush(key(sessionId), turn);
            jedis.ltrim(key(sessionId), 0, maxMessages - 1);
            jedis.expire(key(sessionId), ttlDays * 24 * 60 * 60);
        }
    }

    public void clearMemory(String sessionId) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key(sessionId));
        }
    }

    private String key(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = "default";
        }

        return "alchemy:memory:" + sessionId;
    }
}
