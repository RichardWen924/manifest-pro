package com.manifestreader.user.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manifestreader.user.model.vo.BillExtractTaskVO;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RedisBillParseTaskCacheService implements BillParseTaskCacheService {

    private static final Duration TASK_TTL = Duration.ofHours(6);
    private static final Duration FILE_HASH_TTL = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisBillParseTaskCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<String> findTaskNoByFileHash(Long companyId, String fileHash) {
        String value = redisTemplate.opsForValue().get(fileHashKey(companyId, fileHash));
        return StringUtils.hasText(value) ? Optional.of(value) : Optional.empty();
    }

    @Override
    public void bindFileHash(Long companyId, String fileHash, String taskNo) {
        redisTemplate.opsForValue().set(fileHashKey(companyId, fileHash), taskNo, FILE_HASH_TTL);
    }

    @Override
    public void cacheTask(BillExtractTaskVO task) {
        try {
            redisTemplate.opsForValue().set(taskKey(task.taskNo()), objectMapper.writeValueAsString(task), TASK_TTL);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Serialize bill parse task cache failed", ex);
        }
    }

    @Override
    public Optional<BillExtractTaskVO> getTask(String taskNo) {
        String value = redisTemplate.opsForValue().get(taskKey(taskNo));
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value, BillExtractTaskVO.class));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Deserialize bill parse task cache failed", ex);
        }
    }

    private String taskKey(String taskNo) {
        return "manifest:bill:task:" + taskNo;
    }

    private String fileHashKey(Long companyId, String fileHash) {
        return "manifest:bill:file-hash:" + companyId + ":" + fileHash;
    }
}
