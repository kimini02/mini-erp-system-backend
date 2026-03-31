package com.minierp.backend.global.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class PasswordResetStoreService {

    private static final Duration VERIFICATION_CODE_TTL = Duration.ofMinutes(5);
    private static final Duration RESET_PROOF_TTL = Duration.ofMinutes(15);
    private static final Duration REQUEST_RATE_LIMIT_TTL = Duration.ofMinutes(1);
    private static final long MAX_REQUESTS_PER_MINUTE = 5;

    private final StringRedisTemplate redisTemplate;

    public long incrementRequestCount(String email) {
        String key = requestCountKey(email);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, REQUEST_RATE_LIMIT_TTL);
        }
        return count == null ? 0L : count;
    }

    public void saveVerificationCode(String email, String code) {
        redisTemplate.opsForValue().set(codeKey(email), code, VERIFICATION_CODE_TTL);
    }

    public String getVerificationCode(String email) {
        return redisTemplate.opsForValue().get(codeKey(email));
    }

    public void removeVerificationCode(String email) {
        redisTemplate.delete(codeKey(email));
    }

    public void saveResetProof(String proof, String email) {
        redisTemplate.opsForValue().set(resetProofKey(proof), email, RESET_PROOF_TTL);
    }

    public String getEmailByResetProof(String proof) {
        return redisTemplate.opsForValue().get(resetProofKey(proof));
    }

    public void removeResetProof(String proof) {
        redisTemplate.delete(resetProofKey(proof));
    }

    public long getMaxRequestsPerMinute() {
        return MAX_REQUESTS_PER_MINUTE;
    }

    private String codeKey(String email) {
        return "pwdreset:code:" + email;
    }

    private String resetProofKey(String proof) {
        return "pwdreset:proof:" + proof;
    }

    private String requestCountKey(String email) {
        return "pwdreset:req:" + email;
    }
}

