package com.nowhere.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

public class JsonRedisSerializer implements RedisSerializer<Object> {

    private final ObjectMapper objectMapper;

    public JsonRedisSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] serialize(Object value) throws SerializationException {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (Exception e) {
            throw new SerializationException("JSON serialization failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null) return null;
        try {
            return objectMapper.readValue(bytes, Object.class);
        } catch (Exception e) {
            throw new SerializationException("JSON deserialization failed: " + e.getMessage(), e);
        }
    }
}
