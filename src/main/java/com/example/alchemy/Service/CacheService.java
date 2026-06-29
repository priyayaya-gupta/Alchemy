package com.example.alchemy.Service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.commands.ProtocolCommand;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CacheService {

    private JedisPool jedisPool;

    private static final String INDEX_NAME = "alchemy_semantic_idx";
    private static final String CACHE_PREFIX = "alchemy:semantic:";
    private static final String ADMISSION_PREFIX = "alchemy:admission:";

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${app.cache.ttl-days}")
    private long ttlDays;

    @Value("${app.cache.similarity-threshold}")
    private double similarityThreshold;

    @Value("${app.cache.vector-dimension}")
    private int vectorDimension;

    @Value("${app.cache.admission-threshold}")
    private int admissionThreshold;

    @PostConstruct
    public void init() {
        this.jedisPool = new JedisPool(redisHost, redisPort);
        createVectorIndex();
    }

    public void createVectorIndex() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.sendCommand(
                    RedisSearchCommand.FT_CREATE,
                    bytes(INDEX_NAME),
                    bytes("ON"), bytes("HASH"),
                    bytes("PREFIX"), bytes("1"), bytes(CACHE_PREFIX),
                    bytes("SCHEMA"),
                    bytes("question"), bytes("TEXT"),
                    bytes("answer"), bytes("TEXT"),
                    bytes("scope"), bytes("TAG"),
                    bytes("embedding"), bytes("VECTOR"),
                    bytes("HNSW"), bytes("6"),
                    bytes("TYPE"), bytes("FLOAT32"),
                    bytes("DIM"), bytes(String.valueOf(vectorDimension)),
                    bytes("DISTANCE_METRIC"), bytes("COSINE"));

            System.out.println("Redis vector index created");

        } catch (Exception e) {
            System.out.println("Redis vector index already exists or skipped");
        }
    }

    public String findSimilarCachedAnswer(List<Double> questionVector,
            List<String> documentIds,
            List<String> fileNames) {

        if (!isValidVector(questionVector)) {
            return null;
        }

        String scope = buildDocumentScope(documentIds, fileNames);

        try (Jedis jedis = jedisPool.getResource()) {
            Object result = jedis.sendCommand(
                    RedisSearchCommand.FT_SEARCH,
                    bytes(INDEX_NAME),
                    bytes("@scope:{" + escapeTag(scope) + "}=>[KNN 1 @embedding $vec AS score]"),
                    bytes("PARAMS"), bytes("2"),
                    bytes("vec"), convertToFloat32Bytes(questionVector),
                    bytes("SORTBY"), bytes("score"),
                    bytes("RETURN"), bytes("2"),
                    bytes("answer"), bytes("score"),
                    bytes("DIALECT"), bytes("2"));

            return extractAnswerIfSimilar(result);

        } catch (Exception e) {
            System.out.println("Redis vector cache skipped: " + e.getMessage());
            return null;
        }
    }

    public boolean shouldCacheNow(String question,
            List<String> documentIds,
            List<String> fileNames) {

        String scope = buildDocumentScope(documentIds, fileNames);
        String key = ADMISSION_PREFIX + normalize(question) + ":" + scope;

        try (Jedis jedis = jedisPool.getResource()) {
            long count = jedis.incr(key);
            jedis.expire(key, ttlDays * 24 * 60 * 60);

            System.out.println("Admission count for question = " + count);

            return count >= admissionThreshold;

        } catch (Exception e) {
            System.out.println("Admission check failed: " + e.getMessage());
            return true;
        }
    }

    public void saveSemanticCache(String question,
            List<Double> questionVector,
            String answer,
            List<String> documentIds,
            List<String> fileNames) {

        if (!isValidVector(questionVector)) {
            System.out.println("Cache not saved: invalid vector");
            return;
        }

        String scope = buildDocumentScope(documentIds, fileNames);
        String key = CACHE_PREFIX + UUID.randomUUID();

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hset(bytes(key), bytes("question"), bytes(question));
            jedis.hset(bytes(key), bytes("answer"), bytes(answer));
            jedis.hset(bytes(key), bytes("scope"), bytes(scope));
            jedis.hset(bytes(key), bytes("embedding"), convertToFloat32Bytes(questionVector));
            jedis.expire(bytes(key), ttlDays * 24 * 60 * 60);

            System.out.println("Saved answer in Redis semantic cache");

        } catch (Exception e) {
            System.out.println("Could not save semantic cache: " + e.getMessage());
        }
    }

    public void clearRagCache() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> cacheKeys = jedis.keys(CACHE_PREFIX + "*");
            Set<String> admissionKeys = jedis.keys(ADMISSION_PREFIX + "*");

            if (!cacheKeys.isEmpty()) {
                jedis.del(cacheKeys.toArray(new String[0]));
            }

            if (!admissionKeys.isEmpty()) {
                jedis.del(admissionKeys.toArray(new String[0]));
            }

            System.out.println("Redis cache and admission counters cleared");

        } catch (Exception e) {
            System.out.println("Could not clear Redis cache: " + e.getMessage());
        }
    }

    private boolean isValidVector(List<Double> vector) {
        return vector != null && vector.size() == vectorDimension;
    }

    private String buildDocumentScope(List<String> documentIds, List<String> fileNames) {
        if (documentIds != null && !documentIds.isEmpty()) {
            return "docs:" + documentIds.stream().sorted().toList();
        }

        if (fileNames != null && !fileNames.isEmpty()) {
            return "files:" + fileNames.stream().sorted().toList();
        }

        return "all-documents";
    }

    private String normalize(String question) {
        return question
                .trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", " ");
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
            String field = toStringValue(fields.get(i));
            String value = toStringValue(fields.get(i + 1));

            if ("answer".equals(field)) {
                answer = value;
            }

            if ("score".equals(field)) {
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

    private String toStringValue(Object value) {
        if (value instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }

        return String.valueOf(value);
    }

    private String escapeTag(String value) {
        return value
                .replace("\\", "\\\\")
                .replace(" ", "\\ ")
                .replace(",", "\\,")
                .replace(".", "\\.")
                .replace("-", "\\-")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace(":", "\\:");
    }

    private byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private enum RedisSearchCommand implements ProtocolCommand {
        FT_CREATE("FT.CREATE"),
        FT_SEARCH("FT.SEARCH");

        private final byte[] raw;

        RedisSearchCommand(String command) {
            this.raw = command.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public byte[] getRaw() {
            return raw;
        }
    }
}