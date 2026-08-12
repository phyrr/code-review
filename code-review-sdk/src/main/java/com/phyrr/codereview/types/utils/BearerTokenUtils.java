package com.phyrr.codereview.types.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class BearerTokenUtils {

    private static final long EXPIRE_MILLIS = 30 * 60 * 1000L;

    private static final Cache<String, String> CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(EXPIRE_MILLIS - 60_000L, TimeUnit.MILLISECONDS)
            .build();

    private BearerTokenUtils() {
    }

    public static String getToken(String apiKeySecret) {
        String[] split = apiKeySecret.split("\\.");
        if (split.length != 2) {
            throw new IllegalArgumentException("CHATGLM_APIKEYSECRET must be in format: apiKey.apiSecret");
        }
        return getToken(split[0], split[1]);
    }

    public static String getToken(String apiKey, String apiSecret) {
        String cached = CACHE.getIfPresent(apiKey);
        if (cached != null) {
            return cached;
        }

        Algorithm algorithm = Algorithm.HMAC256(apiSecret.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> payload = new HashMap<>();
        payload.put("api_key", apiKey);
        payload.put("exp", System.currentTimeMillis() + EXPIRE_MILLIS);
        payload.put("timestamp", Calendar.getInstance().getTimeInMillis());

        Map<String, Object> headerClaims = new HashMap<>();
        headerClaims.put("alg", "HS256");
        headerClaims.put("sign_type", "SIGN");

        String token = JWT.create().withPayload(payload).withHeader(headerClaims).sign(algorithm);
        CACHE.put(apiKey, token);
        return token;
    }
}
