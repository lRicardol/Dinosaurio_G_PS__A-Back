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

    // Actualizar input del jugador (desde frontend)
    public void updatePlayerInput(String roomCode, String playerName,
                                  boolean arriba, boolean abajo,
                                  boolean izquierda, boolean derecha) {
        GameRoom room = getRoomByCode(roomCode);
        room.getPlayers().stream()
                .filter(p -> p.getPlayerName().equals(playerName))
                .findFirst()
                .ifPresent(player -> {
                    player.setInput(arriba, abajo, izquierda, derecha);
                    // Log para debug
                    // System.out.println("Input recibido de " + playerName + ": " +
                    //                  (arriba?"↑":"") + (abajo?"↓":"") +
                    //                  (izquierda?"←":"") + (derecha?"→":""));
                });
    }

    // Loop global del juego (cada 50 ms) - ACTUALIZADO
    @Transactional
    @Scheduled(fixedRate = 50)
    public void updateAllRooms() {
        gameRoomRepository.findAll().forEach(room -> {
            if (!room.isGameStarted()) return;

            // 1. Actualizar movimiento y persistir posiciones
            for (Player player : room.getPlayers()) {
                if (player.isAlive()) {
                    player.actualizar();
                    playerRepository.save(player);
                }
            }

            // 2. Revisar interacción con cofres
            for (Player player : room.getPlayers()) {
                if (player.isAlive()) {
                    checkChestInteraction(room, player);
                }
            }

            // 3. Ejecutar ataques automáticos por jugador
            for (Player player : room.getPlayers()) {
                if (player.isAlive() && player.canAttack()) {
                    playerWhipAttack(room.getRoomCode(), player.getPlayerName());
                }
            }

            // 4. Verificar si la partida se ha perdido
            checkGameOver(room.getRoomCode());

            // 5. ENVIAR ESTADO ACTUALIZADO AL FRONTEND
            broadcastGameState(room.getRoomCode());
        });
    }

    // Enviar estado del juego al frontend vía WebSocket
    private void broadcastGameState(String roomCode) {
        try {
            GameRoom room = getRoomByCode(roomCode);

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
            System.err.println(" Error broadcasting game state: " + e.getMessage());
        }
    }

    // Determinar dirección del jugador
    private String determineDirection(Player player) {
        if (player.isFacingRight()) {
            return "right";
        } else {
            return "left";
        }
    }

    // Obtener vida actual de los jugadores
    public List<PlayerHealthDTO> getPlayersHealth(String roomCode) {
        GameRoom room = getRoomByCode(roomCode);
        return room.getPlayers().stream()
                .map(p -> new PlayerHealthDTO(
                        p.getPlayerName(),
                        p.getHealth(),
                        Player.DEFAULT_HEALTH, // maxHealth
                        p.isAlive()            // alive
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

    // Spawn de jugadores + NPCs - CON VERIFICACIÓN
    public List<Map<String, Object>> spawnPlayers(String roomCode) {
        GameRoom room = getRoomByCode(roomCode);
        List<Player> players = room.getPlayers();

        // VERIFICAR SI YA FUERON SPAWNEADOS
        boolean alreadySpawned = players.stream()
                .anyMatch(p -> p.getX() != 0 || p.getY() != 0);

        if (alreadySpawned) {
            System.out.println(" Jugadores ya spawneados en sala " + roomCode);
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
            System.out.println(" Spawneado: " + player.getPlayerName() +
                    " en (" + (int)player.getX() + ", " + (int)player.getY() + ")");
        }

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
        return getRoomByCode(roomCode);
    }

    public synchronized void playerWhipAttack(String roomCode, String playerName) {
        GameRoom room = getRoomByCode(roomCode);
        Player player = room.getPlayers().stream()
                .filter(p -> p.getPlayerName().equals(playerName))
                .findFirst()
                .orElse(null);

        if (player == null || !player.isAlive() || !player.canAttack()) return;

        final double RANGE = 90.0;
        final double HEIGHT = 25.0;
        final int DAMAGE = 20;

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
                inRange = nx >= px && nx <= px + RANGE &&
                        ny >= py - HEIGHT / 2 && ny <= py + HEIGHT / 2;
            } else {
                inRange = nx <= px && nx >= px - RANGE &&
                        ny >= py - HEIGHT / 2 && ny <= py + HEIGHT / 2;
            }

            if (inRange) {
                boolean killed = npc.receiveDamage(DAMAGE, player.getPlayerName());
                if (killed) {
                    System.out.println(" " + playerName + " mató un NPC!");
                    // Enviar evento de NPC muerto
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
                        System.out.println(" " + player.getPlayerName() + " abrió un cofre!");

                        // Enviar evento de cofre abierto
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
        GameRoom room = getRoomByCode(roomCode);

        boolean allDead = room.getPlayers().stream()
                .allMatch(p -> !p.isAlive());

        if (allDead) {
            onGameLost(roomCode);
        }
    }

    private void onGameLost(String roomCode) {
        GameRoom room = getRoomByCode(roomCode);
        room.setGameStarted(false);

        for (Player p : room.getPlayers()) {
            p.setReady(false);
            p.setHealth(Player.DEFAULT_HEALTH);
            playerRepository.save(p);
        }

        experienceService.resetRoomXp(roomCode);

        // Enviar evento de game over
        Map<String, Object> event = new HashMap<>();
        event.put("type", "GAME_OVER");
        event.put("roomCode", roomCode);
        messagingTemplate.convertAndSend("/topic/game/" + roomCode + "/event", event);

        System.out.println(" GAME OVER en sala " + roomCode);

        gameRoomRepository.save(room);
    }
}