package com.example.alchemy.Service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class CacheService {

    private final StringRedisTemplate redisTemplate;

    private static final String INDEX_NAME = "alchemy_semantic_idx";
    private static final String KEY_PREFIX = "alchemy:semantic:";

    @Value("${app.cache.ttl-days}")
    private long ttlDays;

    @Value("${app.cache.similarity-threshold}")
    private double similarityThreshold;

    @Value("${app.cache.vector-dimension}")
    private int vectorDimension;

    public CacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void createVectorIndex() {
        try {
            redisTemplate.execute((RedisConnection connection) -> {
                connection.execute(
                        "FT.CREATE",
                        INDEX_NAME.getBytes(),
                        "ON".getBytes(), "HASH".getBytes(),
                        "PREFIX".getBytes(), "1".getBytes(), KEY_PREFIX.getBytes(),
                        "SCHEMA".getBytes(),
                        "question".getBytes(), "TEXT".getBytes(),
                        "answer".getBytes(), "TEXT".getBytes(),
                        "embedding".getBytes(), "VECTOR".getBytes(),
                        "HNSW".getBytes(), "6".getBytes(),
                        "TYPE".getBytes(), "FLOAT32".getBytes(),
                        "DIM".getBytes(), String.valueOf(vectorDimension).getBytes(),
                        "DISTANCE_METRIC".getBytes(), "COSINE".getBytes());
                return null;
            });

            System.out.println("Redis vector index created");

        } catch (Exception e) {
            System.out.println("Redis vector index already exists or creation skipped");
        }
    }

    public String findSimilarCachedAnswer(List<Double> questionVector) {

        byte[] vectorBytes = convertToFloat32Bytes(questionVector);

        Object result = redisTemplate.execute((RedisConnection connection) -> connection.execute(
                "FT.SEARCH",
                INDEX_NAME.getBytes(),
                "*=>[KNN 1 @embedding $vec AS score]".getBytes(),
                "PARAMS".getBytes(), "2".getBytes(),
                "vec".getBytes(), vectorBytes,
                "SORTBY".getBytes(), "score".getBytes(),
                "RETURN".getBytes(), "2".getBytes(),
                "answer".getBytes(), "score".getBytes(),
                "DIALECT".getBytes(), "2".getBytes()));

        return extractAnswerIfSimilar(result);
    }

    public void saveSemanticCache(String question,
            List<Double> questionVector,
            String answer) {

        String key = KEY_PREFIX + UUID.randomUUID();

        byte[] vectorBytes = convertToFloat32Bytes(questionVector);

        redisTemplate.execute((RedisConnection connection) -> {
            byte[] redisKey = key.getBytes();

            connection.hSet(redisKey, "question".getBytes(), question.getBytes());
            connection.hSet(redisKey, "answer".getBytes(), answer.getBytes());
            connection.hSet(redisKey, "embedding".getBytes(), vectorBytes);

            connection.expire(redisKey, Duration.ofDays(ttlDays).toSeconds());

            return null;
        });
    }

    public void clearRagCache() {
        var keys = redisTemplate.keys(KEY_PREFIX + "*");

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private byte[] convertToFloat32Bytes(List<Double> vector) {

        ByteBuffer buffer = ByteBuffer
                .allocate(vector.size() * Float.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);

        for (Double value : vector) {
            buffer.putFloat(value.floatValue());
        }

        return buffer.array();
    }

    private String extractAnswerIfSimilar(Object result) {

        if (!(result instanceof List<?> list) || list.size() < 3) {
            return null;
        }

        Object fieldsObject = list.get(2);

        if (!(fieldsObject instanceof List<?> fields)) {
            return null;
        }

        String answer = null;
        double distance = 1.0;

        for (int i = 0; i < fields.size() - 1; i += 2) {
            String field = new String((byte[]) fields.get(i));
            String value = new String((byte[]) fields.get(i + 1));

            if (field.equals("answer")) {
                answer = value;
            }

            if (field.equals("score")) {
                distance = Double.parseDouble(value);
            }
        }

        double similarity = 1 - distance;

        if (answer != null && similarity >= similarityThreshold) {
            System.out.println("Redis VECTOR HIT, similarity = " + similarity);
            return answer;
        }

        System.out.println("Redis VECTOR MISS, similarity = " + similarity);
        return null;
    }
}