package com.dinosurio_G.Back.service;

import com.dinosurio_G.Back.model.GameRoom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Servicio para gestionar caché distribuida con Redis
 * Reemplaza el ConcurrentHashMap local por almacenamiento compartido
 */
@Service
public class DistributedCacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String ROOM_CACHE_PREFIX = "game:room:";
    private static final String USER_SESSION_PREFIX = "user:session:";
    private static final String XP_CACHE_PREFIX = "game:xp:";

    // ===== SALAS =====

    public void saveRoom(String roomCode, GameRoom room) {
        String key = ROOM_CACHE_PREFIX + roomCode;
        redisTemplate.opsForValue().set(key, room, 2, TimeUnit.HOURS);
        System.out.println(" Sala " + roomCode + " guardada en Redis");
    }

    public GameRoom getRoom(String roomCode) {
        String key = ROOM_CACHE_PREFIX + roomCode;
        Object value = redisTemplate.opsForValue().get(key);

        if (value instanceof GameRoom) {
            return (GameRoom) value;
        }
        return null;
    }

    public void deleteRoom(String roomCode) {
        String key = ROOM_CACHE_PREFIX + roomCode;
        redisTemplate.delete(key);
        System.out.println(" Sala " + roomCode + " eliminada de Redis");
    }

    // ===== SESIONES DE USUARIO =====

    public boolean hasActiveSession(String playerName) {
        String key = USER_SESSION_PREFIX + playerName;
        Boolean exists = redisTemplate.hasKey(key);
        return exists != null && exists;
    }

    public void startSession(String playerName) {
        String key = USER_SESSION_PREFIX + playerName;
        redisTemplate.opsForValue().set(key, System.currentTimeMillis(), 4, TimeUnit.HOURS);
        System.out.println(" Sesión Redis iniciada para " + playerName);
    }

    public void endSession(String playerName) {
        String key = USER_SESSION_PREFIX + playerName;
        redisTemplate.delete(key);
        System.out.println(" Sesión Redis terminada para " + playerName);
    }

    // ===== EXPERIENCIA =====

    public void saveXp(String roomCode, int xp) {
        String key = XP_CACHE_PREFIX + roomCode;
        redisTemplate.opsForValue().set(key, xp, 2, TimeUnit.HOURS);
    }

    public Integer getXp(String roomCode) {
        String key = XP_CACHE_PREFIX + roomCode;
        Object value = redisTemplate.opsForValue().get(key);

        if (value instanceof Integer) {
            return (Integer) value;
        }
        return null;
    }

    public void deleteXp(String roomCode) {
        String key = XP_CACHE_PREFIX + roomCode;
        redisTemplate.delete(key);
    }

    // ===== HEALTH CHECK =====

    public boolean isRedisConnected() {
        try {
            redisTemplate.opsForValue().set("health:check", "ok", 10, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            System.err.println(" Redis no está conectado: " + e.getMessage());
            return false;
        }
    }
}