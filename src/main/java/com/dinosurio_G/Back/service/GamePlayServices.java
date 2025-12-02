package com.dinosurio_G.Back.service;

import com.dinosurio_G.Back.dto.PlayerHealthDTO;
import com.dinosurio_G.Back.model.*;
import com.dinosurio_G.Back.repository.GameRoomRepository;
import com.dinosurio_G.Back.repository.PlayerRepository;
import com.dinosurio_G.Back.service.core.LockManager;
import com.dinosurio_G.Back.service.impl.ChestService;
import com.dinosurio_G.Back.service.impl.GameMapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class GamePlayServices {
    private static final int CHEST_REWARD_XP = 150;
    private static final double CHEST_INTERACT_RADIUS = 50.0;

    @Autowired
    private GameRoomRepository gameRoomRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private GameMapService gameMapService;

    @Autowired
    private ChestService chestService;

    @Autowired
    private NPCManager npcManager;

    @Autowired
    private ExperienceService experienceService;

    @Autowired
    private LockManager lockManager;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Caché de salas en memoria para mantener el estado de inputs
    private final Map<String, GameRoom> roomCache = new ConcurrentHashMap<>();

    // Actualizar input del jugador (desde frontend)
    public void updatePlayerInput(String roomCode, String playerName,
                                  boolean arriba, boolean abajo,
                                  boolean izquierda, boolean derecha) {
        GameRoom room = getOrLoadRoom(roomCode);
        room.getPlayers().stream()
                .filter(p -> p.getPlayerName().equals(playerName))
                .findFirst()
                .ifPresent(player -> {
                    player.setInput(arriba, abajo, izquierda, derecha);
                    System.out.println("✓ Input actualizado para " + playerName + ": " +
                            (arriba?"↑":"") + (abajo?"↓":"") +
                            (izquierda?"←":"") + (derecha?"→":""));
                });
    }

    // Obtener o cargar sala en caché
    private GameRoom getOrLoadRoom(String roomCode) {
        return roomCache.computeIfAbsent(roomCode, code -> {
            GameRoom room = gameRoomRepository.findByRoomCode(code)
                    .orElseThrow(() -> new RuntimeException("La sala con código " + code + " no existe"));
            System.out.println("✓ Sala " + roomCode + " cargada en memoria");
            return room;
        });
    }

    // Loop global del juego (cada 50 ms) - CORREGIDO
    @Transactional
    @Scheduled(fixedRate = 50)
    public void updateAllRooms() {
        // Recargar salas activas desde DB
        List<GameRoom> activeRooms = gameRoomRepository.findAll().stream()
                .filter(GameRoom::isGameStarted)
                .collect(Collectors.toList());

        // Actualizar caché con salas activas
        activeRooms.forEach(room -> {
            String roomCode = room.getRoomCode();
            GameRoom cachedRoom = roomCache.get(roomCode);

            if (cachedRoom != null) {
                // Mantener inputs de la caché, actualizar resto desde DB
                syncRoomFromDb(cachedRoom, room);
            } else {
                roomCache.put(roomCode, room);
            }
        });

        // Procesar cada sala en caché
        roomCache.values().forEach(room -> {
            if (!room.isGameStarted()) return;

            // 1. Actualizar movimiento (usa inputs en memoria)
            for (Player player : room.getPlayers()) {
                if (player.isAlive()) {
                    player.actualizar();
                }
            }

            // 2. Persistir posiciones actualizadas
            for (Player player : room.getPlayers()) {
                if (player.isAlive()) {
                    playerRepository.save(player);
                }
            }

            // 3. Revisar interacción con cofres
            for (Player player : room.getPlayers()) {
                if (player.isAlive()) {
                    checkChestInteraction(room, player);
                }
            }

            // 4. Ejecutar ataques automáticos por jugador
            for (Player player : room.getPlayers()) {
                if (player.isAlive()) {
                    long now = System.currentTimeMillis();
                    if (now - player.getLastAttackTime() >= 1500) {
                        playerWhipAttack(room.getRoomCode(), player.getPlayerName());
                    }
                }
            }

            // 5. Verificar si la partida se ha perdido
            checkGameOver(room.getRoomCode());

            // 6. Enviar estado actualizado al frontend
            broadcastGameState(room.getRoomCode());
        });

        // Limpiar salas inactivas del caché
        roomCache.entrySet().removeIf(entry -> !entry.getValue().isGameStarted());
    }

    // Sincronizar datos de DB manteniendo inputs
    private void syncRoomFromDb(GameRoom cachedRoom, GameRoom dbRoom) {
        // Actualizar propiedades de la sala
        cachedRoom.setGameStarted(dbRoom.isGameStarted());
        cachedRoom.setMap(dbRoom.getMap());

        // Sincronizar jugadores manteniendo inputs
        for (Player dbPlayer : dbRoom.getPlayers()) {
            cachedRoom.getPlayers().stream()
                    .filter(p -> p.getId().equals(dbPlayer.getId()))
                    .findFirst()
                    .ifPresent(cachedPlayer -> {
                        // Actualizar propiedades persistidas
                        cachedPlayer.setHealth(dbPlayer.getHealth());
                        // NO actualizar x, y ni inputs (se mantienen en memoria)
                    });
        }
    }

    // Enviar estado del juego al frontend vía WebSocket
    private void broadcastGameState(String roomCode) {
        try {
            GameRoom room = getOrLoadRoom(roomCode);

            // Preparar datos de jugadores
            List<Map<String, Object>> playersData = room.getPlayers().stream()
                    .map(p -> {
                        Map<String, Object> playerData = new HashMap<>();
                        playerData.put("playerName", p.getPlayerName());
                        playerData.put("x", p.getX());
                        playerData.put("y", p.getY());
                        playerData.put("health", p.getHealth());
                        playerData.put("maxHealth", Player.DEFAULT_HEALTH);
                        playerData.put("alive", p.isAlive());
                        playerData.put("direction", determineDirection(p));
                        return playerData;
                    })
                    .collect(Collectors.toList());

            // Preparar datos de NPCs
            List<NPC> npcs = npcManager.getNpcsForRoom(roomCode);
            List<Map<String, Object>> npcsData = npcs.stream()
                    .filter(npc -> !npc.isDead())
                    .map(npc -> {
                        Map<String, Object> npcData = new HashMap<>();
                        npcData.put("id", npc.getId());
                        npcData.put("x", npc.getX());
                        npcData.put("y", npc.getY());
                        npcData.put("health", npc.getHealth());
                        return npcData;
                    })
                    .collect(Collectors.toList());

            // Crear payload
            Map<String, Object> gameState = new HashMap<>();
            gameState.put("players", playersData);
            gameState.put("npcs", npcsData);
            gameState.put("timestamp", System.currentTimeMillis());

            // Enviar por WebSocket
            messagingTemplate.convertAndSend("/topic/game/" + roomCode + "/state", gameState);

        } catch (Exception e) {
            System.err.println("Error broadcasting game state: " + e.getMessage());
        }
    }

    // Determinar dirección del jugador
    private String determineDirection(Player player) {
        return player.isFacingRight() ? "right" : "left";
    }

    // Obtener vida actual de los jugadores
    public List<PlayerHealthDTO> getPlayersHealth(String roomCode) {
        GameRoom room = getOrLoadRoom(roomCode);
        return room.getPlayers().stream()
                .map(p -> new PlayerHealthDTO(
                        p.getPlayerName(),
                        p.getHealth(),
                        Player.DEFAULT_HEALTH,
                        p.isAlive()
                ))
                .collect(Collectors.toList());
    }

    // Utilidad interna
    private GameRoom getRoomByCode(String roomCode) {
        return gameRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RuntimeException("La sala con código " + roomCode + " no existe"));
    }

    // Sumar XP al room
    public void addExperience(String roomCode, int amount) {
        experienceService.addExperience(roomCode, amount);
    }

    public double getProgress(String roomCode) {
        return experienceService.getProgress(roomCode);
    }

    // Spawn de jugadores + NPCs
    public List<Map<String, Object>> spawnPlayers(String roomCode) {
        GameRoom room = getRoomByCode(roomCode);
        List<Player> players = room.getPlayers();

        // Verificar si ya fueron spawneados
        boolean alreadySpawned = players.stream()
                .anyMatch(p -> p.getX() != 0 || p.getY() != 0);

        if (alreadySpawned) {
            System.out.println("⚠ Jugadores ya spawneados en sala " + roomCode);
            return players.stream()
                    .map(p -> {
                        Map<String, Object> playerData = new HashMap<>();
                        playerData.put("name", p.getPlayerName());
                        playerData.put("x", p.getX());
                        playerData.put("y", p.getY());
                        playerData.put("health", p.getHealth());
                        return playerData;
                    })
                    .collect(Collectors.toList());
        }

        double startX = 100;
        double startY = 100;
        double offset = 80;

        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (player.getGameRoom() == null) {
                player.setGameRoom(room);
            }
            player.setX(startX + (i * offset));
            player.setY(startY);
            player.setHealth(Player.DEFAULT_HEALTH);
            playerRepository.save(player);
            System.out.println("✓ Spawneado: " + player.getPlayerName() +
                    " en (" + (int)player.getX() + ", " + (int)player.getY() + ")");
        }

        // Cargar sala en caché después del spawn
        roomCache.put(roomCode, room);

        // Spawnear NPCs después
        npcManager.spawnInitialNpcs(roomCode);
        gameRoomRepository.save(room);

        return players.stream()
                .map(p -> {
                    Map<String, Object> playerData = new HashMap<>();
                    playerData.put("name", p.getPlayerName());
                    playerData.put("x", p.getX());
                    playerData.put("y", p.getY());
                    playerData.put("health", p.getHealth());
                    return playerData;
                })
                .collect(Collectors.toList());
    }

    public GameRoom getRoomInMemory(String roomCode) {
        return getOrLoadRoom(roomCode);
    }

    public synchronized void playerWhipAttack(String roomCode, String playerName) {
        System.out.println(" Ejecutando ataque de " + playerName + " en sala " + roomCode);
        GameRoom room = getOrLoadRoom(roomCode);
        Player player = room.getPlayers().stream()
                .filter(p -> p.getPlayerName().equals(playerName))
                .findFirst()
                .orElse(null);

        if (player == null || !player.isAlive()) {
            System.out.println(" Jugador no válido para atacar");
            return;
        }

        final double RANGE = 80.0;
        final double HEIGHT = 100.0;
        final int DAMAGE = 5;

        double px = player.getX();
        double py = player.getY();

        List<NPC> npcs = npcManager.getNpcsForRoom(roomCode);

        if (npcs == null || npcs.isEmpty()) {
            System.out.println(" No hay NPCs en la sala " + roomCode);
            return;
        }

        boolean hitSomething = false;
        int npcsHit = 0;

        for (NPC npc : npcs) {
            if (npc.isDead()) continue;

            double nx = npc.getX();
            double ny = npc.getY();

            // Calcular hitbox según dirección
            boolean inRange;
            if (player.isFacingRight()) {
                // Atacando hacia la derecha: de px hasta px+RANGE
                inRange = nx >= px && nx <= (px + RANGE) &&
                        ny >= (py - HEIGHT / 2) && ny <= (py + HEIGHT / 2);
            } else {
                // Atacando hacia la izquierda: de px-RANGE hasta px
                inRange = nx <= px && nx >= (px - RANGE) &&
                        ny >= (py - HEIGHT / 2) && ny <= (py + HEIGHT / 2);
            }

            if (inRange) {
                hitSomething = true;
                npcsHit++;

                boolean killed = npc.receiveDamage(DAMAGE, player.getPlayerName());

                if (killed) {
                    System.out.println(" " + playerName + " MATÓ un NPC #" + npc.getId());

                    Map<String, Object> event = new HashMap<>();
                    event.put("type", "NPC_KILLED");
                    event.put("npcId", npc.getId());
                    event.put("killedBy", playerName);
                    messagingTemplate.convertAndSend("/topic/game/" + roomCode + "/event", event);
                } else {
                    System.out.println(" " + playerName + " golpeó NPC #" + npc.getId() +
                            " (HP: " + npc.getHealth() + "/" + DAMAGE + " daño)");
                }
            }
        }

        if (hitSomething) {
            System.out.println(" " + playerName + " impactó " + npcsHit + " NPC(s)");
        } else {
            System.out.println(" " + playerName + " no golpeó nada (Dir: " +
                    (player.isFacingRight() ? "→" : "←") +
                    ", Pos: " + (int)px + "," + (int)py + ")");
        }
    }

    private void checkChestInteraction(GameRoom room, Player player) {
        if (!player.isAlive()) return;

        GameMap map = room.getMap();
        if (map == null) return;

        List<Chest> chests = chestService.findByMapId(map.getId());
        for (Chest chest : chests) {
            if (!chest.isActive()) continue;

            double dx = chest.getPosition().getX() - player.getX();
            double dy = chest.getPosition().getY() - player.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance <= CHEST_INTERACT_RADIUS) {
                lockManager.withLock("CHEST_" + chest.getId(), () -> {
                    if (!chest.isActive()) return;
                    boolean opened = chestService.tryOpenChest(chest.getId());
                    if (opened) {
                        addExperience(room.getRoomCode(), CHEST_REWARD_XP);
                        System.out.println("📦 " + player.getPlayerName() + " abrió un cofre!");

                        Map<String, Object> event = new HashMap<>();
                        event.put("type", "CHEST_OPENED");
                        event.put("chestId", chest.getId());
                        event.put("openedBy", player.getPlayerName());
                        messagingTemplate.convertAndSend("/topic/game/" + room.getRoomCode() + "/event", event);
                    }
                });
            }
        }
    }

    private void checkGameOver(String roomCode) {
        GameRoom room = getOrLoadRoom(roomCode);

        boolean allDead = room.getPlayers().stream()
                .allMatch(p -> !p.isAlive());

        if (allDead) {
            onGameLost(roomCode);
        }
    }

    private void onGameLost(String roomCode) {
        GameRoom room = getOrLoadRoom(roomCode);

        // Limpiar jugadores y desactivar sesiones
        for (Player p : room.getPlayers()) {
            // Desactivar sesión del UserAccount
            if (p.getUserAccount() != null) {
                p.getUserAccount().endSession();
                playerRepository.save(p);  // Esto también guarda el UserAccount en cascada
                System.out.println(" Sesión desactivada para " + p.getPlayerName());
            }

            p.setReady(false);
            p.setHealth(Player.DEFAULT_HEALTH);
            p.setX(0);
            p.setY(0);
            p.setHost(false);
            p.setGameRoom(null);
            playerRepository.save(p);
        }

        room.getPlayers().clear();
        experienceService.resetRoomXp(roomCode);

        // Notificar Game Over
        Map<String, Object> event = new HashMap<>();
        event.put("type", "GAME_OVER");
        event.put("roomCode", roomCode);
        messagingTemplate.convertAndSend("/topic/game/" + roomCode + "/event", event);

        System.out.println(" GAME OVER en sala " + roomCode + " - Jugadores desvinculados");

        // Limpiar sala del caché
        roomCache.remove(roomCode);

        // Eliminar la sala de la base de datos
        try {
            gameRoomRepository.delete(room);
            System.out.println("️ Sala " + roomCode + " eliminada de la base de datos");
        } catch (Exception e) {
            System.err.println("Error eliminando sala: " + e.getMessage());
        }
    }

    /**
     * Obtiene los cofres activos de una sala (para el frontend)
     */
    public List<Map<String, Object>> getChestsForRoom(String roomCode) {
        GameRoom room = getRoomByCode(roomCode);
        if (room.getMap() == null) {
            return Collections.emptyList();
        }

        return chestService.findByMapId(room.getMap().getId()).stream()
                .map(chest -> {
                    Map<String, Object> chestData = new HashMap<>();
                    chestData.put("id", chest.getId());
                    chestData.put("x", chest.getPosition().getX());
                    chestData.put("y", chest.getPosition().getY());
                    chestData.put("active", chest.isActive());
                    return chestData;
                })
                .collect(Collectors.toList());
    }
}