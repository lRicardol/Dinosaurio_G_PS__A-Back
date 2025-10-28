package com.dinosurio_G.Back.service;

import com.dinosurio_G.Back.dto.PlayerHealthDTO;
import com.dinosurio_G.Back.model.*;
import com.dinosurio_G.Back.repository.GameRoomRepository;
import com.dinosurio_G.Back.repository.PlayerRepository;
import com.dinosurio_G.Back.service.impl.ChestService;
import com.dinosurio_G.Back.service.impl.GameMapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class GamePlayServices {
    private static final int goalXp = 1000;
    private final Map<String, Integer> experienceByRoom = new ConcurrentHashMap<>();
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

    private static final double CHEST_INTERACT_RADIUS = 50.0;


    public void updatePlayerInput(String roomCode, String playerName,
                                  boolean arriba, boolean abajo,
                                  boolean izquierda, boolean derecha) {
        GameRoom room = getRoomByCode(roomCode);

        room.getPlayers().stream()
                .filter(p -> p.getPlayerName().equals(playerName))
                .findFirst()
                .ifPresent(player -> {
                    player.setInput(arriba, abajo, izquierda, derecha);
                });
    }

    // Loop global del juego (cada 50 ms)
    @Scheduled(fixedRate = 50)
    public void updateAllRooms() {
        gameRoomRepository.findAll().forEach(room -> {
            if (!room.isGameStarted()) return;

            // 1. Actualizar movimiento
            room.getPlayers().forEach(Player::actualizar);

            // 2. Revisar interacción con cofres
            room.getPlayers().forEach(player -> checkChestInteraction(room.getRoomCode(), player));

            // 3. Ejecutar ataques automáticos
            room.getPlayers().forEach(player -> {
                if (player.isAlive() && player.canAttack()) {
                    playerWhipAttack(room.getRoomCode(), player.getPlayerName());
                }
            });
        });
    }



    //  Obtener la vida actual de los jugadores
    public List<PlayerHealthDTO> getPlayersHealth(String roomCode) {
        GameRoom room = getRoomByCode(roomCode);

        return room.getPlayers().stream()
                .map(p -> new PlayerHealthDTO(p.getPlayerName(), p.getHealth()))
                .collect(Collectors.toList());
    }

    //  Utilidad interna
    private GameRoom getRoomByCode(String roomCode) {
        return gameRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RuntimeException("La sala con código " + roomCode + " no existe"));
    }


    // Sumar XP al room
    public void addExperience(String roomCode, int amount) {
        experienceByRoom.merge(roomCode, amount, Integer::sum);
        int currentXp = experienceByRoom.get(roomCode);

        if (currentXp >= goalXp) {
            experienceByRoom.put(roomCode, goalXp);
            onGameWon(roomCode);
        }
    }
    // Obtener progreso (0.0 a 1.0)
    public double getProgress(String roomCode) {
        return experienceByRoom.getOrDefault(roomCode, 0) / (double) goalXp;
    }

    // Metodo que maneja victoria
    private void onGameWon(String roomCode) {
        // Aquí pones la lógica de final de partida
        System.out.println("¡La partida de la sala " + roomCode + " se ha ganado!");
    }


    public List<Map<String, Object>> spawnPlayers(String roomCode) {
        // Obtener la sala
        GameRoom room = getRoomByCode(roomCode);
        List<Player> players = room.getPlayers();

        double startX = 100;  // posición base
        double startY = 100;
        double offset = 80;   // distancia entre jugadores

        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            player.setX(startX + (i * offset));
            player.setY(startY);
            playerRepository.save(player);
        }
        // Spawnear NPCs
        npcManager.spawnInitialNpcs(roomCode);

        // Retornar posiciones
        return players.stream()
                .map(p -> {
                    Map<String, Object> playerData = new HashMap<>();
                    playerData.put("name", p.getPlayerName());
                    playerData.put("x", p.getX());
                    playerData.put("y", p.getY());
                    return playerData;
                })
                .collect(Collectors.toList());
    }


    // Obtener sala en memoria
    public GameRoom getRoomInMemory(String roomCode) {
        return getRoomByCode(roomCode);
    }

    public synchronized void  playerWhipAttack(String roomCode, String playerName) {
        GameRoom room = getRoomByCode(roomCode);
        Player player = room.getPlayers().stream()
                .filter(p -> p.getPlayerName().equals(playerName))
                .findFirst()
                .orElse(null);

        if (player == null || !player.isAlive() || !player.canAttack()) return;

        // ⚙Parámetros del ataque
        final double RANGE = 90.0;   // Alcance horizontal
        final double HEIGHT = 25.0;  // Grosor vertical
        final int DAMAGE = 20;

        double px = player.getX();
        double py = player.getY();

        // Obtener NPCs
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
                    System.out.println("💀 NPC " + npc.getId() + " muerto por " + player.getPlayerName());
                }
            }
        }
    }

    private void checkChestInteraction(String roomCode, Player player) {
        if (!player.isAlive()) return;

        List<Chest> chests = chestService.findAll();
        for (Chest chest : chests) {
            if (!chest.isActive()) continue;

            double dx = chest.getPosition().getX() - player.getX();
            double dy = chest.getPosition().getY() - player.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance <= CHEST_INTERACT_RADIUS) {
                // 🔒 Sincronizar por ID de cofre
                synchronized (("CHEST_LOCK_" + chest.getId()).intern()) {
                    if (!chest.isActive()) continue;

                    boolean opened = chestService.tryOpenChest(chest.getId());
                    if (opened) {
                        int rewardXp = 150; // o leerlo del chest.getContents()
                        addExperience(roomCode, rewardXp);

                        System.out.println("💰 " + player.getPlayerName() +
                                " abrió cofre " + chest.getId() +
                                " y ganó " + rewardXp + " XP");
                    }
                }
            }
        }
    }


}
