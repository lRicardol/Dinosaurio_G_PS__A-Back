package com.dinosurio_G.Back.service;

import com.dinosurio_G.Back.model.Chest;
import com.dinosurio_G.Back.model.GameMap;
import com.dinosurio_G.Back.model.GameRoom;
import com.dinosurio_G.Back.model.Position;
import com.dinosurio_G.Back.repository.GameRoomRepository;
import com.dinosurio_G.Back.service.impl.ChestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio que maneja el spawn periódico de cofres en las salas activas
 */
@Service
public class ChestSpawner {

    private static final int SPAWN_INTERVAL_MS = 15000; // Cada 15 segundos
    private static final int MAX_CHESTS_PER_ROOM = 5; // Máximo de cofres activos
    private static final int MAP_WIDTH = 800;
    private static final int MAP_HEIGHT = 600;
    private static final int MARGIN = 100; // Margen desde los bordes

    @Autowired
    private GameRoomRepository gameRoomRepository;

    @Autowired
    private ChestService chestService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Track de cofres spawneados por sala
    private final Map<String, List<Long>> chestsByRoom = new ConcurrentHashMap<>();
    private final Random random = new Random();

    /**
     * Ejecuta el spawn de cofres cada cierto tiempo
     */
    @Transactional
    @Scheduled(fixedRate = SPAWN_INTERVAL_MS)
    public void spawnChestsInActiveRooms() {
        List<GameRoom> activeRooms = gameRoomRepository.findAll().stream()
                .filter(GameRoom::isGameStarted)
                .toList();

        for (GameRoom room : activeRooms) {
            trySpawnChest(room);
        }
    }

    /**
     * Intenta spawnear un cofre si hay espacio disponible
     */
    private void trySpawnChest(GameRoom room) {
        String roomCode = room.getRoomCode();
        GameMap map = room.getMap();

        if (map == null) {
            return;
        }

        // Contar cofres activos en la sala
        List<Chest> activeChests = chestService.findByMapId(map.getId()).stream()
                .filter(Chest::isActive)
                .toList();

        if (activeChests.size() >= MAX_CHESTS_PER_ROOM) {
            return; // Ya hay suficientes cofres
        }

        // Generar posición aleatoria
        Position position = generateRandomPosition();

        // Crear el cofre
        Chest newChest = new Chest("experiencia", position);
        newChest.setType("exp");
        newChest.setMap(map);
        newChest.setActive(true);

        // Guardar en BD
        Chest savedChest = chestService.save(newChest);

        // Registrar en tracking
        chestsByRoom.computeIfAbsent(roomCode, k -> new ArrayList<>()).add(savedChest.getId());

        System.out.println("📦 COFRE SPAWNEADO en sala " + roomCode +
                " en (" + (int)position.getX() + ", " + (int)position.getY() + ")");

        // Notificar al frontend por WebSocket
        notifyChestSpawned(roomCode, savedChest);
    }

    /**
     * Genera una posición aleatoria dentro del mapa
     */
    private Position generateRandomPosition() {
        double x = MARGIN + random.nextDouble() * (MAP_WIDTH - 2 * MARGIN);
        double y = MARGIN + random.nextDouble() * (MAP_HEIGHT - 2 * MARGIN);
        return new Position(x, y);
    }

    /**
     * Notifica al frontend que apareció un nuevo cofre
     */
    private void notifyChestSpawned(String roomCode, Chest chest) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "CHEST_SPAWNED");
        event.put("chestId", chest.getId());
        event.put("x", chest.getPosition().getX());
        event.put("y", chest.getPosition().getY());

        messagingTemplate.convertAndSend("/topic/game/" + roomCode + "/event", event);
    }

    /**
     * Limpia cofres inactivos antiguos (opcional, para liberar memoria)
     */
    @Transactional
    @Scheduled(fixedRate = 60000) // Cada minuto
    public void cleanupOldChests() {
        List<GameRoom> activeRooms = gameRoomRepository.findAll().stream()
                .filter(GameRoom::isGameStarted)
                .toList();

        for (GameRoom room : activeRooms) {
            if (room.getMap() == null) continue;

            List<Chest> inactiveChests = chestService.findByMapId(room.getMap().getId()).stream()
                    .filter(chest -> !chest.isActive())
                    .toList();

            // Eliminar cofres inactivos después de cierto tiempo
            for (Chest chest : inactiveChests) {
                if (chest.getGeneratedAt() != null &&
                        java.time.Duration.between(chest.getGeneratedAt(),
                                java.time.LocalDateTime.now()).toMinutes() > 5) {
                    chestService.deleteById(chest.getId());
                    System.out.println("🗑️ Cofre antiguo " + chest.getId() + " eliminado");
                }
            }
        }
    }

    /**
     * Obtiene todos los cofres activos de una sala (para el frontend)
     */
    public List<Chest> getActiveChests(String roomCode) {
        GameRoom room = gameRoomRepository.findByRoomCode(roomCode).orElse(null);
        if (room == null || room.getMap() == null) {
            return Collections.emptyList();
        }

        return chestService.findByMapId(room.getMap().getId()).stream()
                .filter(Chest::isActive)
                .toList();
    }

    /**
     * Limpia el tracking de una sala (llamar cuando termine el juego)
     */
    public void clearRoomChests(String roomCode) {
        chestsByRoom.remove(roomCode);
    }
}
