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

    @Autowired
    private DistributedCacheService distributedCache; //  NUEVO

    //  ELIMINADO: public final Map<String, GameRoom> roomCache = new ConcurrentHashMap<>();

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
                });

        // Guardar en Redis
        distributedCache.saveRoom(roomCode, room);
    }

    // Obtener o cargar sala desde Redis o DB
    private GameRoom getOrLoadRoom(String roomCode) {
        // 1. Intentar desde Redis
        GameRoom cached = distributedCache.getRoom(roomCode);
        if (cached != null) {
            return cached;
        }

        // 2. Cargar desde DB
        GameRoom room = gameRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Sala no encontrada: " + roomCode));

        // 3. Guardar en Redis
        distributedCache.saveRoom(roomCode, room);

        return room;
    }

    // Loop global del juego (cada 50 ms)
    @Transactional
    @Scheduled(fixedRate = 50)
    public void updateAllRooms() {
        List<GameRoom> activeRooms = gameRoomRepository.findAll().stream()
                .filter(GameRoom::isGameStarted)
                .collect(Collectors.toList());

        activeRooms.forEach(dbRoom -> {
            String roomCode = dbRoom.getRoomCode();

            // Obtener desde Redis o cargar
            GameRoom room = getOrLoadRoom(roomCode);

            // Sincronizar propiedades desde DB
            room.setGameStarted(dbRoom.isGameStarted());
            room.setMap(dbRoom.getMap());

            if (!room.isGameStarted()) return;

            // 1. Actualizar movimiento
            for (Player player : room.getPlayers()) {
                if (player.isAlive()) {
                    player.actualizar();
                }
            }

            // 2. Persistir posiciones
            for (Player player : room.getPlayers()) {
                if (player.isAlive()) {
                    playerRepository.save(player);
                }
            }

            // 3. Revisar cofres
            for (Player player : room.getPlayers()) {
                if (player.isAlive()) {
                    checkChestInteraction(room, player);
                }
            }

            // 4. Ataques automáticos
            for (Player player : room.getPlayers()) {
                if (player.isAlive()) {
                    long now = System.currentTimeMillis();
                    if (now - player.getLastAttackTime() >= 1500) {
                        playerWhipAttack(roomCode, player.getPlayerName());
                    }
                }
            }

            // 5. Verificar game over
            checkGameOver(roomCode);

            // 6. Enviar estado
            broadcastGameState(roomCode);

            // 7. Guardar en Redis
            distributedCache.saveRoom(roomCode, room);
        });
    }

    private void broadcastGameState(String roomCode) {
        try {
            GameRoom room = getOrLoadRoom(roomCode);

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

            Map<String, Object> gameState = new HashMap<>();
            gameState.put("players", playersData);
            gameState.put("npcs", npcsData);
            gameState.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend("/topic/game/" + roomCode + "/state", gameState);

        } catch (Exception e) {
            System.err.println("Error broadcasting game state: " + e.getMessage());
        }
    }

    private String determineDirection(Player player) {
        return player.isFacingRight() ? "right" : "left";
    }

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

    private GameRoom getRoomByCode(String roomCode) {
        return gameRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Sala no encontrada: " + roomCode));
    }

    public void addExperience(String roomCode, int amount) {
        experienceService.addExperience(roomCode, amount);
    }

    public double getProgress(String roomCode) {
        return experienceService.getProgress(roomCode);
    }

    public List<Map<String, Object>> spawnPlayers(String roomCode) {
        GameRoom room = getRoomByCode(roomCode);
        List<Player> players = room.getPlayers();

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

        // Guardar en Redis
        distributedCache.saveRoom(roomCode, room);

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
        GameRoom room = getOrLoadRoom(roomCode);
        Player player = room.getPlayers().stream()
                .filter(p -> p.getPlayerName().equals(playerName))
                .findFirst()
                .orElse(null);

        if (player == null || !player.isAlive()) return;

        final double RANGE = 80.0;
        final double HEIGHT = 100.0;
        final int DAMAGE = 5;

        double px = player.getX();
        double py = player.getY();

        List<NPC> npcs = npcManager.getNpcsForRoom(roomCode);
        if (npcs == null || npcs.isEmpty()) return;

        for (NPC npc : npcs) {
            if (npc.isDead()) continue;

            double nx = npc.getX();
            double ny = npc.getY();

            boolean inRange;
            if (player.isFacingRight()) {
                inRange = nx >= px && nx <= (px + RANGE) &&
                        ny >= (py - HEIGHT / 2) && ny <= (py + HEIGHT / 2);
            } else {
                inRange = nx <= px && nx >= (px - RANGE) &&
                        ny >= (py - HEIGHT / 2) && ny <= (py + HEIGHT / 2);
            }

            if (inRange) {
                boolean killed = npc.receiveDamage(DAMAGE, player.getPlayerName());

                if (killed) {
                    Map<String, Object> event = new HashMap<>();
                    event.put("type", "NPC_KILLED");
                    event.put("npcId", npc.getId());
                    event.put("killedBy", playerName);
                    messagingTemplate.convertAndSend("/topic/game/" + roomCode + "/event", event);
                }
            }
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

    private void onGameLost(String roomCode) {
        GameRoom room = getOrLoadRoom(roomCode);

        for (Player p : room.getPlayers()) {
            if (p.getUserAccount() != null) {
                UserAccount account = p.getUserAccount();
                account.endSession();
                distributedCache.endSession(p.getPlayerName()); //  Redis
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

        Map<String, Object> event = new HashMap<>();
        event.put("type", "GAME_OVER");
        event.put("roomCode", roomCode);
        messagingTemplate.convertAndSend("/topic/game/" + roomCode + "/event", event);

        distributedCache.deleteRoom(roomCode); //  Redis

        try {
            gameRoomRepository.delete(room);
        } catch (Exception e) {
            System.err.println(" Error eliminando sala: " + e.getMessage());
        }
    }

    @Transactional
    public void onGameWon(String roomCode) {
        GameRoom room = getOrLoadRoom(roomCode);

        for (Player p : room.getPlayers()) {
            if (p.getUserAccount() != null) {
                UserAccount account = p.getUserAccount();
                account.endSession();
                distributedCache.endSession(p.getPlayerName()); //  Redis
            }

            p.setReady(false);
            p.setHealth(Player.DEFAULT_HEALTH);
            p.setX(0);
            p.setY(0);
            playerRepository.save(p);
        }

        room.setGameStarted(false);
        gameRoomRepository.save(room);
        experienceService.resetRoomXp(roomCode);

        Map<String, Object> event = new HashMap<>();
        event.put("type", "GAME_WON");
        event.put("roomCode", roomCode);
        messagingTemplate.convertAndSend("/topic/game/" + roomCode + "/event", event);

        distributedCache.deleteRoom(roomCode); //  Redis
    }

    private void checkGameOver(String roomCode) {
        GameRoom room = getOrLoadRoom(roomCode);

        boolean allDead = room.getPlayers().stream()
                .allMatch(p -> !p.isAlive());

        if (allDead) {
            onGameLost(roomCode);
        }
    }

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